package com.ject.highfive.concert.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ject.highfive.concert.dto.ConcertUpdateRequest;
import com.ject.highfive.concert.entity.Concert;
import com.ject.highfive.concert.entity.ConcertTeam;
import com.ject.highfive.concert.entity.Genre;
import com.ject.highfive.concert.exception.ConcertErrorCode;
import com.ject.highfive.concert.repository.ConcertRepository;
import com.ject.highfive.concert.repository.ConcertTeamRepository;
import com.ject.highfive.global.exception.BusinessException;
import com.ject.highfive.support.ConcertFixture;
import com.ject.highfive.support.MySqlContainerConfig;
import java.time.LocalDate;
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

// 실제 MySQL에서 두 트랜잭션을 겹쳐 비관적 락과 버전 비교가 함께 동작하는지 본다 (의사결정 D12)
@SpringBootTest
@Import(MySqlContainerConfig.class)
class ConcertServiceConcurrencyTest {

  private static final LocalDate PERFORM_DATE = LocalDate.of(2999, 12, 20);

  @Autowired ConcertService concertService;
  @Autowired ConcertRepository concertRepository;
  @Autowired ConcertTeamRepository concertTeamRepository;
  @Autowired TransactionTemplate transactionTemplate;

  @AfterEach
  void cleanUp() {
    concertTeamRepository.deleteAll();
    concertRepository.deleteAll();
  }

  private ConcertUpdateRequest request(String title, Long version) {
    return new ConcertUpdateRequest(
        title,
        List.of(Genre.ROCK),
        3,
        PERFORM_DATE,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        version);
  }

  @Test
  @DisplayName("같은 버전으로 동시에 수정하면 뒤 요청은 앞 요청이 끝날 때까지 기다렸다가 CONCERT_012를 받는다")
  void concurrentSameVersionUpdateWaitsThenRejected() throws Exception {
    Concert saved = concertRepository.saveAndFlush(ConcertFixture.concert());
    // 요청자(사용자 3)가 호스트 밴드(3)의 멤버라 참여 팀 멤버 확인을 통과한다
    concertTeamRepository.saveAndFlush(
        ConcertTeam.builder().concert(saved).bandId(ConcertFixture.HOST_BAND_ID).build());
    Long concertId = saved.getId();
    CountDownLatch firstLocked = new CountDownLatch(1);
    CountDownLatch releaseFirst = new CountDownLatch(1);
    ExecutorService executor = Executors.newFixedThreadPool(2);
    try {
      // A: 공연 행을 잠그고 고친 뒤, 커밋하지 않고 멈춰 있는다
      final Future<?> first =
          executor.submit(
              () ->
                  transactionTemplate.executeWithoutResult(
                      status -> {
                        Concert concert =
                            concertRepository.findByIdAndDeletedAtIsNull(concertId).orElseThrow();
                        concert.update(
                            "먼저 고친 제목",
                            List.of(Genre.ROCK),
                            3,
                            PERFORM_DATE,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            1L);
                        firstLocked.countDown();
                        awaitQuietly(releaseFirst);
                      }));
      assertThat(firstLocked.await(10, TimeUnit.SECONDS)).isTrue();

      // B: A와 같은 버전(0)을 보고 고친 요청
      Future<?> second =
          executor.submit(() -> concertService.update(concertId, 3L, request("나중에 고친 제목", 0L)));

      // 락이 없으면 B는 A를 기다리지 않고 바로 끝난다
      Thread.sleep(500);
      assertThat(second.isDone()).as("B는 A의 행 잠금을 기다려야 한다").isFalse();

      releaseFirst.countDown();
      first.get(10, TimeUnit.SECONDS);
      assertThatThrownBy(() -> second.get(10, TimeUnit.SECONDS))
          .isInstanceOf(ExecutionException.class)
          .cause()
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ConcertErrorCode.CONCERT_MODIFIED_BY_OTHER);
    } finally {
      releaseFirst.countDown();
      executor.shutdownNow();
    }

    Concert result = concertRepository.findById(concertId).orElseThrow();
    assertThat(result.getTitle()).isEqualTo("먼저 고친 제목");
    assertThat(result.getVersion()).isEqualTo(1L);
  }

  private static void awaitQuietly(CountDownLatch latch) {
    try {
      latch.await(10, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
