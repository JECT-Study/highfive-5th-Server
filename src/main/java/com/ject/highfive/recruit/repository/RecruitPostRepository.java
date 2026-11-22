package com.ject.highfive.recruit.repository;

import com.ject.highfive.recruit.entity.RecruitPost;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;

public interface RecruitPostRepository
    extends JpaRepository<RecruitPost, Long>, JpaSpecificationExecutor<RecruitPost> {

  // 삭제한 공고는 세지 않는다. 지운 뒤에는 같은 공연에 다시 쓸 수 있다 (의사결정 D13)
  boolean existsByConcertIdAndDeletedAtIsNull(Long concertId);

  // 수정·마감·삭제가 동시에 오면 UPDATE가 옛 status·deleted_at을 다시 써서 서로를 덮어쓴다. 행을 잠가 차례로 처리한다
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<RecruitPost> findByIdAndDeletedAtIsNull(Long id);

  // 상세(2.7)용이라 락을 걸지 않는다. 공연도 응답에 쓰므로 함께 읽고, 공연이 삭제됐으면 찾지 않는다.
  // findWith…의 With…는 Spring Data가 무시하는 부분으로, 락 걸린 findByIdAndDeletedAtIsNull과 이름으로 구분한다
  @EntityGraph(attributePaths = "concert")
  Optional<RecruitPost> findWithConcertByIdAndDeletedAtIsNullAndConcertDeletedAtIsNull(Long id);

  Optional<RecruitPost> findByConcertIdAndDeletedAtIsNull(Long concertId);

  // 찜은 공고가 있는지만 잠그지 않고 본다. 배타 잠금으로 읽으면 같은 공고의 찜끼리 줄을 선다 (222 스펙 4.4)
  boolean existsByIdAndDeletedAtIsNull(Long id);

  // 목록(2.5)은 공연을 함께 읽는다. 공고당 공연 1개라 행이 늘지 않고, 개수 쿼리에는 걸리지 않는다
  @Override
  @EntityGraph(attributePaths = "concert")
  Page<RecruitPost> findAll(Specification<RecruitPost> spec, Pageable pageable);
}
