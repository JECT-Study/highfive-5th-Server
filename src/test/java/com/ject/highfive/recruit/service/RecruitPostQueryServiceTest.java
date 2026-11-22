package com.ject.highfive.recruit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.ject.highfive.concert.dummy.DummyBandStore;
import com.ject.highfive.concert.entity.Concert;
import com.ject.highfive.concert.entity.ConcertTeam;
import com.ject.highfive.concert.entity.ConcertTeamStatus;
import com.ject.highfive.concert.entity.Genre;
import com.ject.highfive.concert.repository.ConcertTeamRepository;
import com.ject.highfive.concert.repository.JoinedTeamCount;
import com.ject.highfive.global.common.response.PageResponse;
import com.ject.highfive.global.exception.BusinessException;
import com.ject.highfive.recruit.dto.RecruitPostDetailResponse;
import com.ject.highfive.recruit.dto.RecruitPostSearchRequest;
import com.ject.highfive.recruit.dto.RecruitPostSummaryResponse;
import com.ject.highfive.recruit.entity.RecruitPost;
import com.ject.highfive.recruit.entity.RecruitPostStatus;
import com.ject.highfive.recruit.entity.TeamType;
import com.ject.highfive.recruit.entity.WantedCareerLevel;
import com.ject.highfive.recruit.exception.RecruitErrorCode;
import com.ject.highfive.recruit.repository.RecruitPostFavoriteRepository;
import com.ject.highfive.recruit.repository.RecruitPostRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RecruitPostQueryServiceTest {

  private static final Long USER_ID = 7L;
  private static final RecruitPostSearchRequest NO_CONDITION =
      new RecruitPostSearchRequest(null, null, null, null);

  @Mock RecruitPostRepository recruitPostRepository;
  @Mock ConcertTeamRepository concertTeamRepository;
  @Mock RecruitPostFavoriteRepository favoriteRepository;
  @Spy DummyBandStore dummyBandStore = new DummyBandStore();
  @InjectMocks RecruitPostQueryService service;

  private RecruitPost post(Long postId, Long concertId, LocalDate deadline) {
    Concert concert =
        Concert.builder()
            .hostBandId(3L)
            .createdBy(1L)
            .title("겨울 합동공연")
            .recruitTeamCount(3)
            .performDate(LocalDate.of(2999, 12, 20))
            .region("SEOUL")
            .genres(List.of(Genre.INDIE, Genre.ROCK))
            .build();
    ReflectionTestUtils.setField(concert, "id", concertId);
    RecruitPost post =
        RecruitPost.builder()
            .concert(concert)
            .authorUserId(10L)
            .wantedCareerLevel(WantedCareerLevel.Y1_TO_3)
            .teamTypes(List.of(TeamType.ANYONE))
            .deadline(deadline)
            .description("소개")
            .build();
    ReflectionTestUtils.setField(post, "id", postId);
    return post;
  }

  private JoinedTeamCount count(Long concertId, long joined) {
    return new JoinedTeamCount() {
      @Override
      public Long getConcertId() {
        return concertId;
      }

      @Override
      public Long getJoinedTeamCount() {
        return joined;
      }
    };
  }

  @SuppressWarnings("unchecked")
  private void givenPage(List<RecruitPost> posts, Pageable pageable, long total) {
    given(recruitPostRepository.findAll(any(Specification.class), any(Pageable.class)))
        .willReturn(new PageImpl<>(posts, pageable, total));
  }

  @Test
  @DisplayName("공고·공연 값과 합류 팀 수·찜 여부를 묶어 응답을 만든다")
  void searchBuildsSummaries() {
    RecruitPost favorited = post(5L, 12L, LocalDate.of(2999, 12, 1));
    RecruitPost notFavorited = post(9L, 15L, LocalDate.of(2999, 12, 1));
    givenPage(List.of(favorited, notFavorited), PageRequest.of(0, 10), 2);
    given(
            concertTeamRepository.countGuestTeamsByConcertIds(
                anyCollection(), eq(ConcertTeamStatus.JOINED)))
        .willReturn(List.of(count(12L, 2)));
    given(favoriteRepository.findRecruitPostIdsByUserId(eq(USER_ID), anyCollection()))
        .willReturn(Set.of(5L));

    PageResponse<RecruitPostSummaryResponse> result = service.search(NO_CONDITION, 1, 10, USER_ID);

    RecruitPostSummaryResponse first = result.content().get(0);
    assertThat(first.recruitPostId()).isEqualTo(5L);
    assertThat(first.concertId()).isEqualTo(12L);
    assertThat(first.title()).isEqualTo("겨울 합동공연");
    assertThat(first.genres()).containsExactly(Genre.ROCK, Genre.INDIE); // enum 선언 순서
    assertThat(first.region()).isEqualTo("SEOUL");
    assertThat(first.wantedCareerLevel()).isEqualTo(WantedCareerLevel.Y1_TO_3);
    assertThat(first.status()).isEqualTo(RecruitPostStatus.RECRUITING);
    assertThat(first.joinedTeamCount()).isEqualTo(2);
    assertThat(first.recruitTeamCount()).isEqualTo(3);
    assertThat(first.isFavorite()).isTrue();
    assertThat(first.isMine()).isFalse();

    RecruitPostSummaryResponse second = result.content().get(1);
    assertThat(second.joinedTeamCount()).isZero(); // 팀 수 결과에 없는 공연
    assertThat(second.isFavorite()).isFalse();
    assertThat(result.page()).isEqualTo(1);
    assertThat(result.totalElements()).isEqualTo(2);
  }

  @Test
  @DisplayName("마감일이 지났거나 직접 마감한 공고는 CLOSED로 내려준다")
  void searchComputesClosedStatus() {
    RecruitPost deadlinePassed = post(5L, 12L, LocalDate.of(2000, 1, 1));
    RecruitPost manuallyClosed = post(9L, 15L, LocalDate.of(2999, 12, 1));
    manuallyClosed.close();
    givenPage(List.of(deadlinePassed, manuallyClosed), PageRequest.of(0, 10), 2);

    PageResponse<RecruitPostSummaryResponse> result = service.search(NO_CONDITION, 1, 10, USER_ID);

    assertThat(result.content())
        .extracting(RecruitPostSummaryResponse::status)
        .containsOnly(RecruitPostStatus.CLOSED);
  }

  @Test
  @DisplayName("1부터 센 page를 0부터로 바꾸고 최신 등록순 정렬을 붙여 조회한다")
  void searchPassesPageAndLatestSort() {
    givenPage(List.of(), PageRequest.of(1, 20), 0);

    service.search(NO_CONDITION, 2, 20, USER_ID);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
    verify(recruitPostRepository).findAll(any(Specification.class), captor.capture());
    assertThat(captor.getValue().getPageNumber()).isEqualTo(1);
    assertThat(captor.getValue().getPageSize()).isEqualTo(20);
    assertThat(captor.getValue().getSort())
        .isEqualTo(Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
  }

  @Test
  @DisplayName("결과가 없으면 합류 팀 수·찜 조회를 하지 않는다")
  void emptyPageSkipsSideQueries() {
    givenPage(List.of(), PageRequest.of(0, 10), 0);

    PageResponse<RecruitPostSummaryResponse> result = service.search(NO_CONDITION, 1, 10, USER_ID);

    assertThat(result.content()).isEmpty();
    verifyNoInteractions(concertTeamRepository, favoriteRepository);
  }

  @Test
  @DisplayName("페이지가 결과 범위를 넘으면 내용은 비어도 전체 개수는 유지한다")
  void pageBeyondResultsKeepsTotal() {
    givenPage(List.of(), PageRequest.of(4, 10), 3);

    PageResponse<RecruitPostSummaryResponse> result = service.search(NO_CONDITION, 5, 10, USER_ID);

    assertThat(result.content()).isEmpty();
    assertThat(result.page()).isEqualTo(5);
    assertThat(result.totalElements()).isEqualTo(3);
    assertThat(result.hasNext()).isFalse();
  }

  private static final Long POST_ID = 5L;
  private static final Long CONCERT_ID = 12L;
  private static final LocalDate OPEN_DEADLINE = LocalDate.now().plusDays(10);

  private ConcertTeam team(Long id, Concert concert, Long bandId, Integer lineupOrder) {
    ConcertTeam team =
        ConcertTeam.builder().concert(concert).bandId(bandId).lineupOrder(lineupOrder).build();
    ReflectionTestUtils.setField(team, "id", id);
    return team;
  }

  private RecruitPostDetailResponse detail(RecruitPost post, List<ConcertTeam> teams) {
    given(
            recruitPostRepository.findWithConcertByIdAndDeletedAtIsNullAndConcertDeletedAtIsNull(
                POST_ID))
        .willReturn(Optional.of(post));
    given(concertTeamRepository.findLineup(CONCERT_ID, ConcertTeamStatus.JOINED)).willReturn(teams);
    return service.getDetail(POST_ID, USER_ID);
  }

  @Test
  @DisplayName("팀은 라인업 조회가 준 순서 그대로 나오고 주최 팀이 표시된다")
  void teamsKeepLineupOrder() {
    RecruitPost post = post(POST_ID, CONCERT_ID, OPEN_DEADLINE);
    Concert concert = post.getConcert();

    RecruitPostDetailResponse response =
        detail(
            post,
            List.of(
                team(21L, concert, 3L, 1),
                team(24L, concert, 4L, 2),
                team(22L, concert, 1L, null),
                team(23L, concert, 2L, null)));

    assertThat(response.teams()).extracting(t -> t.band().bandId()).containsExactly(3L, 4L, 1L, 2L);
    assertThat(response.teams())
        .extracting(RecruitPostDetailResponse.TeamInfo::isHost)
        .containsExactly(true, false, false, false);
    RecruitPostDetailResponse.BandInfo host = response.teams().get(0).band();
    assertThat(host.name()).isEqualTo("소음공장");
    assertThat(host.genres()).containsExactly(Genre.ROCK);
    assertThat(host.memberCount()).isEqualTo(4);
  }

  @Test
  @DisplayName("합류 팀 수는 주최 팀을 빼고 세고, 모집 팀 수는 공연 값이다")
  void joinedTeamCountExcludesHost() {
    RecruitPost post = post(POST_ID, CONCERT_ID, OPEN_DEADLINE);
    Concert concert = post.getConcert();

    RecruitPostDetailResponse response =
        detail(post, List.of(team(21L, concert, 3L, 1), team(22L, concert, 1L, 2)));

    assertThat(response.joinedTeamCount()).isEqualTo(1);
    assertThat(response.recruitTeamCount()).isEqualTo(3);
  }

  @Test
  @DisplayName("DummyBand 목록에 없는 밴드의 팀은 teams에서 빠지지만 합류 팀 수에는 센다")
  void bandMissingFromStoreExcludedButCounted() {
    RecruitPost post = post(POST_ID, CONCERT_ID, OPEN_DEADLINE);
    Concert concert = post.getConcert();

    RecruitPostDetailResponse response =
        detail(post, List.of(team(21L, concert, 3L, 1), team(22L, concert, 99L, 2)));

    assertThat(response.teams()).extracting(t -> t.band().bandId()).containsExactly(3L);
    assertThat(response.joinedTeamCount()).isEqualTo(1);
  }

  @Test
  @DisplayName("마감일이 지났으면 CLOSED이고 문의할 수 없다")
  void closedPostCannotInquire() {
    RecruitPostDetailResponse response =
        detail(post(POST_ID, CONCERT_ID, LocalDate.now().minusDays(1)), List.of());

    assertThat(response.status()).isEqualTo(RecruitPostStatus.CLOSED);
    assertThat(response.canInquire()).isFalse();
  }

  @Test
  @DisplayName("마감일이 오늘이면 아직 모집 중이고 문의할 수 있다 (D10)")
  void deadlineTodayStillRecruiting() {
    RecruitPostDetailResponse response =
        detail(post(POST_ID, CONCERT_ID, LocalDate.now()), List.of());

    assertThat(response.status()).isEqualTo(RecruitPostStatus.RECRUITING);
    assertThat(response.canInquire()).isTrue();
  }

  @Test
  @DisplayName("직접 마감한 공고는 마감일이 남아도 CLOSED이고 문의할 수 없다")
  void manuallyClosedCannotInquire() {
    RecruitPost post = post(POST_ID, CONCERT_ID, OPEN_DEADLINE);
    post.close();

    RecruitPostDetailResponse response = detail(post, List.of());

    assertThat(response.status()).isEqualTo(RecruitPostStatus.CLOSED);
    assertThat(response.canInquire()).isFalse();
  }

  @Test
  @DisplayName("찜한 공고면 isFavorite가 true다")
  void favoriteReflected() {
    given(favoriteRepository.findRecruitPostIdsByUserId(USER_ID, List.of(POST_ID)))
        .willReturn(Set.of(POST_ID));

    RecruitPostDetailResponse response =
        detail(post(POST_ID, CONCERT_ID, OPEN_DEADLINE), List.of());

    assertThat(response.isFavorite()).isTrue();
  }

  @Test
  @DisplayName("공고·공연 값이 그대로 담기고, 셋리스트는 빈 목록, isMine은 false다")
  void fieldsAndFixedValues() {
    RecruitPost post = post(POST_ID, CONCERT_ID, OPEN_DEADLINE);

    RecruitPostDetailResponse response = detail(post, List.of(team(21L, post.getConcert(), 3L, 1)));

    assertThat(response.recruitPostId()).isEqualTo(POST_ID);
    assertThat(response.deadline()).isEqualTo(OPEN_DEADLINE);
    assertThat(response.wantedCareerLevel()).isEqualTo(WantedCareerLevel.Y1_TO_3);
    assertThat(response.teamTypes()).containsExactly(TeamType.ANYONE);
    assertThat(response.description()).isEqualTo("소개");
    assertThat(response.concert().concertId()).isEqualTo(CONCERT_ID);
    assertThat(response.concert().title()).isEqualTo("겨울 합동공연");
    assertThat(response.concert().genres()).containsExactly(Genre.ROCK, Genre.INDIE);
    assertThat(response.concert().region()).isEqualTo("SEOUL");
    assertThat(response.teams().get(0).setlist()).isEmpty();
    assertThat(response.isMine()).isFalse();
    assertThat(response.isFavorite()).isFalse();
  }

  @Test
  @DisplayName("미정인 공연 날짜·시간·장소·대관비·포스터는 null로 담긴다")
  void undecidedConcertFieldsAreNull() {
    RecruitPostDetailResponse.ConcertInfo concert =
        detail(post(POST_ID, CONCERT_ID, OPEN_DEADLINE), List.of()).concert();

    assertThat(concert.startTime()).isNull();
    assertThat(concert.endTime()).isNull();
    assertThat(concert.place()).isNull();
    assertThat(concert.address()).isNull();
    assertThat(concert.rentalFee()).isNull();
    assertThat(concert.posterImageUrl()).isNull();
  }

  @Test
  @DisplayName("공고가 없거나 삭제됐거나 공연이 삭제됐으면 RECRUIT_001")
  void notFound() {
    given(
            recruitPostRepository.findWithConcertByIdAndDeletedAtIsNullAndConcertDeletedAtIsNull(
                POST_ID))
        .willReturn(Optional.empty());

    assertThatThrownBy(() -> service.getDetail(POST_ID, USER_ID))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(RecruitErrorCode.RECRUIT_POST_NOT_FOUND);
    verifyNoInteractions(concertTeamRepository, favoriteRepository);
  }
}
