package com.ject.highfive.recruit.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ject.highfive.concert.entity.Concert;
import com.ject.highfive.concert.entity.Genre;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RecruitPostTest {

  // isClosed는 오늘을 인자로 받으므로 고정 날짜로 경계를 시험한다
  private static final LocalDate TODAY = LocalDate.of(2026, 11, 17);

  private Concert concert() {
    return Concert.builder()
        .hostBandId(3L)
        .createdBy(1L)
        .title("겨울 합동공연")
        .recruitTeamCount(3)
        .performDate(LocalDate.of(2026, 12, 20))
        .genres(List.of(Genre.ROCK))
        .build();
  }

  private RecruitPost post(LocalDate deadline, List<TeamType> teamTypes) {
    return RecruitPost.builder()
        .concert(concert())
        .authorUserId(10L)
        .wantedCareerLevel(WantedCareerLevel.Y1_TO_3)
        .teamTypes(teamTypes)
        .deadline(deadline)
        .description("12월 홍대에서 함께할 팀을 찾습니다.")
        .build();
  }

  private RecruitPost post(LocalDate deadline, TeamType... teamTypes) {
    return post(deadline, List.of(teamTypes));
  }

  @Test
  @DisplayName("새 공고는 모집 중이고, 마감일 당일까지는 마감이 아니다")
  void newPostIsRecruitingAndOpen() {
    RecruitPost post = post(TODAY, TeamType.ANYONE);

    assertThat(post.getStatus()).isEqualTo(RecruitPostStatus.RECRUITING);
    assertThat(post.isClosed(TODAY)).isFalse();
  }

  @Test
  @DisplayName("마감일이 지나면 status가 모집 중이어도 마감이다")
  void closedWhenDeadlinePassed() {
    RecruitPost post = post(TODAY.minusDays(1), TeamType.ANYONE);

    assertThat(post.getStatus()).isEqualTo(RecruitPostStatus.RECRUITING);
    assertThat(post.isClosed(TODAY)).isTrue();
  }

  @Test
  @DisplayName("같은 팀 유형이 여러 번 와도 하나로 합친다")
  void duplicateTeamTypesCollapse() {
    RecruitPost post = post(TODAY, TeamType.ANYONE, TeamType.ANYONE);

    assertThat(post.getTeamTypes()).containsExactly(TeamType.ANYONE);
  }

  @Test
  @DisplayName("팀 유형을 섞어 넣어도 선언 순서로 나온다")
  void teamTypesFollowDeclarationOrder() {
    RecruitPost post = post(TODAY, TeamType.ANYONE, TeamType.FIRST_PERFORMANCE, TeamType.HOBBY);

    assertThat(post.getTeamTypes())
        .containsExactly(TeamType.FIRST_PERFORMANCE, TeamType.HOBBY, TeamType.ANYONE);
  }

  @Test
  @DisplayName("팀 유형 선언 순서는 ERD 주석 순서와 같다 (화면 표시 순서)")
  void declarationOrderMatchesErdComment() {
    assertThat(TeamType.values())
        .containsExactly(
            TeamType.FIRST_PERFORMANCE,
            TeamType.EXPERIENCED,
            TeamType.ACTIVE,
            TeamType.HOBBY,
            TeamType.ORIGINAL_SONGS,
            TeamType.ANYONE);
  }

  @Test
  @DisplayName("팀 유형이 null이거나 비어 있거나 null 원소가 있으면 거부한다")
  void invalidTeamTypesRejected() {
    assertThatThrownBy(() -> post(TODAY, (List<TeamType>) null))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> post(TODAY, List.of())).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> post(TODAY, Collections.singletonList(null)))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("getTeamTypes()로 받은 Set은 고칠 수 없다")
  void teamTypesViewIsReadOnly() {
    RecruitPost post = post(TODAY, TeamType.ANYONE);

    assertThatThrownBy(() -> post.getTeamTypes().add(TeamType.HOBBY))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  @DisplayName("수정하면 네 필드를 통째로 바꾸고, 빠진 팀 유형은 지우고 새 유형을 넣는다")
  void updateReplacesAllFields() {
    RecruitPost post = post(TODAY, TeamType.FIRST_PERFORMANCE, TeamType.HOBBY);

    post.update(
        WantedCareerLevel.OVER_3Y,
        List.of(TeamType.ANYONE, TeamType.HOBBY),
        TODAY.plusDays(10),
        "소개를 고쳤습니다.");

    assertThat(post.getWantedCareerLevel()).isEqualTo(WantedCareerLevel.OVER_3Y);
    assertThat(post.getTeamTypes()).containsExactly(TeamType.HOBBY, TeamType.ANYONE);
    assertThat(post.getDeadline()).isEqualTo(TODAY.plusDays(10));
    assertThat(post.getDescription()).isEqualTo("소개를 고쳤습니다.");
    assertThat(post.getStatus()).isEqualTo(RecruitPostStatus.RECRUITING);
  }

  @Test
  @DisplayName("팀 유형이 비어 있으면 수정을 거부하고 다른 필드도 바꾸지 않는다")
  void invalidTeamTypesLeavePostUnchanged() {
    RecruitPost post = post(TODAY, TeamType.ANYONE);

    assertThatThrownBy(
            () -> post.update(WantedCareerLevel.OVER_3Y, List.of(), TODAY.plusDays(10), "고침"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(post.getWantedCareerLevel()).isEqualTo(WantedCareerLevel.Y1_TO_3);
    assertThat(post.getDeadline()).isEqualTo(TODAY);
    assertThat(post.getDescription()).isEqualTo("12월 홍대에서 함께할 팀을 찾습니다.");
    assertThat(post.getTeamTypes()).containsExactly(TeamType.ANYONE);
  }

  @Test
  @DisplayName("직접 마감하면 마감일이 남아 있어도 마감이다")
  void closeMakesPostClosedRegardlessOfDeadline() {
    RecruitPost post = post(TODAY.plusDays(30), TeamType.ANYONE);

    post.close();

    assertThat(post.getStatus()).isEqualTo(RecruitPostStatus.CLOSED);
    assertThat(post.isClosed(TODAY)).isTrue();
  }

  @Test
  @DisplayName("삭제하면 삭제 시각만 기록하고 상태는 그대로다")
  void deleteRecordsDeletedAt() {
    RecruitPost post = post(TODAY, TeamType.ANYONE);

    post.delete();

    assertThat(post.getDeletedAt()).isNotNull();
    assertThat(post.getStatus()).isEqualTo(RecruitPostStatus.RECRUITING);
  }
}
