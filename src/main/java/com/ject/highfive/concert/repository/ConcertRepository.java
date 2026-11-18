package com.ject.highfive.concert.repository;

import com.ject.highfive.concert.entity.Concert;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConcertRepository extends JpaRepository<Concert, Long> {

  Optional<Concert> findByIdAndDeletedAtIsNull(Long id);
}
