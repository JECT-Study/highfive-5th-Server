package com.ject.highfive.concert.service;

import com.ject.highfive.concert.dto.ConcertCreateRequest;
import com.ject.highfive.concert.dto.ConcertCreateResponse;
import com.ject.highfive.concert.dto.ConcertUpdateRequest;
import com.ject.highfive.concert.entity.Concert;
import com.ject.highfive.concert.entity.ConcertTeam;
import com.ject.highfive.concert.entity.ConcertTeamStatus;
import com.ject.highfive.concert.exception.ConcertErrorCode;
import com.ject.highfive.concert.repository.ConcertRepository;
import com.ject.highfive.concert.repository.ConcertTeamRepository;
import com.ject.highfive.global.exception.BusinessException;
import com.ject.highfive.recruit.repository.RecruitPostRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 생성 검사 순서: 시작·종료 시간 짝(400) → 공연 날짜(400) → 권한(나중에 403)
// 수정 검사 순서: 시간 짝(400) → 공연 존재(404) → 참여 팀 멤버(403) → 버전(409) → 바꾼 날짜가 과거(400)
//   → 공고 마감일보다 이른 날짜(400) → 합류 팀 수(409)
@Service
@Transactional
@RequiredArgsConstructor
public class ConcertService {

  private static final int HOST_LINEUP_ORDER = 1;

  private final ConcertRepository concertRepository;
  private final ConcertTeamRepository concertTeamRepository;
  private final RecruitPostRepository recruitPostRepository;
  private final ConcertAccessChecker concertAccessChecker;

  public ConcertCreateResponse create(Long userId, ConcertCreateRequest request) {
    validatePerformTime(request.startTime(), request.endTime());
    validatePerformDateNotPast(request.performDate());
    // TODO: JECT-248 #58 머지 후 요청한 bandId의 대표자인지, 밴드가 있는지 확인한다 (403 AUTH_005)

    Concert concert = concertRepository.save(request.toEntity(userId));
    // 주최 밴드도 참여 팀이다. 공연만 생기고 주최 팀이 빠지지 않도록 같은 트랜잭션에서 저장한다
    concertTeamRepository.save(
        ConcertTeam.builder()
            .concert(concert)
            .bandId(request.bandId())
            .lineupOrder(HOST_LINEUP_ORDER)
            .build());
    return ConcertCreateResponse.from(concert);
  }

  public void update(Long concertId, Long userId, ConcertUpdateRequest request) {
    validatePerformTime(request.startTime(), request.endTime());
    Concert concert =
        concertRepository
            .findByIdAndDeletedAtIsNull(concertId)
            .orElseThrow(() -> new BusinessException(ConcertErrorCode.CONCERT_NOT_FOUND));
    concertAccessChecker.checkParticipant(concertId, userId);
    // 수정 화면을 연 뒤 다른 멤버가 먼저 고쳤으면 옛 화면 값으로 덮어쓰지 않는다 (의사결정 D12)
    if (!request.version().equals(concert.getVersion())) {
      throw new BusinessException(ConcertErrorCode.CONCERT_MODIFIED_BY_OTHER);
    }
    // PUT은 기존 날짜를 다시 보낸다. 바꿀 때만 막아야 지난 공연도 제목 오타 등을 고칠 수 있다
    if (!Objects.equals(request.performDate(), concert.getPerformDate())) {
      validatePerformDateNotPast(request.performDate());
    }
    validatePerformDateNotBeforeRecruitDeadline(concertId, request.performDate());
    validateRecruitTeamCountNotBelowJoined(concert, request.recruitTeamCount());

    request.applyTo(concert, userId);
  }

  // 둘 다 있거나 둘 다 미정. 종료가 시작보다 이른지는 보지 않는다 (자정을 넘기는 공연)
  private void validatePerformTime(LocalTime startTime, LocalTime endTime) {
    if ((startTime == null) != (endTime == null)) {
      throw new BusinessException(ConcertErrorCode.PERFORM_TIME_INCOMPLETE);
    }
  }

  private void validatePerformDateNotPast(LocalDate performDate) {
    if (performDate != null && performDate.isBefore(LocalDate.now())) {
      throw new BusinessException(ConcertErrorCode.PERFORM_DATE_BEFORE_TODAY);
    }
  }

  // 날짜가 미정이거나 공고가 없거나(삭제 포함) 이미 마감됐으면 보지 않는다.
  // 마감된 공고의 마감일 때문에 공연 날짜를 못 옮기면 안 된다. 같은 날은 허용
  private void validatePerformDateNotBeforeRecruitDeadline(Long concertId, LocalDate performDate) {
    if (performDate == null) {
      return;
    }
    boolean beforeDeadline =
        recruitPostRepository
            .findByConcertIdAndDeletedAtIsNull(concertId)
            .filter(post -> !post.isClosed(LocalDate.now()))
            .filter(post -> post.getDeadline().isAfter(performDate))
            .isPresent();
    if (beforeDeadline) {
      throw new BusinessException(ConcertErrorCode.PERFORM_DATE_BEFORE_RECRUIT_DEADLINE);
    }
  }

  // recruitTeamCount는 주최 팀을 빼고 세므로 합류 수에서도 주최 팀을 뺀다. 같으면 허용
  private void validateRecruitTeamCountNotBelowJoined(Concert concert, int recruitTeamCount) {
    long joinedGuestTeams =
        concertTeamRepository.countByConcertIdAndStatusAndBandIdNot(
            concert.getId(), ConcertTeamStatus.JOINED, concert.getHostBandId());
    if (recruitTeamCount < joinedGuestTeams) {
      throw new BusinessException(ConcertErrorCode.RECRUIT_TEAM_COUNT_BELOW_JOINED);
    }
  }
}
