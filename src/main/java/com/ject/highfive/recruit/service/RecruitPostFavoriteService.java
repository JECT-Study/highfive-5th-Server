package com.ject.highfive.recruit.service;

import com.ject.highfive.global.exception.BusinessException;
import com.ject.highfive.recruit.exception.RecruitErrorCode;
import com.ject.highfive.recruit.repository.RecruitPostFavoriteRepository;
import com.ject.highfive.recruit.repository.RecruitPostRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 찜·해제는 SQL 한 줄로 끝내고 공고 행을 잠그지 않는다. 겹치는 요청은 찜 행의 UNIQUE가 정리한다 (스펙 4.4)
@Service
@Transactional
@RequiredArgsConstructor
public class RecruitPostFavoriteService {

  private final RecruitPostRepository recruitPostRepository;
  private final RecruitPostFavoriteRepository favoriteRepository;

  public void favorite(Long recruitPostId, Long userId) {
    if (!recruitPostRepository.existsByIdAndDeletedAtIsNull(recruitPostId)) {
      throw new BusinessException(RecruitErrorCode.RECRUIT_POST_NOT_FOUND);
    }
    favoriteRepository.insertIfAbsent(userId, recruitPostId, LocalDateTime.now());
  }

  // 공고를 보지 않는다. 찜하지 않았어도 성공하고, 삭제된 공고에 남은 찜도 지울 수 있다
  public void unfavorite(Long recruitPostId, Long userId) {
    favoriteRepository.deleteByUserIdAndRecruitPostId(userId, recruitPostId);
  }
}
