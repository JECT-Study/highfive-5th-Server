package com.ject.highfive.concert.entity;

import com.ject.highfive.global.common.entity.BaseTimeEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "concerts")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Concert extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "host_band_id", nullable = false)
  private Long hostBandId;

  @Column(name = "created_by", nullable = false)
  private Long createdBy;

  @Column(name = "updated_by", nullable = false)
  private Long updatedBy;

  @Column(name = "title", nullable = false, length = 100)
  private String title;

  @Column(name = "recruit_team_count", nullable = false)
  private int recruitTeamCount;

  @Column(name = "perform_date")
  private LocalDate performDate;

  @Column(name = "start_time")
  private LocalTime startTime;

  @Column(name = "end_time")
  private LocalTime endTime;

  @Column(name = "region", length = 50)
  private String region;

  @Column(name = "place", length = 100)
  private String place;

  @Column(name = "address", length = 200)
  private String address;

  @Column(name = "rental_fee")
  private Integer rentalFee;

  @Column(name = "poster_file_id", length = 500)
  private String posterFileId;

  @Column(name = "apply_url", length = 500)
  private String applyUrl;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private ConcertStatus status;

  @Getter(AccessLevel.NONE)
  @ElementCollection
  @CollectionTable(name = "concert_genres", joinColumns = @JoinColumn(name = "concert_id"))
  @Enumerated(EnumType.STRING)
  @Column(name = "genre_code", nullable = false, length = 30)
  private Set<Genre> genres = new HashSet<>();

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  @Builder
  private Concert(
      Long hostBandId,
      Long createdBy,
      String title,
      int recruitTeamCount,
      LocalDate performDate,
      LocalTime startTime,
      LocalTime endTime,
      String region,
      String place,
      String address,
      Integer rentalFee,
      String posterFileId,
      Collection<Genre> genres) {
    this.hostBandId = hostBandId;
    this.createdBy = createdBy;
    this.updatedBy = createdBy;
    this.title = title;
    this.recruitTeamCount = recruitTeamCount;
    this.performDate = performDate;
    this.startTime = startTime;
    this.endTime = endTime;
    this.region = region;
    this.place = place;
    this.address = address;
    this.rentalFee = rentalFee;
    this.posterFileId = posterFileId;
    this.status = ConcertStatus.PREPARING;
    changeGenres(genres);
  }

  public Set<Genre> getGenres() {
    return Collections.unmodifiableSet(genres);
  }

  // 빠진 장르는 행이 지워지고(hard delete, 의사결정 D9) 새 장르는 추가된다
  public void changeGenres(Collection<Genre> requested) {
    Set<Genre> wanted = new HashSet<>(requested);
    genres.retainAll(wanted);
    genres.addAll(wanted);
  }
}
