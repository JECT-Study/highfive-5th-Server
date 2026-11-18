package com.ject.highfive.concert.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.ject.highfive.concert.dto.ConcertCreateRequest;
import com.ject.highfive.concert.dto.ConcertCreateResponse;
import com.ject.highfive.concert.dto.ConcertUpdateRequest;
import com.ject.highfive.concert.entity.Concert;
import com.ject.highfive.concert.entity.ConcertStatus;
import com.ject.highfive.concert.entity.ConcertTeam;
import com.ject.highfive.concert.entity.ConcertTeamStatus;
import com.ject.highfive.concert.entity.Genre;
import com.ject.highfive.concert.exception.ConcertErrorCode;
import com.ject.highfive.concert.repository.ConcertRepository;
import com.ject.highfive.concert.repository.ConcertTeamRepository;
import com.ject.highfive.global.exception.BusinessException;
import com.ject.highfive.recruit.entity.RecruitPost;
import com.ject.highfive.recruit.entity.RecruitPostStatus;
import com.ject.highfive.recruit.entity.TeamType;
import com.ject.highfive.recruit.entity.WantedCareerLevel;
import com.ject.highfive.recruit.repository.RecruitPostRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ConcertServiceTest {

  private static final Long USER_ID = 10L;
  private static final Long BAND_ID = 3L;
  private static final LocalDate FUTURE = LocalDate.of(2999, 12, 20);
  private static final LocalDate PAST = LocalDate.of(2000, 1, 1);
  private static final LocalTime START = LocalTime.of(19, 0);
  private static final LocalTime END = LocalTime.of(21, 0);
  private static final Long CONCERT_ID = 12L;
  private static final LocalDate DEADLINE = LocalDate.of(2999, 12, 10);

  @Mock ConcertRepository concertRepository;
  @Mock ConcertTeamRepository concertTeamRepository;
  @Mock RecruitPostRepository recruitPostRepository;
  @InjectMocks ConcertService service;

  private ConcertCreateRequest request(LocalDate performDate, LocalTime start, LocalTime end) {
    return new ConcertCreateRequest(
        BAND_ID,
        "겨울 합동공연",
        List.of(Genre.ROCK, Genre.INDIE),
        3,
        performDate,
        start,
        end,
        "홍대 롤링홀",
        "서울 마포구 어울마당로 35",
        "SEOUL",
        500000,
        "https://cdn.example.com/posters/1.jpg");
  }

  private void givenSaveAssignsId(Long id) {
    given(concertRepository.save(any(Concert.class)))
        .willAnswer(
            invocation -> {
              Concert concert = invocation.getArgument(0);
              ReflectionTestUtils.setField(concert, "id", id);
              return concert;
            });
  }

  private void assertError(Runnable call, ConcertErrorCode expected) {
    assertThatThrownBy(call::run)
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(expected);
  }

  private Concert existingConcert(LocalDate performDate) {
    Concert concert =
        Concert.builder()
            .hostBandId(BAND_ID)
            .createdBy(1L)
            .title("겨울 합동공연")
            .recruitTeamCount(3)
            .performDate(performDate)
            .genres(List.of(Genre.ROCK))
            .build();
    ReflectionTestUtils.setField(concert, "id", CONCERT_ID);
    given(concertRepository.findByIdAndDeletedAtIsNull(CONCERT_ID))
        .willReturn(Optional.of(concert));
    return concert;
  }

  private ConcertUpdateRequest updateRequest(
      LocalDate performDate, LocalTime start, LocalTime end, int recruitTeamCount) {
    return new ConcertUpdateRequest(
        "봄 합동공연",
        List.of(Genre.JAZZ),
        recruitTeamCount,
        performDate,
        start,
        end,
        null,
        null,
        null,
        null,
        null);
  }

  private ConcertUpdateRequest updateRequest(LocalDate performDate) {
    return updateRequest(performDate, START, END, 3);
  }

  private RecruitPost givenRecruitPost(Concert concert, LocalDate deadline) {
    RecruitPost post =
        RecruitPost.builder()
            .concert(concert)
            .authorUserId(USER_ID)
            .wantedCareerLevel(WantedCareerLevel.Y1_TO_3)
            .teamTypes(List.of(TeamType.ANYONE))
            .deadline(deadline)
            .description("함께할 팀을 찾습니다.")
            .build();
    given(recruitPostRepository.findByConcertIdAndDeletedAtIsNull(CONCERT_ID))
        .willReturn(Optional.of(post));
    return post;
  }

  private void givenJoinedGuestTeams(long count) {
    given(
            concertTeamRepository.countByConcertIdAndStatusAndBandIdNot(
                CONCERT_ID, ConcertTeamStatus.JOINED, BAND_ID))
        .willReturn(count);
  }

  @Test
  @DisplayName("공연과 주최 밴드(1번 순서, 합류 상태)를 함께 저장하고 공연 ID를 돌려준다")
  void createSavesConcertAndHostTeam() {
    givenSaveAssignsId(12L);

    ConcertCreateResponse response = service.create(USER_ID, request(FUTURE, START, END));

    assertThat(response.concertId()).isEqualTo(12L);

    ArgumentCaptor<Concert> concertCaptor = ArgumentCaptor.forClass(Concert.class);
    verify(concertRepository).save(concertCaptor.capture());
    Concert concert = concertCaptor.getValue();
    assertThat(concert.getHostBandId()).isEqualTo(BAND_ID);
    assertThat(concert.getCreatedBy()).isEqualTo(USER_ID);
    assertThat(concert.getUpdatedBy()).isEqualTo(USER_ID);
    assertThat(concert.getStatus()).isEqualTo(ConcertStatus.PREPARING);
    assertThat(concert.getTitle()).isEqualTo("겨울 합동공연");
    assertThat(concert.getGenres()).containsExactly(Genre.ROCK, Genre.INDIE);
    assertThat(concert.getRecruitTeamCount()).isEqualTo(3);
    assertThat(concert.getPerformDate()).isEqualTo(FUTURE);
    assertThat(concert.getStartTime()).isEqualTo(START);
    assertThat(concert.getEndTime()).isEqualTo(END);
    assertThat(concert.getPlace()).isEqualTo("홍대 롤링홀");
    assertThat(concert.getAddress()).isEqualTo("서울 마포구 어울마당로 35");
    assertThat(concert.getRegion()).isEqualTo("SEOUL");
    assertThat(concert.getRentalFee()).isEqualTo(500000);
    assertThat(concert.getPosterImageUrl()).isEqualTo("https://cdn.example.com/posters/1.jpg");

    ArgumentCaptor<ConcertTeam> teamCaptor = ArgumentCaptor.forClass(ConcertTeam.class);
    verify(concertTeamRepository).save(teamCaptor.capture());
    ConcertTeam hostTeam = teamCaptor.getValue();
    assertThat(hostTeam.getConcert()).isSameAs(concert);
    assertThat(hostTeam.getBandId()).isEqualTo(BAND_ID);
    assertThat(hostTeam.getLineupOrder()).isEqualTo(1);
    assertThat(hostTeam.getStatus()).isEqualTo(ConcertTeamStatus.JOINED);
  }

  @Test
  @DisplayName("중복된 장르는 하나로 합쳐 저장한다")
  void duplicateGenresCollapse() {
    givenSaveAssignsId(12L);
    ConcertCreateRequest request =
        new ConcertCreateRequest(
            BAND_ID,
            "겨울 합동공연",
            List.of(Genre.ROCK, Genre.INDIE, Genre.ROCK),
            3,
            FUTURE,
            START,
            END,
            null,
            null,
            null,
            null,
            null);

    service.create(USER_ID, request);

    ArgumentCaptor<Concert> concertCaptor = ArgumentCaptor.forClass(Concert.class);
    verify(concertRepository).save(concertCaptor.capture());
    assertThat(concertCaptor.getValue().getGenres()).containsExactly(Genre.ROCK, Genre.INDIE);
  }

  @Test
  @DisplayName("선택 값이 모두 미정(null)이어도 만들 수 있다")
  void undecidedOptionalValuesAllowed() {
    givenSaveAssignsId(12L);
    ConcertCreateRequest request =
        new ConcertCreateRequest(
            BAND_ID,
            "겨울 합동공연",
            List.of(Genre.ROCK),
            3,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null);

    assertThat(service.create(USER_ID, request).concertId()).isEqualTo(12L);
  }

  @Test
  @DisplayName("시작 시간만 있으면 CONCERT_010이고 저장하지 않는다")
  void onlyStartTimeRejected() {
    assertError(
        () -> service.create(USER_ID, request(FUTURE, START, null)),
        ConcertErrorCode.PERFORM_TIME_INCOMPLETE);
    verifyNoInteractions(concertRepository, concertTeamRepository);
  }

  @Test
  @DisplayName("종료 시간만 있으면 CONCERT_010이고 저장하지 않는다")
  void onlyEndTimeRejected() {
    assertError(
        () -> service.create(USER_ID, request(FUTURE, null, END)),
        ConcertErrorCode.PERFORM_TIME_INCOMPLETE);
    verifyNoInteractions(concertRepository, concertTeamRepository);
  }

  @Test
  @DisplayName("종료 시간이 시작 시간보다 이르면(자정을 넘기는 공연) 그대로 받는다")
  void endBeforeStartAllowed() {
    givenSaveAssignsId(12L);

    assertThat(
            service
                .create(USER_ID, request(FUTURE, LocalTime.of(23, 0), LocalTime.of(1, 0)))
                .concertId())
        .isEqualTo(12L);
  }

  @Test
  @DisplayName("공연 날짜가 오늘 이전이면 CONCERT_002이고 저장하지 않는다")
  void pastPerformDateRejected() {
    assertError(
        () -> service.create(USER_ID, request(PAST, START, END)),
        ConcertErrorCode.PERFORM_DATE_BEFORE_TODAY);
    verifyNoInteractions(concertRepository, concertTeamRepository);
  }

  @Test
  @DisplayName("공연 날짜가 오늘이면 만들 수 있다")
  void performDateTodayAllowed() {
    givenSaveAssignsId(12L);

    assertThat(service.create(USER_ID, request(LocalDate.now(), START, END)).concertId())
        .isEqualTo(12L);
  }

  @Test
  @DisplayName("시간 짝과 날짜가 둘 다 틀리면 시간 짝(CONCERT_010)을 먼저 알려 준다")
  void timePairCheckedBeforeDate() {
    assertError(
        () -> service.create(USER_ID, request(PAST, START, null)),
        ConcertErrorCode.PERFORM_TIME_INCOMPLETE);
  }

  @Test
  @DisplayName("수정에 성공하면 요청 값으로 공연을 바꾸고 수정자를 기록한다")
  void updateChangesConcert() {
    Concert concert = existingConcert(FUTURE);

    service.update(CONCERT_ID, USER_ID, updateRequest(LocalDate.of(2999, 11, 1), START, END, 5));

    assertThat(concert.getTitle()).isEqualTo("봄 합동공연");
    assertThat(concert.getGenres()).containsExactly(Genre.JAZZ);
    assertThat(concert.getRecruitTeamCount()).isEqualTo(5);
    assertThat(concert.getPerformDate()).isEqualTo(LocalDate.of(2999, 11, 1));
    assertThat(concert.getUpdatedBy()).isEqualTo(USER_ID);
    assertThat(concert.getCreatedBy()).isEqualTo(1L);
  }

  @Test
  @DisplayName("공연이 없거나 삭제됐으면 CONCERT_001이고 다른 조회를 하지 않는다")
  void updateMissingConcertRejected() {
    assertError(
        () -> service.update(CONCERT_ID, USER_ID, updateRequest(FUTURE)),
        ConcertErrorCode.CONCERT_NOT_FOUND);
    verifyNoInteractions(concertTeamRepository, recruitPostRepository);
  }

  @Test
  @DisplayName("시작 시간만 있으면 공연을 찾기 전에 CONCERT_010")
  void updateTimePairCheckedFirst() {
    assertError(
        () -> service.update(CONCERT_ID, USER_ID, updateRequest(FUTURE, START, null, 3)),
        ConcertErrorCode.PERFORM_TIME_INCOMPLETE);
    verifyNoInteractions(concertRepository, concertTeamRepository, recruitPostRepository);
  }

  @Test
  @DisplayName("지난 공연도 날짜를 그대로 두면 다른 정보를 고칠 수 있다")
  void unchangedPastDateAllowed() {
    Concert concert = existingConcert(PAST);

    service.update(CONCERT_ID, USER_ID, updateRequest(PAST));

    assertThat(concert.getTitle()).isEqualTo("봄 합동공연");
  }

  @Test
  @DisplayName("날짜를 과거로 바꾸면 CONCERT_002이고 공연은 그대로다")
  void changedToPastRejected() {
    Concert concert = existingConcert(FUTURE);

    assertError(
        () -> service.update(CONCERT_ID, USER_ID, updateRequest(PAST)),
        ConcertErrorCode.PERFORM_DATE_BEFORE_TODAY);
    assertThat(concert.getTitle()).isEqualTo("겨울 합동공연");
    assertThat(concert.getPerformDate()).isEqualTo(FUTURE);
  }

  @Test
  @DisplayName("미정이던 날짜를 과거로 정해도 바꾼 것이라 CONCERT_002")
  void undecidedToPastRejected() {
    existingConcert(null);

    assertError(
        () -> service.update(CONCERT_ID, USER_ID, updateRequest(PAST)),
        ConcertErrorCode.PERFORM_DATE_BEFORE_TODAY);
  }

  @Test
  @DisplayName("날짜를 미정으로 바꾸면 공고 마감일을 보지 않고 통과한다")
  void changeToUndecidedSkipsDeadlineCheck() {
    Concert concert = existingConcert(PAST);

    service.update(CONCERT_ID, USER_ID, updateRequest(null, null, null, 3));

    assertThat(concert.getPerformDate()).isNull();
    verifyNoInteractions(recruitPostRepository);
  }

  @Test
  @DisplayName("공연 날짜를 진행 중인 공고 마감일보다 앞으로 바꾸면 CONCERT_011")
  void performDateBeforeDeadlineRejected() {
    Concert concert = existingConcert(FUTURE);
    givenRecruitPost(concert, DEADLINE);

    assertError(
        () -> service.update(CONCERT_ID, USER_ID, updateRequest(DEADLINE.minusDays(1))),
        ConcertErrorCode.PERFORM_DATE_BEFORE_RECRUIT_DEADLINE);
  }

  @Test
  @DisplayName("공연 날짜가 공고 마감일과 같은 날이면 통과한다")
  void performDateOnDeadlineAllowed() {
    Concert concert = existingConcert(FUTURE);
    givenRecruitPost(concert, DEADLINE);

    service.update(CONCERT_ID, USER_ID, updateRequest(DEADLINE));

    assertThat(concert.getPerformDate()).isEqualTo(DEADLINE);
  }

  @Test
  @DisplayName("공고가 이미 마감됐으면 마감일보다 앞으로 옮겨도 통과한다")
  void closedRecruitPostSkipped() {
    Concert concert = existingConcert(FUTURE);
    RecruitPost post = givenRecruitPost(concert, DEADLINE);
    ReflectionTestUtils.setField(post, "status", RecruitPostStatus.CLOSED);

    service.update(CONCERT_ID, USER_ID, updateRequest(DEADLINE.minusDays(1)));

    assertThat(concert.getPerformDate()).isEqualTo(DEADLINE.minusDays(1));
  }

  @Test
  @DisplayName("공고가 없으면(삭제한 공고 포함) 마감일을 보지 않고 통과한다")
  void noRecruitPostSkipped() {
    Concert concert = existingConcert(FUTURE);

    service.update(CONCERT_ID, USER_ID, updateRequest(DEADLINE.minusDays(1)));

    verify(recruitPostRepository).findByConcertIdAndDeletedAtIsNull(CONCERT_ID);
    assertThat(concert.getPerformDate()).isEqualTo(DEADLINE.minusDays(1));
  }

  @Test
  @DisplayName("모집 팀 수를 주최 팀을 뺀 합류 팀 수보다 적게 줄이면 CONCERT_003")
  void recruitTeamCountBelowJoinedRejected() {
    Concert concert = existingConcert(FUTURE);
    givenJoinedGuestTeams(3);

    assertError(
        () -> service.update(CONCERT_ID, USER_ID, updateRequest(FUTURE, START, END, 2)),
        ConcertErrorCode.RECRUIT_TEAM_COUNT_BELOW_JOINED);
    assertThat(concert.getRecruitTeamCount()).isEqualTo(3);
  }

  @Test
  @DisplayName("모집 팀 수가 합류 팀 수와 같으면 통과한다")
  void recruitTeamCountEqualToJoinedAllowed() {
    Concert concert = existingConcert(FUTURE);
    givenJoinedGuestTeams(3);

    service.update(CONCERT_ID, USER_ID, updateRequest(FUTURE, START, END, 3));

    assertThat(concert.getTitle()).isEqualTo("봄 합동공연");
  }
}
