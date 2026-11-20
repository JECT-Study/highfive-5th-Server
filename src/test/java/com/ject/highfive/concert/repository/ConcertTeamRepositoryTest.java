package com.ject.highfive.concert.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import com.ject.highfive.concert.entity.Concert;
import com.ject.highfive.concert.entity.ConcertTeam;
import com.ject.highfive.concert.entity.ConcertTeamStatus;
import com.ject.highfive.support.ConcertFixture;
import com.ject.highfive.support.MySqlRepositoryTest;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@MySqlRepositoryTest
class ConcertTeamRepositoryTest {

  @Autowired ConcertRepository concertRepository;
  @Autowired ConcertTeamRepository concertTeamRepository;

  private Concert savedConcert() {
    return concertRepository.saveAndFlush(ConcertFixture.concert());
  }

  private ConcertTeam savedTeam(Concert concert, Long bandId) {
    return concertTeamRepository.saveAndFlush(
        ConcertTeam.builder().concert(concert).bandId(bandId).build());
  }

  @Test
  @DisplayName("합류 수에서 주최 팀, 나간 팀, 강퇴된 팀, 다른 공연의 팀은 빠진다")
  void countJoinedGuestTeams() {
    Concert concert = savedConcert();
    savedTeam(concert, ConcertFixture.HOST_BAND_ID);
    savedTeam(concert, 11L);
    savedTeam(concert, 12L);
    savedTeam(concert, 13L).leave();
    savedTeam(concert, 14L).kick();
    savedTeam(savedConcert(), 11L);
    concertTeamRepository.flush();

    assertThat(
            concertTeamRepository.countByConcertIdAndStatusAndBandIdNot(
                concert.getId(), ConcertTeamStatus.JOINED, ConcertFixture.HOST_BAND_ID))
        .isEqualTo(2);
  }

  @Test
  @DisplayName("공연별 합류 팀 수는 주최 팀·나간 팀·강퇴된 팀을 빼고 세며, 팀이 없는 공연은 결과에 없다")
  void countGuestTeamsByConcertIds() {
    Concert first = savedConcert();
    savedTeam(first, ConcertFixture.HOST_BAND_ID);
    savedTeam(first, 11L);
    savedTeam(first, 12L);
    savedTeam(first, 13L).leave();
    savedTeam(first, 14L).kick();
    Concert second = savedConcert();
    savedTeam(second, 11L);
    Concert empty = savedConcert();
    savedTeam(savedConcert(), 15L); // 목록에 없는 공연
    concertTeamRepository.flush();

    Map<Long, Long> counts =
        concertTeamRepository
            .countGuestTeamsByConcertIds(
                List.of(first.getId(), second.getId(), empty.getId()), ConcertTeamStatus.JOINED)
            .stream()
            .collect(
                Collectors.toMap(
                    JoinedTeamCount::getConcertId, JoinedTeamCount::getJoinedTeamCount));

    assertThat(counts).containsOnly(entry(first.getId(), 2L), entry(second.getId(), 1L));
  }
}
