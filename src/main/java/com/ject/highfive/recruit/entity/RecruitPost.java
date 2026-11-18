package com.ject.highfive.recruit.entity;

import com.ject.highfive.concert.entity.Concert;
import com.ject.highfive.global.common.entity.BaseTimeEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
@Table(name = "recruit_posts")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecruitPost extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  // 공연당 공고는 하나 (UNIQUE). 삭제한 공고도 행이 남아 다시 작성할 수 없다 (스펙 4.5)
  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "concert_id", nullable = false, unique = true)
  private Concert concert;

  @Column(name = "author_user_id", nullable = false)
  private Long authorUserId;

  @Enumerated(EnumType.STRING)
  @Column(name = "wanted_career_level", nullable = false, length = 20)
  private WantedCareerLevel wantedCareerLevel;

  // HashSet은 JVM마다 순서가 달라져 화면 순서가 흔들린다. enum 선언 순서로 정렬한다
  @Getter(AccessLevel.NONE)
  @ElementCollection
  @CollectionTable(name = "recruit_post_team_types", joinColumns = @JoinColumn(name = "post_id"))
  @Enumerated(EnumType.STRING)
  @Column(name = "team_type", nullable = false, length = 30)
  @SortNatural
  private SortedSet<TeamType> teamTypes = new TreeSet<>();

  @Column(name = "description", nullable = false, length = 500)
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 10)
  private RecruitPostStatus status;

  @Column(name = "deadline", nullable = false)
  private LocalDate deadline;

  // 값은 220의 delete()가 채운다
  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  @Builder
  private RecruitPost(
      Concert concert,
      Long authorUserId,
      WantedCareerLevel wantedCareerLevel,
      Collection<TeamType> teamTypes,
      LocalDate deadline,
      String description) {
    this.concert = concert;
    this.authorUserId = authorUserId;
    this.wantedCareerLevel = wantedCareerLevel;
    this.deadline = deadline;
    this.description = description;
    this.status = RecruitPostStatus.RECRUITING;
    changeTeamTypes(teamTypes);
  }

  public SortedSet<TeamType> getTeamTypes() {
    return Collections.unmodifiableSortedSet(teamTypes);
  }

  // 마감 판정은 이 메서드 하나에서만 한다 (의사결정 D10). 마감일 당일까지는 모집 중
  public boolean isClosed(LocalDate today) {
    return status == RecruitPostStatus.CLOSED || deadline.isBefore(today);
  }

  // 빠진 유형은 행이 지워지고(hard delete, 의사결정 D9) 새 유형은 추가된다. 220의 update도 쓴다
  private void changeTeamTypes(Collection<TeamType> requested) {
    if (requested == null || requested.isEmpty() || requested.stream().anyMatch(Objects::isNull)) {
      throw new IllegalArgumentException("모집 팀 유형은 null 없이 하나 이상이어야 합니다.");
    }
    Set<TeamType> wanted = new HashSet<>(requested);
    teamTypes.retainAll(wanted);
    teamTypes.addAll(wanted);
  }
}
