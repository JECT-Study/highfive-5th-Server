package com.ject.highfive.concert.service;

import com.ject.highfive.concert.dto.ConcertCreateRequest;
import com.ject.highfive.concert.dto.ConcertCreateResponse;
import com.ject.highfive.concert.entity.Concert;
import com.ject.highfive.concert.entity.ConcertTeam;
import com.ject.highfive.concert.exception.ConcertErrorCode;
import com.ject.highfive.concert.repository.ConcertRepository;
import com.ject.highfive.concert.repository.ConcertTeamRepository;
import com.ject.highfive.global.exception.BusinessException;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 검사 순서: 시작·종료 시간 짝(400) → 공연 날짜(400) → 권한(나중에 403)
@Service
@Transactional
@RequiredArgsConstructor
public class ConcertService {

  private static final int HOST_LINEUP_ORDER = 1;

  private final ConcertRepository concertRepository;
  private final ConcertTeamRepository concertTeamRepository;

  public ConcertCreateResponse create(Long userId, ConcertCreateRequest request) {
    validatePerformTime(request.startTime(), request.endTime());
    validatePerformDateNotPast(request.performDate());
    // TODO: JECT-248 #58 머지 후 요청한 bandId의 대표자인지, 밴드가 있는지 확인한다 (403 AUTH_005)

    Concert concert =
        concertRepository.save(
            Concert.builder()
                .hostBandId(request.bandId())
                .createdBy(userId)
                .title(request.title())
                .recruitTeamCount(request.recruitTeamCount())
                .performDate(request.performDate())
                .startTime(request.startTime())
                .endTime(request.endTime())
                .place(request.place())
                .address(request.address())
                .region(request.region())
                .rentalFee(request.rentalFee())
                .posterImageUrl(request.posterImageUrl())
                .genres(request.genres())
                .build());
    // 주최 밴드도 참여 팀이다. 공연만 생기고 주최 팀이 빠지지 않도록 같은 트랜잭션에서 저장한다
    concertTeamRepository.save(
        ConcertTeam.builder()
            .concert(concert)
            .bandId(request.bandId())
            .lineupOrder(HOST_LINEUP_ORDER)
            .build());
    return ConcertCreateResponse.from(concert);
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
}
