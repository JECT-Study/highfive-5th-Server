package com.ject.highfive.recruit.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ject.highfive.concert.repository.ConcertRepository;
import com.ject.highfive.recruit.entity.RecruitPost;
import com.ject.highfive.recruit.entity.RecruitPostFavorite;
import com.ject.highfive.support.ConcertFixture;
import com.ject.highfive.support.MySqlRepositoryTest;
import com.ject.highfive.support.RecruitPostFixture;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

@MySqlRepositoryTest
class RecruitPostFavoriteRepositoryTest {

  private static final Long USER_ID = 7L;
  private static final Long OTHER_USER_ID = 8L;
  private static final LocalDateTime FIRST = LocalDateTime.of(2026, 11, 19, 10, 0, 0);
  private static final LocalDateTime LATER = LocalDateTime.of(2026, 11, 19, 11, 0, 0);

  @Autowired RecruitPostFavoriteRepository favoriteRepository;
  @Autowired RecruitPostRepository recruitPostRepository;
  @Autowired ConcertRepository concertRepository;
  @Autowired TestEntityManager em;

  private RecruitPost savedPost() {
    return recruitPostRepository.saveAndFlush(
        RecruitPostFixture.recruitPost(concertRepository.saveAndFlush(ConcertFixture.concert())));
  }

  private RecruitPostFavorite favorite(Long userId, RecruitPost post) {
    return RecruitPostFavorite.builder().userId(userId).recruitPost(post).build();
  }

  @SuppressWarnings("unchecked")
  private List<Object[]> rows(Long postId) {
    em.flush();
    em.clear();
    return em.getEntityManager()
        .createNativeQuery(
            "select user_id, created_at from recruitment_favorites"
                + " where recruitment_post_id = ? order by user_id")
        .setParameter(1, postId)
        .getResultList();
  }

  @Test
  @DisplayName("같은 사용자가 같은 공고를 엔티티로 두 번 저장하면 UNIQUE에 걸린다")
  void duplicateFavoriteViolatesUnique() {
    RecruitPost post = savedPost();
    favoriteRepository.saveAndFlush(favorite(USER_ID, post));

    assertThatThrownBy(() -> favoriteRepository.saveAndFlush(favorite(USER_ID, post)))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  @DisplayName("insertIfAbsent는 찜이 없으면 넘긴 시각으로 행을 넣는다")
  void insertIfAbsentInsertsRow() {
    RecruitPost post = savedPost();

    favoriteRepository.insertIfAbsent(USER_ID, post.getId(), FIRST);

    List<Object[]> rows = rows(post.getId());
    assertThat(rows).hasSize(1);
    assertThat(((Number) rows.get(0)[0]).longValue()).isEqualTo(USER_ID);
    assertThat(rows.get(0)[1]).isEqualTo(FIRST);
  }

  @Test
  @DisplayName("insertIfAbsent를 두 번 불러도 예외 없이 행은 하나이고 처음 찜한 시각이 남는다")
  void insertIfAbsentTwiceKeepsFirstRow() {
    RecruitPost post = savedPost();

    favoriteRepository.insertIfAbsent(USER_ID, post.getId(), FIRST);
    favoriteRepository.insertIfAbsent(USER_ID, post.getId(), LATER);

    List<Object[]> rows = rows(post.getId());
    assertThat(rows).hasSize(1);
    assertThat(rows.get(0)[1]).isEqualTo(FIRST);
  }

  @Test
  @DisplayName("다른 사용자는 같은 공고를 각각 찜할 수 있다")
  void differentUsersCanFavoriteSamePost() {
    RecruitPost post = savedPost();

    favoriteRepository.insertIfAbsent(USER_ID, post.getId(), FIRST);
    favoriteRepository.insertIfAbsent(OTHER_USER_ID, post.getId(), FIRST);

    assertThat(rows(post.getId())).hasSize(2);
  }

  @Test
  @DisplayName("찜 해제는 그 사용자의 행만 지운다")
  void deleteRemovesOnlyThatUsersFavorite() {
    RecruitPost post = savedPost();
    favoriteRepository.insertIfAbsent(USER_ID, post.getId(), FIRST);
    favoriteRepository.insertIfAbsent(OTHER_USER_ID, post.getId(), FIRST);

    favoriteRepository.deleteByUserIdAndRecruitPostId(USER_ID, post.getId());

    List<Object[]> rows = rows(post.getId());
    assertThat(rows).hasSize(1);
    assertThat(((Number) rows.get(0)[0]).longValue()).isEqualTo(OTHER_USER_ID);
  }

  @Test
  @DisplayName("찜하지 않은 공고를 해제해도 예외 없이 아무것도 지우지 않는다")
  void deleteMissingFavoriteDoesNothing() {
    RecruitPost post = savedPost();
    favoriteRepository.insertIfAbsent(OTHER_USER_ID, post.getId(), FIRST);

    favoriteRepository.deleteByUserIdAndRecruitPostId(USER_ID, post.getId());

    assertThat(rows(post.getId())).hasSize(1);
  }

  @Test
  @DisplayName("해제한 뒤 다시 찜하면 새 시각으로 행이 들어간다")
  void favoriteAgainAfterDelete() {
    RecruitPost post = savedPost();
    favoriteRepository.insertIfAbsent(USER_ID, post.getId(), FIRST);
    favoriteRepository.deleteByUserIdAndRecruitPostId(USER_ID, post.getId());

    favoriteRepository.insertIfAbsent(USER_ID, post.getId(), LATER);

    List<Object[]> rows = rows(post.getId());
    assertThat(rows).hasSize(1);
    assertThat(rows.get(0)[1]).isEqualTo(LATER);
  }
}
