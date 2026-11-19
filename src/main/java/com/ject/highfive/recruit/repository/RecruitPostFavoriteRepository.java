package com.ject.highfive.recruit.repository;

import com.ject.highfive.recruit.entity.RecruitPostFavorite;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecruitPostFavoriteRepository extends JpaRepository<RecruitPostFavorite, Long> {

  boolean existsByUserIdAndRecruitPostId(Long userId, Long recruitPostId);

  // 행이 없으면 아무것도 하지 않는다 (찜 해제는 멱등, 스펙 4.4)
  void deleteByUserIdAndRecruitPostId(Long userId, Long recruitPostId);
}
