package com.ject.highfive.concert.repository;

import com.ject.highfive.concert.entity.ConcertTeam;
import com.ject.highfive.concert.entity.ConcertTeamStatus;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConcertTeamRepository extends JpaRepository<ConcertTeam, Long> {

  long countByConcertIdAndStatusAndBandIdNot(Long concertId, ConcertTeamStatus status, Long bandId);

  // 공고 목록(2.5)의 합류 팀 수. 주최 팀은 빼고 센다(recruitTeamCount와 같은 기준). 팀이 없는 공연은 결과에 없다
  @Query(
      "select t.concert.id as concertId, count(t) as joinedTeamCount from ConcertTeam t"
          + " where t.concert.id in :concertIds and t.status = :status"
          + " and t.bandId <> t.concert.hostBandId"
          + " group by t.concert.id")
  List<JoinedTeamCount> countGuestTeamsByConcertIds(
      @Param("concertIds") Collection<Long> concertIds, @Param("status") ConcertTeamStatus status);
}
