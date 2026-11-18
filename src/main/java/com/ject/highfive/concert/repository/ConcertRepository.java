package com.ject.highfive.concert.repository;

import com.ject.highfive.concert.entity.Concert;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface ConcertRepository extends JpaRepository<Concert, Long> {

  // 수정·공고 작성이 같은 공연을 동시에 다루면 차례로 처리한다 (의사결정 D12).
  // 같은 version으로 거의 동시에 온 두 수정 중 뒤 요청은 앞 요청이 커밋한 뒤 버전 비교에서 409가 된다
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<Concert> findByIdAndDeletedAtIsNull(Long id);
}
