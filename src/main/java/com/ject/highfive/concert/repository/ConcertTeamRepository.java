package com.ject.highfive.concert.repository;

import com.ject.highfive.concert.entity.ConcertTeam;
import com.ject.highfive.concert.entity.ConcertTeamStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConcertTeamRepository extends JpaRepository<ConcertTeam, Long> {

  long countByConcertIdAndStatusAndBandIdNot(Long concertId, ConcertTeamStatus status, Long bandId);
}
