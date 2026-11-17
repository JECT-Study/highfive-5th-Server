package com.ject.highfive.concert.entity;

import com.ject.highfive.global.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
    name = "concert_teams",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_concert_teams_concert_band",
            columnNames = {"concert_id", "band_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ConcertTeam extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "concert_id", nullable = false)
  private Concert concert;

  @Column(name = "band_id", nullable = false)
  private Long bandId;

  @Column(name = "lineup_order")
  private Integer lineupOrder;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 10)
  private ConcertTeamStatus status;

  @Builder
  private ConcertTeam(Concert concert, Long bandId, Integer lineupOrder) {
    this.concert = concert;
    this.bandId = bandId;
    this.lineupOrder = lineupOrder;
    this.status = ConcertTeamStatus.JOINED;
  }

  public void leave() {
    this.status = ConcertTeamStatus.LEFT;
  }

  public void kick() {
    this.status = ConcertTeamStatus.KICKED;
  }

  public void rejoin() {
    this.status = ConcertTeamStatus.JOINED;
  }
}
