package com.ject.highfive.recruit.repository;

import com.ject.highfive.recruit.entity.RecruitPost;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface RecruitPostRepository extends JpaRepository<RecruitPost, Long> {

  // 삭제한 공고도 포함한다 (공연당 공고 1개, 스펙 4.5)
  boolean existsByConcertId(Long concertId);

  // 수정·마감·삭제가 동시에 오면 UPDATE가 옛 status·deleted_at을 다시 써서 서로를 덮어쓴다. 행을 잠가 차례로 처리한다
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<RecruitPost> findByIdAndDeletedAtIsNull(Long id);

  Optional<RecruitPost> findByConcertIdAndDeletedAtIsNull(Long concertId);

  // 찜은 공고가 있는지만 본다. 잠그면 같은 공고의 모든 찜과 수정·마감·삭제가 줄을 선다 (222 스펙 4.4)
  boolean existsByIdAndDeletedAtIsNull(Long id);
}
