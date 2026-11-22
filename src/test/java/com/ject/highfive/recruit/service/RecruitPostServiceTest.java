package com.ject.highfive.recruit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.ject.highfive.concert.entity.Concert;
import com.ject.highfive.concert.entity.Genre;
import com.ject.highfive.concert.exception.ConcertErrorCode;
import com.ject.highfive.concert.repository.ConcertRepository;
import com.ject.highfive.global.exception.BusinessException;
import com.ject.highfive.global.exception.ErrorCode;
import com.ject.highfive.recruit.dto.RecruitPostCreateResponse;
import com.ject.highfive.recruit.dto.RecruitPostRequest;
import com.ject.highfive.recruit.entity.RecruitPost;
import com.ject.highfive.recruit.entity.RecruitPostStatus;
import com.ject.highfive.recruit.entity.TeamType;
import com.ject.highfive.recruit.entity.WantedCareerLevel;
import com.ject.highfive.recruit.exception.RecruitErrorCode;
import com.ject.highfive.recruit.repository.RecruitPostRepository;
import java.time.LocalDate;
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
class RecruitPostServiceTest {

  private static final LocalDate PERFORM_DATE = LocalDate.of(2999, 12, 20);
  private static final LocalDate DEADLINE = LocalDate.of(2999, 12, 1);
  private static final LocalDate PAST = LocalDate.of(2000, 1, 1);
  private static final Long CONCERT_ID = 12L;
  private static final Long USER_ID = 10L;
  private static final Long POST_ID = 5L;
  private static final LocalDate OPEN_DEADLINE = LocalDate.of(2999, 11, 1);

  @Mock RecruitPostRepository recruitPostRepository;
  @Mock ConcertRepository concertRepository;
  @InjectMocks RecruitPostService service;

  private Concert concert(LocalDate performDate) {
    return Concert.builder()
        .hostBandId(3L)
        .createdBy(1L)
        .title("겨울 합동공연")
        .recruitTeamCount(3)
        .performDate(performDate)
        .genres(List.of(Genre.ROCK))
        .build();
  }

  private RecruitPostRequest request(LocalDate deadline) {
    return new RecruitPostRequest(
        WantedCareerLevel.Y1_TO_3,
        List.of(TeamType.FIRST_PERFORMANCE, TeamType.ORIGINAL_SONGS),
        deadline,
        "12월 홍대에서 함께할 팀을 찾습니다.");
  }

  private void givenConcert(Concert concert) {
    given(concertRepository.findByIdAndDeletedAtIsNull(CONCERT_ID))
        .willReturn(Optional.of(concert));
  }

  private void givenSaveAssignsId(Long id) {
    given(recruitPostRepository.existsByConcertIdAndDeletedAtIsNull(CONCERT_ID)).willReturn(false);
    given(recruitPostRepository.save(any(RecruitPost.class)))
        .willAnswer(
            invocation -> {
              RecruitPost post = invocation.getArgument(0);
              ReflectionTestUtils.setField(post, "id", id);
              return post;
            });
  }

  private void assertError(Runnable call, ErrorCode expected) {
    assertThatThrownBy(call::run)
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(expected);
  }

  private RecruitPost givenPost(LocalDate performDate, LocalDate deadline) {
    RecruitPost post =
        RecruitPost.builder()
            .concert(concert(performDate))
            .authorUserId(USER_ID)
            .wantedCareerLevel(WantedCareerLevel.ANY)
            .teamTypes(List.of(TeamType.ANYONE))
            .deadline(deadline)
            .description("기존 소개")
            .build();
    given(recruitPostRepository.findByIdAndDeletedAtIsNull(POST_ID)).willReturn(Optional.of(post));
    return post;
  }

  @Test
  @DisplayName("작성하면 모집 중 공고가 저장되고 ID를 돌려준다")
  void createSavesPost() {
    Concert concert = concert(PERFORM_DATE);
    givenConcert(concert);
    givenSaveAssignsId(5L);

    RecruitPostCreateResponse response = service.create(CONCERT_ID, USER_ID, request(DEADLINE));

    assertThat(response.recruitPostId()).isEqualTo(5L);
    ArgumentCaptor<RecruitPost> captor = ArgumentCaptor.forClass(RecruitPost.class);
    verify(recruitPostRepository).save(captor.capture());
    RecruitPost saved = captor.getValue();
    assertThat(saved.getConcert()).isSameAs(concert);
    assertThat(saved.getAuthorUserId()).isEqualTo(USER_ID);
    assertThat(saved.getStatus()).isEqualTo(RecruitPostStatus.RECRUITING);
    assertThat(saved.getWantedCareerLevel()).isEqualTo(WantedCareerLevel.Y1_TO_3);
    assertThat(saved.getTeamTypes())
        .containsExactly(TeamType.FIRST_PERFORMANCE, TeamType.ORIGINAL_SONGS);
    assertThat(saved.getDeadline()).isEqualTo(DEADLINE);
    assertThat(saved.getDescription()).isEqualTo("12월 홍대에서 함께할 팀을 찾습니다.");
  }

  @Test
  @DisplayName("중복된 팀 유형은 하나로 합쳐 선언 순서대로 저장한다")
  void duplicateTeamTypesAreDeduplicated() {
    givenConcert(concert(PERFORM_DATE));
    givenSaveAssignsId(5L);
    RecruitPostRequest request =
        new RecruitPostRequest(
            WantedCareerLevel.Y1_TO_3,
            List.of(TeamType.ANYONE, TeamType.ANYONE, TeamType.HOBBY),
            DEADLINE,
            "12월 홍대에서 함께할 팀을 찾습니다.");

    service.create(CONCERT_ID, USER_ID, request);

    ArgumentCaptor<RecruitPost> captor = ArgumentCaptor.forClass(RecruitPost.class);
    verify(recruitPostRepository).save(captor.capture());
    assertThat(captor.getValue().getTeamTypes()).containsExactly(TeamType.HOBBY, TeamType.ANYONE);
  }

  @Test
  @DisplayName("마감일이 오늘 이전이면 DB를 보기 전에 RECRUIT_003")
  void deadlineBeforeTodayRejected() {
    assertError(
        () -> service.create(CONCERT_ID, USER_ID, request(PAST)),
        RecruitErrorCode.DEADLINE_BEFORE_TODAY);
    verifyNoInteractions(concertRepository, recruitPostRepository);
  }

  @Test
  @DisplayName("마감일이 오늘이면 작성할 수 있다")
  void deadlineTodayAllowed() {
    givenConcert(concert(PERFORM_DATE));
    givenSaveAssignsId(5L);

    assertThat(service.create(CONCERT_ID, USER_ID, request(LocalDate.now())).recruitPostId())
        .isEqualTo(5L);
  }

  @Test
  @DisplayName("공연이 없거나 삭제됐으면 CONCERT_001이고 저장하지 않는다")
  void concertNotFound() {
    given(concertRepository.findByIdAndDeletedAtIsNull(CONCERT_ID)).willReturn(Optional.empty());

    assertError(
        () -> service.create(CONCERT_ID, USER_ID, request(DEADLINE)),
        ConcertErrorCode.CONCERT_NOT_FOUND);
    verifyNoInteractions(recruitPostRepository);
  }

  @Test
  @DisplayName("마감일이 공연 날짜보다 늦으면 RECRUIT_005")
  void deadlineAfterPerformDateRejected() {
    givenConcert(concert(PERFORM_DATE));

    assertError(
        () -> service.create(CONCERT_ID, USER_ID, request(PERFORM_DATE.plusDays(1))),
        RecruitErrorCode.DEADLINE_AFTER_PERFORM_DATE);
    verifyNoInteractions(recruitPostRepository);
  }

  @Test
  @DisplayName("마감일이 공연 날짜와 같은 날이면 작성할 수 있다")
  void deadlineOnPerformDateAllowed() {
    givenConcert(concert(PERFORM_DATE));
    givenSaveAssignsId(5L);

    assertThat(service.create(CONCERT_ID, USER_ID, request(PERFORM_DATE)).recruitPostId())
        .isEqualTo(5L);
  }

  @Test
  @DisplayName("공연 날짜가 미정이면 공연 날짜와 비교하지 않는다")
  void undecidedPerformDateSkipsCheck() {
    givenConcert(concert(null));
    givenSaveAssignsId(5L);

    assertThat(
            service
                .create(CONCERT_ID, USER_ID, request(LocalDate.of(2999, 12, 31)))
                .recruitPostId())
        .isEqualTo(5L);
  }

  @Test
  @DisplayName("이미 지난 공연이면 RECRUIT_006이고 저장하지 않는다")
  void pastConcertRejected() {
    givenConcert(concert(LocalDate.now().minusDays(1)));

    assertError(
        () -> service.create(CONCERT_ID, USER_ID, request(LocalDate.now())),
        RecruitErrorCode.CONCERT_ALREADY_PERFORMED);
    verifyNoInteractions(recruitPostRepository);
  }

  @Test
  @DisplayName("공연 당일이고 마감일도 오늘이면 작성할 수 있다")
  void concertTodayAllowed() {
    givenConcert(concert(LocalDate.now()));
    givenSaveAssignsId(5L);

    assertThat(service.create(CONCERT_ID, USER_ID, request(LocalDate.now())).recruitPostId())
        .isEqualTo(5L);
  }

  @Test
  @DisplayName("지난 공연에 공연 날짜보다 늦은 마감일을 보내도 RECRUIT_006이 먼저다")
  void pastConcertTakesPrecedenceOverDeadlineAfterPerformDate() {
    givenConcert(concert(LocalDate.now().minusDays(1)));

    assertError(
        () -> service.create(CONCERT_ID, USER_ID, request(LocalDate.now().plusDays(5))),
        RecruitErrorCode.CONCERT_ALREADY_PERFORMED);
    verifyNoInteractions(recruitPostRepository);
  }

  @Test
  @DisplayName("삭제하지 않은 공고가 이미 있는 공연이면 RECRUIT_002")
  void alreadyExists() {
    givenConcert(concert(PERFORM_DATE));
    given(recruitPostRepository.existsByConcertIdAndDeletedAtIsNull(CONCERT_ID)).willReturn(true);

    assertError(
        () -> service.create(CONCERT_ID, USER_ID, request(DEADLINE)),
        RecruitErrorCode.RECRUIT_POST_ALREADY_EXISTS);
    verify(recruitPostRepository, never()).save(any());
  }

  @Test
  @DisplayName("수정하면 요청 값으로 공고를 통째로 바꾼다")
  void updateReplacesPost() {
    RecruitPost post = givenPost(PERFORM_DATE, OPEN_DEADLINE);

    service.update(POST_ID, USER_ID, request(DEADLINE));

    assertThat(post.getWantedCareerLevel()).isEqualTo(WantedCareerLevel.Y1_TO_3);
    assertThat(post.getTeamTypes())
        .containsExactly(TeamType.FIRST_PERFORMANCE, TeamType.ORIGINAL_SONGS);
    assertThat(post.getDeadline()).isEqualTo(DEADLINE);
    assertThat(post.getDescription()).isEqualTo("12월 홍대에서 함께할 팀을 찾습니다.");
  }

  @Test
  @DisplayName("수정 마감일이 오늘 이전이면 공고를 찾기 전에 RECRUIT_003")
  void updateDeadlineBeforeTodayRejected() {
    assertError(
        () -> service.update(POST_ID, USER_ID, request(PAST)),
        RecruitErrorCode.DEADLINE_BEFORE_TODAY);
    verifyNoInteractions(recruitPostRepository);
  }

  @Test
  @DisplayName("수정 마감일을 오늘로 바꿀 수 있다")
  void updateDeadlineTodayAllowed() {
    RecruitPost post = givenPost(PERFORM_DATE, OPEN_DEADLINE);

    service.update(POST_ID, USER_ID, request(LocalDate.now()));

    assertThat(post.getDeadline()).isEqualTo(LocalDate.now());
  }

  @Test
  @DisplayName("공고가 없거나 삭제됐으면 수정은 RECRUIT_001")
  void updateMissingPostRejected() {
    assertError(
        () -> service.update(POST_ID, USER_ID, request(DEADLINE)),
        RecruitErrorCode.RECRUIT_POST_NOT_FOUND);
  }

  @Test
  @DisplayName("지난 공연의 공고는 이미 마감이라 수정하면 RECRUIT_004이고 공고는 그대로다")
  void updatePastConcertPostRejectedAsClosed() {
    RecruitPost post = givenPost(PAST, PAST);

    assertError(
        () -> service.update(POST_ID, USER_ID, request(DEADLINE)),
        RecruitErrorCode.RECRUIT_POST_ALREADY_CLOSED);
    assertThat(post.getDeadline()).isEqualTo(PAST);
    assertThat(post.getDescription()).isEqualTo("기존 소개");
  }

  @Test
  @DisplayName("공연이 오늘이면 공고를 수정할 수 있다")
  void updateConcertTodayAllowed() {
    RecruitPost post = givenPost(LocalDate.now(), OPEN_DEADLINE);

    service.update(POST_ID, USER_ID, request(LocalDate.now()));

    assertThat(post.getDeadline()).isEqualTo(LocalDate.now());
  }

  @Test
  @DisplayName("수정 마감일이 공연 날짜보다 늦으면 RECRUIT_005")
  void updateDeadlineAfterPerformDateRejected() {
    givenPost(PERFORM_DATE, OPEN_DEADLINE);

    assertError(
        () -> service.update(POST_ID, USER_ID, request(PERFORM_DATE.plusDays(1))),
        RecruitErrorCode.DEADLINE_AFTER_PERFORM_DATE);
  }

  @Test
  @DisplayName("공연 날짜가 미정이면 수정 마감일을 공연 날짜와 비교하지 않는다")
  void updateUndecidedPerformDateSkipsCheck() {
    RecruitPost post = givenPost(null, OPEN_DEADLINE);

    service.update(POST_ID, USER_ID, request(LocalDate.of(2999, 12, 31)));

    assertThat(post.getDeadline()).isEqualTo(LocalDate.of(2999, 12, 31));
  }

  @Test
  @DisplayName("직접 마감한 공고는 수정할 수 없다 (RECRUIT_004)")
  void updateDirectlyClosedRejected() {
    RecruitPost post = givenPost(PERFORM_DATE, OPEN_DEADLINE);
    post.close();

    assertError(
        () -> service.update(POST_ID, USER_ID, request(DEADLINE)),
        RecruitErrorCode.RECRUIT_POST_ALREADY_CLOSED);
    assertThat(post.getDeadline()).isEqualTo(OPEN_DEADLINE);
  }

  @Test
  @DisplayName("마감된 공고는 마감일이 공연 날짜보다 늦어도 RECRUIT_005가 아니라 RECRUIT_004")
  void updateClosedCheckedBeforePerformDate() {
    RecruitPost post = givenPost(PERFORM_DATE, OPEN_DEADLINE);
    post.close();

    assertError(
        () -> service.update(POST_ID, USER_ID, request(PERFORM_DATE.plusDays(1))),
        RecruitErrorCode.RECRUIT_POST_ALREADY_CLOSED);
  }

  @Test
  @DisplayName("마감일이 지난 공고의 마감일을 미래로 바꾸는 수정은 다시 열기라 RECRUIT_004")
  void updateDeadlinePassedRejected() {
    RecruitPost post = givenPost(PERFORM_DATE, PAST);

    assertError(
        () -> service.update(POST_ID, USER_ID, request(DEADLINE)),
        RecruitErrorCode.RECRUIT_POST_ALREADY_CLOSED);
    assertThat(post.getDeadline()).isEqualTo(PAST);
    assertThat(post.getDescription()).isEqualTo("기존 소개");
  }

  @Test
  @DisplayName("마감하면 공고가 CLOSED가 된다")
  void closeClosesPost() {
    RecruitPost post = givenPost(PERFORM_DATE, OPEN_DEADLINE);

    service.close(POST_ID, USER_ID);

    assertThat(post.getStatus()).isEqualTo(RecruitPostStatus.CLOSED);
  }

  @Test
  @DisplayName("이미 직접 마감한 공고를 다시 마감하면 RECRUIT_004")
  void closeAlreadyClosedRejected() {
    RecruitPost post = givenPost(PERFORM_DATE, OPEN_DEADLINE);
    post.close();

    assertError(
        () -> service.close(POST_ID, USER_ID), RecruitErrorCode.RECRUIT_POST_ALREADY_CLOSED);
  }

  @Test
  @DisplayName("마감일이 지난 공고를 마감하면 RECRUIT_004이고 status는 그대로다")
  void closeDeadlinePassedRejected() {
    RecruitPost post = givenPost(PERFORM_DATE, PAST);

    assertError(
        () -> service.close(POST_ID, USER_ID), RecruitErrorCode.RECRUIT_POST_ALREADY_CLOSED);
    assertThat(post.getStatus()).isEqualTo(RecruitPostStatus.RECRUITING);
  }

  @Test
  @DisplayName("공고가 없거나 삭제됐으면 마감은 RECRUIT_001")
  void closeMissingPostRejected() {
    assertError(() -> service.close(POST_ID, USER_ID), RecruitErrorCode.RECRUIT_POST_NOT_FOUND);
  }

  @Test
  @DisplayName("삭제하면 삭제 시각이 기록된다")
  void deleteMarksPostDeleted() {
    RecruitPost post = givenPost(PERFORM_DATE, OPEN_DEADLINE);

    service.delete(POST_ID, USER_ID);

    assertThat(post.getDeletedAt()).isNotNull();
  }

  @Test
  @DisplayName("마감된 공고도 삭제할 수 있다")
  void deleteClosedPostAllowed() {
    RecruitPost post = givenPost(PERFORM_DATE, PAST);
    post.close();

    service.delete(POST_ID, USER_ID);

    assertThat(post.getDeletedAt()).isNotNull();
  }

  @Test
  @DisplayName("공고가 없거나 이미 삭제됐으면 삭제는 RECRUIT_001")
  void deleteMissingPostRejected() {
    assertError(() -> service.delete(POST_ID, USER_ID), RecruitErrorCode.RECRUIT_POST_NOT_FOUND);
  }
}
