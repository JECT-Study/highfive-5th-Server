package com.ject.highfive.recruit.service;

import com.ject.highfive.concert.entity.Concert;
import com.ject.highfive.concert.exception.ConcertErrorCode;
import com.ject.highfive.concert.repository.ConcertRepository;
import com.ject.highfive.global.exception.BusinessException;
import com.ject.highfive.recruit.dto.RecruitPostCreateResponse;
import com.ject.highfive.recruit.dto.RecruitPostRequest;
import com.ject.highfive.recruit.entity.RecruitPost;
import com.ject.highfive.recruit.exception.RecruitErrorCode;
import com.ject.highfive.recruit.repository.RecruitPostRepository;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 검사 순서: 마감일이 오늘 이전(400) → 공연 존재(404) → 권한(나중에 403) → 공연 날짜(400) → 중복(409)
@Service
@Transactional
@RequiredArgsConstructor
public class RecruitPostService {

  private final RecruitPostRepository recruitPostRepository;
  private final ConcertRepository concertRepository;

  public RecruitPostCreateResponse create(Long concertId, Long userId, RecruitPostRequest request) {
    validateDeadlineNotPast(request.deadline());
    Concert concert =
        concertRepository
            .findById(concertId)
            .orElseThrow(() -> new BusinessException(ConcertErrorCode.CONCERT_NOT_FOUND));
    // TODO: JECT-248 #58 머지 후 concert.getHostBandId()의 현재 대표자인지 확인한다 (403 AUTH_005)
    validateDeadlineNotAfterPerformDate(request.deadline(), concert);
    if (recruitPostRepository.existsByConcertId(concertId)) {
      throw new BusinessException(RecruitErrorCode.RECRUIT_POST_ALREADY_EXISTS);
    }

    RecruitPost recruitPost =
        RecruitPost.builder()
            .concert(concert)
            .authorUserId(userId)
            .wantedCareerLevel(request.wantedCareerLevel())
            .teamTypes(request.teamTypes())
            .deadline(request.deadline())
            .description(request.description())
            .build();
    try {
      return RecruitPostCreateResponse.from(recruitPostRepository.saveAndFlush(recruitPost));
    } catch (DataIntegrityViolationException e) {
      // 같은 공연에 동시에 작성한 경우 UNIQUE(concert_id)에 걸린다
      throw new BusinessException(RecruitErrorCode.RECRUIT_POST_ALREADY_EXISTS);
    }
  }

  private void validateDeadlineNotPast(LocalDate deadline) {
    if (deadline.isBefore(LocalDate.now())) {
      throw new BusinessException(RecruitErrorCode.DEADLINE_BEFORE_TODAY);
    }
  }

  // 공연 날짜가 미정이면 비교하지 않는다. 같은 날은 허용
  private void validateDeadlineNotAfterPerformDate(LocalDate deadline, Concert concert) {
    LocalDate performDate = concert.getPerformDate();
    if (performDate != null && deadline.isAfter(performDate)) {
      throw new BusinessException(RecruitErrorCode.DEADLINE_AFTER_PERFORM_DATE);
    }
  }
}
