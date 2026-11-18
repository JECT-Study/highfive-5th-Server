package com.ject.highfive.concert.exception;

import com.ject.highfive.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ConcertErrorCode implements ErrorCode {
  CONCERT_NOT_FOUND(HttpStatus.NOT_FOUND, "CONCERT_001", "공연을 찾을 수 없습니다."),
  PERFORM_DATE_BEFORE_TODAY(HttpStatus.BAD_REQUEST, "CONCERT_002", "공연 날짜는 오늘 이후여야 합니다."),
  RECRUIT_TEAM_COUNT_BELOW_JOINED(
      HttpStatus.CONFLICT, "CONCERT_003", "모집 팀 수는 이미 합류한 팀 수보다 적을 수 없습니다."),
  // CONCERT_004~009는 API 명세 부록 B에서 다른 API용으로 정해 둔 번호다
  PERFORM_TIME_INCOMPLETE(HttpStatus.BAD_REQUEST, "CONCERT_010", "공연 시작 시간과 종료 시간은 함께 입력해야 합니다."),
  PERFORM_DATE_BEFORE_RECRUIT_DEADLINE(
      HttpStatus.BAD_REQUEST, "CONCERT_011", "공연 날짜는 모집 공고 마감일보다 빠를 수 없습니다."),
  CONCERT_MODIFIED_BY_OTHER(
      HttpStatus.CONFLICT, "CONCERT_012", "다른 멤버가 먼저 공연 정보를 수정했습니다. 다시 불러온 뒤 수정해 주세요.");

  private final HttpStatus status;
  private final String code;
  private final String message;
}
