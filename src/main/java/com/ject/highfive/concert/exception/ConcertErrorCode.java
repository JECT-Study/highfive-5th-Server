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
  // CONCERT_003~009는 API 명세 부록 B에서 다른 API용으로 정해 둔 번호다
  PERFORM_TIME_INCOMPLETE(HttpStatus.BAD_REQUEST, "CONCERT_010", "공연 시작 시간과 종료 시간은 함께 입력해야 합니다.");

  private final HttpStatus status;
  private final String code;
  private final String message;
}
