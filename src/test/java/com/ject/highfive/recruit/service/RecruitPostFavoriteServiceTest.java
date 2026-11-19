package com.ject.highfive.recruit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.ject.highfive.global.exception.BusinessException;
import com.ject.highfive.recruit.entity.RecruitPost;
import com.ject.highfive.recruit.entity.RecruitPostFavorite;
import com.ject.highfive.recruit.entity.TeamType;
import com.ject.highfive.recruit.entity.WantedCareerLevel;
import com.ject.highfive.recruit.exception.RecruitErrorCode;
import com.ject.highfive.recruit.repository.RecruitPostFavoriteRepository;
import com.ject.highfive.recruit.repository.RecruitPostRepository;
import com.ject.highfive.support.ConcertFixture;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RecruitPostFavoriteServiceTest {

  private static final Long POST_ID = 5L;
  private static final Long USER_ID = 7L;

  @Mock RecruitPostRepository recruitPostRepository;
  @Mock RecruitPostFavoriteRepository favoriteRepository;
  @InjectMocks RecruitPostFavoriteService service;

  private RecruitPost post() {
    return RecruitPost.builder()
        .concert(ConcertFixture.concert())
        .authorUserId(10L)
        .wantedCareerLevel(WantedCareerLevel.Y1_TO_3)
        .teamTypes(List.of(TeamType.ANYONE))
        .deadline(LocalDate.of(2999, 12, 1))
        .description("소개")
        .build();
  }

  private void givenPost(RecruitPost post) {
    given(recruitPostRepository.findByIdAndDeletedAtIsNull(POST_ID)).willReturn(Optional.of(post));
  }

  @Test
  @DisplayName("찜하지 않은 공고를 찜하면 요청한 사용자와 공고로 저장한다")
  void favoriteSavesNewFavorite() {
    RecruitPost post = post();
    givenPost(post);
    given(favoriteRepository.existsByUserIdAndRecruitPostId(USER_ID, POST_ID)).willReturn(false);

    service.favorite(POST_ID, USER_ID);

    ArgumentCaptor<RecruitPostFavorite> captor = ArgumentCaptor.forClass(RecruitPostFavorite.class);
    verify(favoriteRepository).save(captor.capture());
    assertThat(captor.getValue().getUserId()).isEqualTo(USER_ID);
    assertThat(captor.getValue().getRecruitPost()).isSameAs(post);
  }

  @Test
  @DisplayName("이미 찜한 공고를 다시 찜하면 저장하지 않고 성공한다")
  void favoriteAlreadyFavoritedDoesNotSave() {
    givenPost(post());
    given(favoriteRepository.existsByUserIdAndRecruitPostId(USER_ID, POST_ID)).willReturn(true);

    service.favorite(POST_ID, USER_ID);

    verify(favoriteRepository, never()).save(any());
  }

  @Test
  @DisplayName("마감된 공고도 찜할 수 있다")
  void favoriteClosedPostAllowed() {
    RecruitPost post = post();
    post.close();
    givenPost(post);
    given(favoriteRepository.existsByUserIdAndRecruitPostId(USER_ID, POST_ID)).willReturn(false);

    service.favorite(POST_ID, USER_ID);

    verify(favoriteRepository).save(any(RecruitPostFavorite.class));
  }

  @Test
  @DisplayName("없거나 삭제된 공고를 찜하면 RECRUIT_001이고 찜 테이블은 보지 않는다")
  void favoriteMissingPostRejected() {
    given(recruitPostRepository.findByIdAndDeletedAtIsNull(POST_ID)).willReturn(Optional.empty());

    assertThatThrownBy(() -> service.favorite(POST_ID, USER_ID))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(RecruitErrorCode.RECRUIT_POST_NOT_FOUND);
    verifyNoInteractions(favoriteRepository);
  }

  @Test
  @DisplayName("찜 해제는 공고를 조회하지 않고 사용자·공고의 찜 행만 지운다")
  void unfavoriteDoesNotLookUpPost() {
    service.unfavorite(POST_ID, USER_ID);

    verify(favoriteRepository).deleteByUserIdAndRecruitPostId(USER_ID, POST_ID);
    verifyNoInteractions(recruitPostRepository);
  }
}
