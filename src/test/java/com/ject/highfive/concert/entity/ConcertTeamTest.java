package com.ject.highfive.concert.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ject.highfive.concert.repository.ConcertRepository;
import com.ject.highfive.concert.repository.ConcertTeamRepository;
import com.ject.highfive.support.RepositoryTest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

@RepositoryTest
class ConcertTeamTest {

  @Autowired ConcertRepository concertRepository;
  @Autowired ConcertTeamRepository concertTeamRepository;
  @Autowired TestEntityManager em;

  private Concert savedConcert() {
    return concertRepository.saveAndFlush(
        Concert.builder()
            .hostBandId(3L)
            .createdBy(1L)
            .title("겨울 합동공연")
            .recruitTeamCount(3)
            .performDate(LocalDate.of(2026, 12, 20))
            .genres(List.of(Genre.ROCK))
            .build());
  }

  private ConcertTeam team(Concert concert, Long bandId, Integer order) {
    return ConcertTeam.builder().concert(concert).bandId(bandId).lineupOrder(order).build();
  }

  @Test
  @DisplayName("참여 팀을 저장하면 JOINED 상태와 합류 시각이 생긴다")
  void saveTeam() {
    ConcertTeam saved = concertTeamRepository.saveAndFlush(team(savedConcert(), 3L, 1));
    em.clear();

    ConcertTeam found = concertTeamRepository.findById(saved.getId()).orElseThrow();
    assertThat(found.getStatus()).isEqualTo(ConcertTeamStatus.JOINED);
    assertThat(found.getBandId()).isEqualTo(3L);
    assertThat(found.getLineupOrder()).isEqualTo(1);
    assertThat(found.getCreatedAt()).isNotNull();
    assertThat(found.getUpdatedAt()).isNotNull();
  }

  @Test
  @DisplayName("같은 공연에 같은 밴드를 두 번 넣으면 UNIQUE 제약 위반")
  void duplicateTeamViolatesUnique() {
    Concert concert = savedConcert();
    concertTeamRepository.saveAndFlush(team(concert, 3L, 1));

    assertThatThrownBy(() -> concertTeamRepository.saveAndFlush(team(concert, 3L, 2)))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  @DisplayName("나간 팀이 다시 합류하면 같은 행이 JOINED가 되고 첫 합류 시각은 유지된다")
  void leaveThenRejoinReusesRow() {
    ConcertTeam saved = concertTeamRepository.saveAndFlush(team(savedConcert(), 7L, 2));
    Long id = saved.getId();
    em.clear();
    LocalDateTime firstJoinedAt = concertTeamRepository.findById(id).orElseThrow().getCreatedAt();

    concertTeamRepository.findById(id).orElseThrow().leave();
    em.flush();
    em.clear();
    assertThat(concertTeamRepository.findById(id).orElseThrow().getStatus())
        .isEqualTo(ConcertTeamStatus.LEFT);

    concertTeamRepository.findById(id).orElseThrow().rejoin();
    em.flush();
    em.clear();

    ConcertTeam rejoined = concertTeamRepository.findById(id).orElseThrow();
    assertThat(rejoined.getStatus()).isEqualTo(ConcertTeamStatus.JOINED);
    assertThat(rejoined.getCreatedAt()).isEqualTo(firstJoinedAt);
    assertThat(concertTeamRepository.count()).isEqualTo(1);
  }

  @Test
  @DisplayName("내보낸 팀은 KICKED 상태가 된다")
  void kick() {
    ConcertTeam saved = concertTeamRepository.saveAndFlush(team(savedConcert(), 9L, 3));
    saved.kick();
    em.flush();
    em.clear();

    assertThat(concertTeamRepository.findById(saved.getId()).orElseThrow().getStatus())
        .isEqualTo(ConcertTeamStatus.KICKED);
  }
}
