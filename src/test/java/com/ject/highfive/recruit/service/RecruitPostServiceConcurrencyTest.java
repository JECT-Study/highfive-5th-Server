package com.ject.highfive.recruit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ject.highfive.concert.entity.Concert;
import com.ject.highfive.concert.repository.ConcertRepository;
import com.ject.highfive.global.exception.BusinessException;
import com.ject.highfive.recruit.dto.RecruitPostRequest;
import com.ject.highfive.recruit.entity.RecruitPost;
import com.ject.highfive.recruit.entity.RecruitPostStatus;
import com.ject.highfive.recruit.entity.TeamType;
import com.ject.highfive.recruit.entity.WantedCareerLevel;
import com.ject.highfive.recruit.exception.RecruitErrorCode;
import com.ject.highfive.recruit.repository.RecruitPostRepository;
import com.ject.highfive.support.ConcertFixture;
import com.ject.highfive.support.MySqlContainerConfig;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.support.TransactionTemplate;

// 실제 MySQL에서 두 트랜잭션을 겹쳐 공고·공연 조회의 비관적 락이 동작하는지 본다
@SpringBootTest
@Import(MySqlContainerConfig.class)
class RecruitPostServiceConcurrencyTest {

  @Autowired RecruitPostService recruitPostService;
  @Autowired RecruitPostRepository recruitPostRepository;
  @Autowired ConcertRepository concertRepository;
  @Autowired TransactionTemplate transactionTemplate;

  @AfterEach
  void cleanUp() {
    recruitPostRepository.deleteAll();
    concertRepository.deleteAll();
  }

  private Long savedPostId() {
    Concert concert = concertRepository.saveAndFlush(ConcertFixture.concert());
    return recruitPostRepository
        .saveAndFlush(
            RecruitPost.builder()
                .concert(concert)
                .authorUserId(10L)
                .wantedCareerLevel(WantedCareerLevel.Y1_TO_3)
                .teamTypes(List.of(TeamType.ANYONE))
                .deadline(LocalDate.of(2999, 12, 1))
                .description("기존 소개")
                .build())
        .getId();
  }

  @Test
  @DisplayName("마감 중인 공고를 동시에 수정하면 수정은 마감이 끝날 때까지 기다렸다가 RECRUIT_004를 받고 마감이 풀리지 않는다")
  void updateDuringCloseWaitsThenRejected() throws Exception {
    Long postId = savedPostId();
    CountDownLatch closeLocked = new CountDownLatch(1);
    CountDownLatch releaseClose = new CountDownLatch(1);
    ExecutorService executor = Executors.newFixedThreadPool(2);
    try {
      // A: 공고 행을 잠그고 마감한 뒤, 커밋하지 않고 멈춰 있는다
      final Future<?> close =
          executor.submit(
              () ->
                  transactionTemplate.executeWithoutResult(
                      status -> {
                        recruitPostRepository
                            .findByIdAndDeletedAtIsNull(postId)
                            .orElseThrow()
                            .close();
                        closeLocked.countDown();
                        awaitQuietly(releaseClose);
                      }));
      assertThat(closeLocked.await(10, TimeUnit.SECONDS)).isTrue();

      // B: 같은 공고를 수정
      RecruitPostRequest request =
          new RecruitPostRequest(
              WantedCareerLevel.OVER_3Y,
              List.of(TeamType.HOBBY),
              LocalDate.of(2999, 12, 5),
              "고친 소개");
      Future<?> update = executor.submit(() -> recruitPostService.update(postId, 10L, request));

      // 락이 없으면 B는 A를 기다리지 않고 바로 끝난다
      Thread.sleep(500);
      assertThat(update.isDone()).as("수정은 마감의 행 잠금을 기다려야 한다").isFalse();

      releaseClose.countDown();
      close.get(10, TimeUnit.SECONDS);
      assertThatThrownBy(() -> update.get(10, TimeUnit.SECONDS))
          .isInstanceOf(ExecutionException.class)
          .cause()
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(RecruitErrorCode.RECRUIT_POST_ALREADY_CLOSED);
    } finally {
      releaseClose.countDown();
      executor.shutdownNow();
    }

    RecruitPost result = recruitPostRepository.findById(postId).orElseThrow();
    assertThat(result.getStatus()).isEqualTo(RecruitPostStatus.CLOSED);
    assertThat(result.getDescription()).isEqualTo("기존 소개");
  }

  @Test
  @DisplayName("같은 공연에 공고를 동시에 작성하면 하나만 만들어지고 나머지는 RECRUIT_002를 받는다")
  void concurrentCreateOnlyOneSucceeds() throws Exception {
    Long concertId = concertRepository.saveAndFlush(ConcertFixture.concert()).getId();
    RecruitPostRequest request =
        new RecruitPostRequest(
            WantedCareerLevel.Y1_TO_3, List.of(TeamType.ANYONE), LocalDate.of(2999, 12, 1), "소개");
    int threads = 2;
    CountDownLatch start = new CountDownLatch(1);
    ExecutorService executor = Executors.newFixedThreadPool(threads);
    List<Future<?>> creates = new ArrayList<>();
    try {
      for (int i = 0; i < threads; i++) {
        creates.add(
            executor.submit(
                () -> {
                  awaitQuietly(start);
                  return recruitPostService.create(concertId, 10L, request);
                }));
      }
      start.countDown();

      // 락이 없으면 둘 다 중복 확인을 통과하고, 진 쪽은 RECRUIT_002가 아니라 UNIQUE 위반으로 실패한다
      List<Object> failures = new ArrayList<>();
      for (Future<?> create : creates) {
        try {
          create.get(10, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
          failures.add(e.getCause());
        }
      }
      assertThat(failures)
          .singleElement()
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(RecruitErrorCode.RECRUIT_POST_ALREADY_EXISTS);
    } finally {
      start.countDown();
      executor.shutdownNow();
    }

    assertThat(recruitPostRepository.findAll()).hasSize(1);
  }

  private static void awaitQuietly(CountDownLatch latch) {
    try {
      latch.await(10, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
