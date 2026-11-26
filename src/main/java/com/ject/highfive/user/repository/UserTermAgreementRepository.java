package com.ject.highfive.user.repository;

import com.ject.highfive.user.entity.UserTermAgreement;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserTermAgreementRepository extends JpaRepository<UserTermAgreement, Long> {

  List<UserTermAgreement> findAllByUserIdAndTermIdIn(Long userId, Collection<Long> termIds);

  long countByUserIdAndTermIdInAndAgreedTrue(Long userId, Collection<Long> termIds);
}
