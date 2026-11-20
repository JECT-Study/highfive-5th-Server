package com.ject.highfive.recruit.dto;

import com.ject.highfive.concert.entity.Concert;
import com.ject.highfive.concert.entity.Genre;
import com.ject.highfive.recruit.entity.RecruitPost;
import com.ject.highfive.recruit.entity.RecruitPostStatus;
import com.ject.highfive.recruit.entity.WantedCareerLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record RecruitPostSummaryResponse(
    Long recruitPostId,
    Long concertId,
    String title,
    String posterImageUrl,
    List<Genre> genres,
    String region,
    LocalDate performDate,
    LocalTime startTime,
    @Schema(description = "마감일이 지났거나 직접 마감하면 CLOSED") RecruitPostStatus status,
    LocalDate deadline,
    WantedCareerLevel wantedCareerLevel,
    @Schema(description = "합류한 팀 수(주최 팀 제외)") int joinedTeamCount,
    @Schema(description = "모집 팀 수(주최 팀 제외)") int recruitTeamCount,
    boolean isFavorite,
    @Schema(description = "내가 주최 대표자인 공고. 지금은 항상 false") boolean isMine) {

  public static RecruitPostSummaryResponse from(
      RecruitPost post, int joinedTeamCount, boolean favorite, LocalDate today) {
    Concert concert = post.getConcert();
    return new RecruitPostSummaryResponse(
        post.getId(),
        concert.getId(),
        concert.getTitle(),
        concert.getPosterImageUrl(),
        List.copyOf(concert.getGenres()),
        concert.getRegion(),
        concert.getPerformDate(),
        concert.getStartTime(),
        post.isClosed(today) ? RecruitPostStatus.CLOSED : RecruitPostStatus.RECRUITING,
        post.getDeadline(),
        post.getWantedCareerLevel(),
        joinedTeamCount,
        concert.getRecruitTeamCount(),
        favorite,
        // TODO: JECT-248 #58 머지 후 요청한 사용자가 주최 대표자인지 판정한다
        false);
  }
}
