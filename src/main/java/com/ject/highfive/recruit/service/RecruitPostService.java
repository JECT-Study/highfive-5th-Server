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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 작성 검사 순서: 마감일이 오늘 이전(400) → 공연 존재(404) → 권한(나중에 403) → 지난 공연(400)
//   → 공연 날짜보다 늦은 마감일(400) → 중복(409)
// 수정 검사 순서: 마감일이 오늘 이전(400) → 공고 존재(404) → 권한(나중에 403) → 지난 공연(400)
//   → 공연 날짜보다 늦은 마감일(400) → 이미 마감(409)
// 권한은 공고 → 공연 → hostBandId를 읽어야 알 수 있어 존재 확인 뒤에 둔다
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
    validateConcertNotPerformed(concert);
    validateDeadlineNotAfterPerformDate(request.deadline(), concert);
    if (recruitPostRepository.existsByConcertId(concertId)) {
      throw new BusinessException(RecruitErrorCode.RECRUIT_POST_ALREADY_EXISTS);
    }

    RecruitPost recruitPost = request.toEntity(concert, userId);
    return RecruitPostCreateResponse.from(recruitPostRepository.save(recruitPost));
  }

  public void update(Long recruitPostId, Long userId, RecruitPostRequest request) {
    validateDeadlineNotPast(request.deadline());
    RecruitPost post = findPost(recruitPostId);
    // TODO: JECT-248 #58 머지 후 post.getConcert().getHostBandId()의 현재 대표자인지 확인한다 (403 AUTH_005)
    validateConcertNotPerformed(post.getConcert());
    validateDeadlineNotAfterPerformDate(request.deadline(), post.getConcert());
    // 기한이 지난 공고의 마감일을 미래로 바꾸면 다시 열기가 되는데, 다시 여는 기능은 없다
    validateNotClosed(post);

    request.applyTo(post);
  }

  public void close(Long recruitPostId, Long userId) {
    RecruitPost post = findPost(recruitPostId);
    // TODO: JECT-248 #58 머지 후 post.getConcert().getHostBandId()의 현재 대표자인지 확인한다 (403 AUTH_005)
    validateNotClosed(post);

    post.close();
  }

  // 마감된 공고도 지울 수 있다
  public void delete(Long recruitPostId, Long userId) {
    RecruitPost post = findPost(recruitPostId);
    // TODO: JECT-248 #58 머지 후 post.getConcert().getHostBandId()의 현재 대표자인지 확인한다 (403 AUTH_005)

    post.delete();
  }

  private RecruitPost findPost(Long recruitPostId) {
    return recruitPostRepository
        .findByIdAndDeletedAtIsNull(recruitPostId)
        .orElseThrow(() -> new BusinessException(RecruitErrorCode.RECRUIT_POST_NOT_FOUND));
  }

  // 직접 마감과 기한 마감을 모두 본다 (의사결정 D10)
  private void validateNotClosed(RecruitPost post) {
    if (post.isClosed(LocalDate.now())) {
      throw new BusinessException(RecruitErrorCode.RECRUIT_POST_ALREADY_CLOSED);
    }
  }

  private void validateDeadlineNotPast(LocalDate deadline) {
    if (deadline.isBefore(LocalDate.now())) {
      throw new BusinessException(RecruitErrorCode.DEADLINE_BEFORE_TODAY);
    }
  }

  // 공연 날짜가 미정이면 건너뛴다. 공연 당일은 허용
  private void validateConcertNotPerformed(Concert concert) {
    LocalDate performDate = concert.getPerformDate();
    if (performDate != null && performDate.isBefore(LocalDate.now())) {
      throw new BusinessException(RecruitErrorCode.CONCERT_ALREADY_PERFORMED);
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
