package com.ject.highfive.recruit.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.ject.highfive.concert.entity.Concert;
import com.ject.highfive.concert.entity.Genre;
import com.ject.highfive.recruit.entity.RecruitPost;
import com.ject.highfive.recruit.entity.RecruitPostStatus;
import com.ject.highfive.recruit.entity.TeamType;
import com.ject.highfive.recruit.entity.WantedCareerLevel;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RecruitPostRequestTest {

  private static final LocalDate DEADLINE = LocalDate.of(2999, 12, 1);

  private Concert concert() {
    return Concert.builder()
        .hostBandId(3L)
        .createdBy(1L)
        .title("겨울 합동공연")
        .recruitTeamCount(3)
        .performDate(LocalDate.of(2999, 12, 20))
        .genres(List.of(Genre.ROCK))
        .build();
  }

  @Test
  @DisplayName("toEntity는 모든 필드를 옮기고 모집 중 상태로 만든다")
  void toEntityMapsAllFields() {
    Concert concert = concert();
    RecruitPostRequest request =
        new RecruitPostRequest(
            WantedCareerLevel.Y1_TO_3,
            List.of(TeamType.FIRST_PERFORMANCE, TeamType.ORIGINAL_SONGS),
            DEADLINE,
            "12월 홍대에서 함께할 팀을 찾습니다.");

    RecruitPost post = request.toEntity(concert, 10L);

    assertThat(post.getConcert()).isSameAs(concert);
    assertThat(post.getAuthorUserId()).isEqualTo(10L);
    assertThat(post.getWantedCareerLevel()).isEqualTo(WantedCareerLevel.Y1_TO_3);
    assertThat(post.getTeamTypes())
        .containsExactly(TeamType.FIRST_PERFORMANCE, TeamType.ORIGINAL_SONGS);
    assertThat(post.getDeadline()).isEqualTo(DEADLINE);
    assertThat(post.getDescription()).isEqualTo("12월 홍대에서 함께할 팀을 찾습니다.");
    assertThat(post.getStatus()).isEqualTo(RecruitPostStatus.RECRUITING);
  }

  @Test
  @DisplayName("toEntity는 중복된 팀 유형을 하나로 합치고 선언 순서로 정렬한다")
  void toEntityNormalizesTeamTypes() {
    RecruitPostRequest request =
        new RecruitPostRequest(
            WantedCareerLevel.Y1_TO_3,
            List.of(TeamType.ANYONE, TeamType.ANYONE, TeamType.HOBBY),
            DEADLINE,
            "소개");

    assertThat(request.toEntity(concert(), 10L).getTeamTypes())
        .containsExactly(TeamType.HOBBY, TeamType.ANYONE);
  }
}
