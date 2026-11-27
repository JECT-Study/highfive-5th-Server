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

  // 참여 팀 멤버 확인(JECT-226). 사용자가 속한 밴드 중 하나라도 이 공연의 해당 상태 팀이면 true
  boolean existsByConcertIdAndStatusAndBandIdIn(
      Long concertId, ConcertTeamStatus status, Collection<Long> bandIds);

  // 공고 목록(2.5)의 합류 팀 수. 주최 팀은 빼고 센다(recruitTeamCount와 같은 기준). 팀이 없는 공연은 결과에 없다
  @Query(
      "select t.concert.id as concertId, count(t) as joinedTeamCount from ConcertTeam t"
          + " where t.concert.id in :concertIds and t.status = :status"
          + " and t.bandId <> t.concert.hostBandId"
          + " group by t.concert.id")
  List<JoinedTeamCount> countGuestTeamsByConcertIds(
      @Param("concertIds") Collection<Long> concertIds, @Param("status") ConcertTeamStatus status);

  // 공고 상세(2.7)·라인업(1.4)의 팀 순서. MySQL은 오름차순에서 NULL을 앞에 두므로 nulls last를 쓴다.
  // 메서드 이름 방식(OrderByLineupOrderAsc)은 NULL이 앞에 와서 쓰지 않는다. Hibernate가 MySQL에 맞게 바꿔 보낸다
  @Query(
      "select t from ConcertTeam t where t.concert.id = :concertId and t.status = :status"
          + " order by t.lineupOrder asc nulls last, t.id asc")
  List<ConcertTeam> findLineup(
      @Param("concertId") Long concertId, @Param("status") ConcertTeamStatus status);
}
