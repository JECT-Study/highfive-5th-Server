package com.ject.highfive.concert.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.ject.highfive.concert.dto.ConcertCreateRequest;
import com.ject.highfive.concert.dto.ConcertCreateResponse;
import com.ject.highfive.concert.entity.Concert;
import com.ject.highfive.concert.entity.ConcertStatus;
import com.ject.highfive.concert.entity.ConcertTeam;
import com.ject.highfive.concert.entity.ConcertTeamStatus;
import com.ject.highfive.concert.entity.Genre;
import com.ject.highfive.concert.exception.ConcertErrorCode;
import com.ject.highfive.concert.repository.ConcertRepository;
import com.ject.highfive.concert.repository.ConcertTeamRepository;
import com.ject.highfive.global.exception.BusinessException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;
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

  @Mock ConcertRepository concertRepository;
  @Mock ConcertTeamRepository concertTeamRepository;
  @InjectMocks ConcertService service;

  private ConcertCreateRequest request(LocalDate performDate, LocalTime start, LocalTime end) {
    return new ConcertCreateRequest(
        BAND_ID,
        "겨울 합동공연",
        Set.of(Genre.ROCK, Genre.INDIE),
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
  @DisplayName("선택 값이 모두 미정(null)이어도 만들 수 있다")
  void undecidedOptionalValuesAllowed() {
    givenSaveAssignsId(12L);
    ConcertCreateRequest request =
        new ConcertCreateRequest(
            BAND_ID,
            "겨울 합동공연",
            Set.of(Genre.ROCK),
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
}
