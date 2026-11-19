package com.ject.highfive.recruit.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ject.highfive.concert.repository.ConcertRepository;
import com.ject.highfive.recruit.entity.RecruitPost;
import com.ject.highfive.recruit.entity.RecruitPostFavorite;
import com.ject.highfive.recruit.entity.TeamType;
import com.ject.highfive.recruit.entity.WantedCareerLevel;
import com.ject.highfive.support.ConcertFixture;
import com.ject.highfive.support.MySqlRepositoryTest;
import java.time.LocalDate;
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

  @Autowired RecruitPostFavoriteRepository favoriteRepository;
  @Autowired RecruitPostRepository recruitPostRepository;
  @Autowired ConcertRepository concertRepository;
  @Autowired TestEntityManager em;

  private RecruitPost savedPost() {
    return recruitPostRepository.saveAndFlush(
        RecruitPost.builder()
            .concert(concertRepository.saveAndFlush(ConcertFixture.concert()))
            .authorUserId(10L)
            .wantedCareerLevel(WantedCareerLevel.Y1_TO_3)
            .teamTypes(List.of(TeamType.ANYONE))
            .deadline(LocalDate.of(2999, 12, 1))
            .description("12월 홍대에서 함께할 팀을 찾습니다.")
            .build());
  }

  private RecruitPostFavorite favorite(Long userId, RecruitPost post) {
    return RecruitPostFavorite.builder().userId(userId).recruitPost(post).build();
  }

  @Test
  @DisplayName("찜을 저장하면 생성 시각이 생기고 사용자·공고로 찾을 수 있다")
  void savedFavoriteIsFound() {
    RecruitPost post = savedPost();
    RecruitPostFavorite saved = favoriteRepository.saveAndFlush(favorite(USER_ID, post));
    em.clear();

    assertThat(saved.getCreatedAt()).isNotNull();
    assertThat(favoriteRepository.existsByUserIdAndRecruitPostId(USER_ID, post.getId())).isTrue();
    assertThat(favoriteRepository.existsByUserIdAndRecruitPostId(OTHER_USER_ID, post.getId()))
        .isFalse();
  }

  @Test
  @DisplayName("같은 사용자가 같은 공고를 두 번 저장하면 UNIQUE에 걸린다")
  void duplicateFavoriteViolatesUnique() {
    RecruitPost post = savedPost();
    favoriteRepository.saveAndFlush(favorite(USER_ID, post));

    assertThatThrownBy(() -> favoriteRepository.saveAndFlush(favorite(USER_ID, post)))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  @DisplayName("다른 사용자는 같은 공고를 각각 찜할 수 있다")
  void differentUsersCanFavoriteSamePost() {
    RecruitPost post = savedPost();
    favoriteRepository.saveAndFlush(favorite(USER_ID, post));
    favoriteRepository.saveAndFlush(favorite(OTHER_USER_ID, post));

    assertThat(favoriteRepository.count()).isEqualTo(2);
  }

  @Test
  @DisplayName("찜 해제는 그 사용자의 행만 지운다")
  void deleteRemovesOnlyThatUsersFavorite() {
    RecruitPost post = savedPost();
    favoriteRepository.saveAndFlush(favorite(USER_ID, post));
    favoriteRepository.saveAndFlush(favorite(OTHER_USER_ID, post));

    favoriteRepository.deleteByUserIdAndRecruitPostId(USER_ID, post.getId());
    em.flush();
    em.clear();

    assertThat(favoriteRepository.existsByUserIdAndRecruitPostId(USER_ID, post.getId())).isFalse();
    assertThat(favoriteRepository.existsByUserIdAndRecruitPostId(OTHER_USER_ID, post.getId()))
        .isTrue();
  }

  @Test
  @DisplayName("찜하지 않은 공고를 해제해도 예외 없이 아무것도 지우지 않는다")
  void deleteMissingFavoriteDoesNothing() {
    RecruitPost post = savedPost();
    favoriteRepository.saveAndFlush(favorite(OTHER_USER_ID, post));

    favoriteRepository.deleteByUserIdAndRecruitPostId(USER_ID, post.getId());
    em.flush();

    assertThat(favoriteRepository.count()).isEqualTo(1);
  }

  @Test
  @DisplayName("해제한 뒤 다시 찜하면 새 행이 들어간다")
  void favoriteAgainAfterDelete() {
    RecruitPost post = savedPost();
    favoriteRepository.saveAndFlush(favorite(USER_ID, post));
    favoriteRepository.deleteByUserIdAndRecruitPostId(USER_ID, post.getId());
    em.flush();

    favoriteRepository.saveAndFlush(favorite(USER_ID, post));

    assertThat(favoriteRepository.existsByUserIdAndRecruitPostId(USER_ID, post.getId())).isTrue();
  }
}
