package com.ject.highfive.recruit.service;

import com.ject.highfive.global.exception.BusinessException;
import com.ject.highfive.recruit.entity.RecruitPost;
import com.ject.highfive.recruit.entity.RecruitPostFavorite;
import com.ject.highfive.recruit.exception.RecruitErrorCode;
import com.ject.highfive.recruit.repository.RecruitPostFavoriteRepository;
import com.ject.highfive.recruit.repository.RecruitPostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class RecruitPostFavoriteService {

  private final RecruitPostRepository recruitPostRepository;
  private final RecruitPostFavoriteRepository favoriteRepository;

  // 공고 행을 잠가 읽는다. 같은 사용자의 찜이 동시에 와도 차례로 처리되어 두 번째는 "이미 찜함"을 본다.
  // 락이 없으면 둘 다 저장하려다 UNIQUE 위반(500)이 난다 (스펙 4.4)
  public void favorite(Long recruitPostId, Long userId) {
    RecruitPost post =
        recruitPostRepository
            .findByIdAndDeletedAtIsNull(recruitPostId)
            .orElseThrow(() -> new BusinessException(RecruitErrorCode.RECRUIT_POST_NOT_FOUND));
    if (favoriteRepository.existsByUserIdAndRecruitPostId(userId, recruitPostId)) {
      return;
    }
    favoriteRepository.save(RecruitPostFavorite.builder().userId(userId).recruitPost(post).build());
  }

  // 공고를 보지 않는다. 찜하지 않았어도 성공하고, 삭제된 공고에 남은 찜도 지울 수 있다 (스펙 4.4)
  public void unfavorite(Long recruitPostId, Long userId) {
    favoriteRepository.deleteByUserIdAndRecruitPostId(userId, recruitPostId);
  }
}
