package com.ject.highfive.recruit.dto;

import com.ject.highfive.concert.dummy.DummyBand;
import com.ject.highfive.concert.entity.Concert;
import com.ject.highfive.concert.entity.ConcertTeam;
import com.ject.highfive.concert.entity.Genre;
import com.ject.highfive.recruit.entity.RecruitPost;
import com.ject.highfive.recruit.entity.RecruitPostStatus;
import com.ject.highfive.recruit.entity.TeamType;
import com.ject.highfive.recruit.entity.WantedCareerLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

public record RecruitPostDetailResponse(
    Long recruitPostId,
    @Schema(description = "마감일이 지났거나 직접 마감하면 CLOSED") RecruitPostStatus status,
    LocalDate deadline,
    WantedCareerLevel wantedCareerLevel,
    List<TeamType> teamTypes,
    String description,
    @Schema(description = "합류한 팀 수(주최 팀 제외)") int joinedTeamCount,
    @Schema(description = "모집 팀 수(주최 팀 제외)") int recruitTeamCount,
    ConcertInfo concert,
    @Schema(description = "주최 팀 + 합류한 팀. 라인업 순서") List<TeamInfo> teams,
    boolean isFavorite,
    @Schema(description = "내가 주최 대표자인 공고. 지금은 항상 false") boolean isMine,
    @Schema(description = "문의하기 버튼 활성 여부. 지금은 마감이 아니면 true") boolean canInquire) {

  // teams는 라인업 순서로 정렬된 JOINED 팀. DummyBand에 없는 팀은 teams에서 빼지만 합류 팀 수는 concert_teams 기준으로 센다
  public static RecruitPostDetailResponse from(
      RecruitPost post,
      List<ConcertTeam> teams,
      Map<Long, DummyBand> bands,
      boolean favorite,
      LocalDate today) {
    Concert concert = post.getConcert();
    Long hostBandId = concert.getHostBandId();
    boolean closed = post.isClosed(today);
    int joinedTeamCount =
        (int) teams.stream().filter(team -> !team.getBandId().equals(hostBandId)).count();
    List<TeamInfo> teamInfos =
        teams.stream()
            .filter(team -> bands.containsKey(team.getBandId()))
            .map(
                team ->
                    TeamInfo.from(bands.get(team.getBandId()), team.getBandId().equals(hostBandId)))
            .toList();
    return new RecruitPostDetailResponse(
        post.getId(),
        closed ? RecruitPostStatus.CLOSED : RecruitPostStatus.RECRUITING,
        post.getDeadline(),
        post.getWantedCareerLevel(),
        List.copyOf(post.getTeamTypes()),
        post.getDescription(),
        joinedTeamCount,
        concert.getRecruitTeamCount(),
        ConcertInfo.from(concert),
        teamInfos,
        favorite,
        // TODO: JECT-248 #58 머지 후 요청한 사용자가 주최 대표자인지 판정한다
        false,
        // TODO: JECT-248 #58 머지 후 내 밴드의 공고면 false로 한다
        !closed);
  }

  public record ConcertInfo(
      Long concertId,
      String title,
      List<Genre> genres,
      LocalDate performDate,
      LocalTime startTime,
      LocalTime endTime,
      String place,
      String address,
      String region,
      Integer rentalFee,
      String posterImageUrl) {

    static ConcertInfo from(Concert concert) {
      return new ConcertInfo(
          concert.getId(),
          concert.getTitle(),
          List.copyOf(concert.getGenres()),
          concert.getPerformDate(),
          concert.getStartTime(),
          concert.getEndTime(),
          concert.getPlace(),
          concert.getAddress(),
          concert.getRegion(),
          concert.getRentalFee(),
          concert.getPosterImageUrl());
    }
  }

  public record TeamInfo(
      boolean isHost,
      BandInfo band,
      @Schema(description = "셋리스트. 셋리스트 기능(JECT-234) 전까지 항상 빈 목록") List<SetlistSong> setlist) {

    static TeamInfo from(DummyBand band, boolean host) {
      return new TeamInfo(host, BandInfo.from(band), List.of());
    }
  }

  public record BandInfo(
      Long bandId,
      String name,
      String profileImageUrl,
      List<Genre> genres,
      @Schema(description = "밴드를 만들 때 입력한 팀원 수") int memberCount) {

    static BandInfo from(DummyBand band) {
      return new BandInfo(
          band.id(), band.name(), band.profileImageUrl(), band.genres(), band.memberCount());
    }
  }

  public record SetlistSong(String songTitle, String artist) {}
}
