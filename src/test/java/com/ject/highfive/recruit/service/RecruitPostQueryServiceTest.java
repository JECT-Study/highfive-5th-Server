package com.ject.highfive.recruit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.ject.highfive.concert.entity.Concert;
import com.ject.highfive.concert.entity.ConcertTeamStatus;
import com.ject.highfive.concert.entity.Genre;
import com.ject.highfive.concert.repository.ConcertTeamRepository;
import com.ject.highfive.concert.repository.JoinedTeamCount;
import com.ject.highfive.global.common.response.PageResponse;
import com.ject.highfive.recruit.dto.RecruitPostSearchRequest;
import com.ject.highfive.recruit.dto.RecruitPostSummaryResponse;
import com.ject.highfive.recruit.entity.RecruitPost;
import com.ject.highfive.recruit.entity.RecruitPostStatus;
import com.ject.highfive.recruit.entity.TeamType;
import com.ject.highfive.recruit.entity.WantedCareerLevel;
import com.ject.highfive.recruit.repository.RecruitPostFavoriteRepository;
import com.ject.highfive.recruit.repository.RecruitPostRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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
}
