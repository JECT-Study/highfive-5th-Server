package com.ject.highfive.recruit.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ject.highfive.concert.entity.Concert;
import com.ject.highfive.concert.repository.ConcertRepository;
import com.ject.highfive.recruit.entity.RecruitPost;
import com.ject.highfive.recruit.entity.TeamType;
import com.ject.highfive.recruit.entity.WantedCareerLevel;
import com.ject.highfive.recruit.repository.RecruitPostFavoriteRepository;
import com.ject.highfive.recruit.repository.RecruitPostRepository;
import com.ject.highfive.support.ConcertFixture;
import com.ject.highfive.support.MySqlContainerConfig;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CountDownLatch;
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

// 실제 MySQL에서 두 트랜잭션을 겹쳐 찜의 공고 행 잠금이 동작하는지 본다
@SpringBootTest
@Import(MySqlContainerConfig.class)
class RecruitPostFavoriteServiceConcurrencyTest {

  private static final Long USER_ID = 7L;

  @Autowired RecruitPostFavoriteService favoriteService;
  @Autowired RecruitPostFavoriteRepository favoriteRepository;
  @Autowired RecruitPostRepository recruitPostRepository;
  @Autowired ConcertRepository concertRepository;
  @Autowired TransactionTemplate transactionTemplate;

  @AfterEach
  void cleanUp() {
    favoriteRepository.deleteAll();
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
                .description("소개")
                .build())
        .getId();
  }

  @Test
  @DisplayName("같은 사용자의 찜이 겹치면 뒤 요청은 앞 요청을 기다렸다가 성공하고 찜 행은 하나만 남는다")
  void concurrentFavoriteWaitsAndKeepsOneRow() throws Exception {
    Long postId = savedPostId();
    CountDownLatch firstSaved = new CountDownLatch(1);
    CountDownLatch releaseFirst = new CountDownLatch(1);
    ExecutorService executor = Executors.newFixedThreadPool(2);
    try {
      // A: 찜을 저장하고(공고 행 잠금), 커밋하지 않고 멈춰 있는다
      final Future<?> first =
          executor.submit(
              () ->
                  transactionTemplate.executeWithoutResult(
                      status -> {
                        favoriteService.favorite(postId, USER_ID);
                        firstSaved.countDown();
                        awaitQuietly(releaseFirst);
                      }));
      assertThat(firstSaved.await(10, TimeUnit.SECONDS)).isTrue();

      // B: 같은 사용자가 같은 공고를 찜
      Future<?> second = executor.submit(() -> favoriteService.favorite(postId, USER_ID));

      // 락이 없으면 B는 A를 기다리지 않고 바로 "없음"을 보고 저장하려 한다
      Thread.sleep(500);
      assertThat(second.isDone()).as("뒤 찜은 앞 찜의 공고 행 잠금을 기다려야 한다").isFalse();

      releaseFirst.countDown();
      first.get(10, TimeUnit.SECONDS);
      second.get(10, TimeUnit.SECONDS); // 예외 없이 끝나야 한다
    } finally {
      releaseFirst.countDown();
      executor.shutdownNow();
    }

    assertThat(favoriteRepository.count()).isEqualTo(1);
  }

  private static void awaitQuietly(CountDownLatch latch) {
    try {
      latch.await(10, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
