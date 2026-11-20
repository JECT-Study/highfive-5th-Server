package com.ject.highfive.concert.repository;

// 공연별 합류 팀 수 조회 결과 (공고 목록 2.5)
public interface JoinedTeamCount {

  Long getConcertId();

  Long getJoinedTeamCount();
}
