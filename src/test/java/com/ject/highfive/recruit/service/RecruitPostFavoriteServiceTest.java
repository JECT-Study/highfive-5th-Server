package com.ject.highfive.recruit.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.ject.highfive.global.exception.BusinessException;
import com.ject.highfive.recruit.exception.RecruitErrorCode;
import com.ject.highfive.recruit.repository.RecruitPostFavoriteRepository;
import com.ject.highfive.recruit.repository.RecruitPostRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

  @Test
  @DisplayName("공고가 있으면 요청한 사용자·공고로 찜을 넣는다(이미 있으면 DB가 그대로 둔다)")
  void favoriteInsertsIfAbsent() {
    given(recruitPostRepository.existsByIdAndDeletedAtIsNull(POST_ID)).willReturn(true);

    service.favorite(POST_ID, USER_ID);

    verify(favoriteRepository).insertIfAbsent(eq(USER_ID), eq(POST_ID), any(LocalDateTime.class));
  }

  @Test
  @DisplayName("없거나 삭제된 공고를 찜하면 RECRUIT_001이고 찜 테이블은 건드리지 않는다")
  void favoriteMissingPostRejected() {
    given(recruitPostRepository.existsByIdAndDeletedAtIsNull(POST_ID)).willReturn(false);

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
