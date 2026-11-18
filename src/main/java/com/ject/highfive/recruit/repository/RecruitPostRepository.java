package com.ject.highfive.recruit.repository;

import com.ject.highfive.recruit.entity.RecruitPost;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecruitPostRepository extends JpaRepository<RecruitPost, Long> {

  // 삭제한 공고도 포함한다 (공연당 공고 1개, 스펙 4.5)
  boolean existsByConcertId(Long concertId);
}
