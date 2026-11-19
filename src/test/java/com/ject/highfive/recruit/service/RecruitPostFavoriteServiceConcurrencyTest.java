package com.ject.highfive.recruit.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ject.highfive.concert.repository.ConcertRepository;
import com.ject.highfive.recruit.entity.RecruitPost;
import com.ject.highfive.recruit.entity.RecruitPostFavorite;
import com.ject.highfive.recruit.repository.RecruitPostFavoriteRepository;
import com.ject.highfive.recruit.repository.RecruitPostRepository;
import com.ject.highfive.support.ConcertFixture;
import com.ject.highfive.support.MySqlContainerConfig;
import com.ject.highfive.support.RecruitPostFixture;
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

// 실제 MySQL에서 두 트랜잭션을 겹쳐 본다. A는 작업 후 커밋하지 않고 붙잡혀 있고, 그동안 B가 들어온다
@SpringBootTest
@Import(MySqlContainerConfig.class)
class RecruitPostFavoriteServiceConcurrencyTest {

  private static final Long USER_ID = 7L;
  private static final Long OTHER_USER_ID = 8L;

  @Autowired RecruitPostFavoriteService favoriteService;
  @Autowired RecruitPostFavoriteRepository favoriteRepository;
  @Autowired RecruitPostRepository recruitPostRepository;
  @Autowired ConcertRepository concertRepository;
  @Autowired TransactionTemplate transactionTemplate;

  private final ExecutorService executor = Executors.newFixedThreadPool(2);
  private final CountDownLatch releaseA = new CountDownLatch(1);

  // A가 커밋을 마친 뒤에 지워야 다음 테스트로 행이 넘어가지 않는다
  @AfterEach
  void cleanUp() throws InterruptedException {
    releaseA.countDown();
    executor.shutdown();
    assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
    favoriteRepository.deleteAll();
    recruitPostRepository.deleteAll();
    concertRepository.deleteAll();
  }

  private RecruitPost savedPost() {
    return recruitPostRepository.saveAndFlush(
        RecruitPostFixture.recruitPost(concertRepository.saveAndFlush(ConcertFixture.concert())));
  }

  // A: work를 실행한 트랜잭션을 커밋하지 않고 releaseA까지 붙잡아 둔다
  private Future<?> holdOpenA(Runnable work) throws InterruptedException {
    CountDownLatch workDone = new CountDownLatch(1);
    Future<?> a =
        executor.submit(
            () ->
                transactionTemplate.executeWithoutResult(
                    status -> {
                      work.run();
                      workDone.countDown();
                      awaitQuietly(releaseA);
                    }));
    assertThat(workDone.await(10, TimeUnit.SECONDS)).isTrue();
    return a;
  }

  @Test
  @DisplayName("같은 사용자의 찜이 겹쳐도 둘 다 성공하고 찜 행은 하나만 남는다")
  void sameUserFavoriteOverlapBothSucceed() throws Exception {
    Long postId = savedPost().getId();
    Future<?> a = holdOpenA(() -> favoriteService.favorite(postId, USER_ID));

    final Future<?> b = executor.submit(() -> favoriteService.favorite(postId, USER_ID));
    Thread.sleep(500);
    releaseA.countDown();

    a.get(10, TimeUnit.SECONDS);
    b.get(10, TimeUnit.SECONDS); // 예외 없이 끝나야 한다
    assertThat(favoriteRepository.count()).isEqualTo(1);
  }

  @Test
  @DisplayName("같은 사용자의 찜 해제가 겹쳐도 둘 다 성공한다")
  void sameUserUnfavoriteOverlapBothSucceed() throws Exception {
    RecruitPost post = savedPost();
    favoriteRepository.saveAndFlush(
        RecruitPostFavorite.builder().userId(USER_ID).recruitPost(post).build());
    Future<?> a = holdOpenA(() -> favoriteService.unfavorite(post.getId(), USER_ID));

    // B가 행을 읽은 뒤 A의 삭제를 기다리도록 시간을 준다
    final Future<?> b = executor.submit(() -> favoriteService.unfavorite(post.getId(), USER_ID));
    Thread.sleep(500);
    releaseA.countDown();

    a.get(10, TimeUnit.SECONDS);
    b.get(10, TimeUnit.SECONDS); // 예외 없이 끝나야 한다
    assertThat(favoriteRepository.count()).isZero();
  }

  @Test
  @DisplayName("한 사용자의 찜이 커밋 전이어도 다른 사용자의 같은 공고 찜은 기다리지 않는다")
  void otherUserFavoriteDoesNotWait() throws Exception {
    Long postId = savedPost().getId();
    holdOpenA(() -> favoriteService.favorite(postId, USER_ID));

    Future<?> b = executor.submit(() -> favoriteService.favorite(postId, OTHER_USER_ID));

    b.get(5, TimeUnit.SECONDS); // A가 붙잡혀 있는 동안 끝나야 한다
  }

  private static void awaitQuietly(CountDownLatch latch) {
    try {
      latch.await(10, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
