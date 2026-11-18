package com.ject.highfive.concert.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.ject.highfive.concert.entity.Concert;
import com.ject.highfive.concert.entity.ConcertTeam;
import com.ject.highfive.concert.entity.ConcertTeamStatus;
import com.ject.highfive.concert.entity.Genre;
import com.ject.highfive.support.MySqlRepositoryTest;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@MySqlRepositoryTest
class ConcertTeamRepositoryTest {

  private static final Long HOST_BAND_ID = 3L;

  @Autowired ConcertRepository concertRepository;
  @Autowired ConcertTeamRepository concertTeamRepository;

  private Concert savedConcert() {
    return concertRepository.saveAndFlush(
        Concert.builder()
            .hostBandId(HOST_BAND_ID)
            .createdBy(1L)
            .title("겨울 합동공연")
            .recruitTeamCount(3)
            .performDate(LocalDate.of(2999, 12, 20))
            .genres(List.of(Genre.ROCK))
            .build());
  }

  private ConcertTeam savedTeam(Concert concert, Long bandId) {
    return concertTeamRepository.saveAndFlush(
        ConcertTeam.builder().concert(concert).bandId(bandId).build());
  }

  @Test
  @DisplayName("합류 수에서 주최 팀, 나간 팀, 강퇴된 팀, 다른 공연의 팀은 빠진다")
  void countJoinedGuestTeams() {
    Concert concert = savedConcert();
    savedTeam(concert, HOST_BAND_ID);
    savedTeam(concert, 11L);
    savedTeam(concert, 12L);
    savedTeam(concert, 13L).leave();
    savedTeam(concert, 14L).kick();
    savedTeam(savedConcert(), 11L);
    concertTeamRepository.flush();

    assertThat(
            concertTeamRepository.countByConcertIdAndStatusAndBandIdNot(
                concert.getId(), ConcertTeamStatus.JOINED, HOST_BAND_ID))
        .isEqualTo(2);
  }
}
