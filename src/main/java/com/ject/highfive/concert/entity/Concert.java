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
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SortNatural;

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

  // 업로드 API가 돌려준 fileUrl (프로필 사진 profileImageUrl과 같은 방식)
  @Column(name = "poster_image_url", length = 500)
  private String posterImageUrl;

  @Column(name = "apply_url", length = 500)
  private String applyUrl;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private ConcertStatus status;

  // HashSet은 JVM마다 순서가 달라져 화면 순서가 흔들린다. enum 선언 순서로 정렬한다
  @Getter(AccessLevel.NONE)
  @ElementCollection
  @CollectionTable(name = "concert_genres", joinColumns = @JoinColumn(name = "concert_id"))
  @Enumerated(EnumType.STRING)
  @Column(name = "genre_code", nullable = false, length = 30)
  @SortNatural
  private SortedSet<Genre> genres = new TreeSet<>();

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
      String posterImageUrl,
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
    this.posterImageUrl = posterImageUrl;
    this.status = ConcertStatus.PREPARING;
    changeGenres(genres);
  }

  public SortedSet<Genre> getGenres() {
    return Collections.unmodifiableSortedSet(genres);
  }

  // PUT 전체 교체(API 명세 Q5)라 선택 값이 null이면 미정으로 지운다.
  // 장르를 먼저 바꿔, 장르가 거부되면 다른 필드도 바뀌지 않게 한다
  public void update(
      String title,
      Collection<Genre> genres,
      int recruitTeamCount,
      LocalDate performDate,
      LocalTime startTime,
      LocalTime endTime,
      String place,
      String address,
      String region,
      Integer rentalFee,
      String posterImageUrl,
      Long updatedBy) {
    changeGenres(genres);
    this.title = title;
    this.recruitTeamCount = recruitTeamCount;
    this.performDate = performDate;
    this.startTime = startTime;
    this.endTime = endTime;
    this.place = place;
    this.address = address;
    this.region = region;
    this.rentalFee = rentalFee;
    this.posterImageUrl = posterImageUrl;
    this.updatedBy = updatedBy;
  }

  // 빠진 장르는 행이 지워지고(hard delete, 의사결정 D9) 새 장르는 추가된다
  public void changeGenres(Collection<Genre> requested) {
    if (requested == null || requested.isEmpty() || requested.stream().anyMatch(Objects::isNull)) {
      throw new IllegalArgumentException("공연 장르는 null 없이 하나 이상이어야 합니다.");
    }
    Set<Genre> wanted = new HashSet<>(requested);
    genres.retainAll(wanted);
    genres.addAll(wanted);
  }
}
