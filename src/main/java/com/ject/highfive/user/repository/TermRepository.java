package com.ject.highfive.user.repository;

import com.ject.highfive.user.entity.Term;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TermRepository extends JpaRepository<Term, Long> {

  // 시행일이 지난 모든 버전. 약관 종류·버전이 몇 개 안 돼 code별 최신 버전은 CurrentTerms가 고른다
  List<Term> findAllByEffectiveAtLessThanEqual(LocalDateTime now);
}
