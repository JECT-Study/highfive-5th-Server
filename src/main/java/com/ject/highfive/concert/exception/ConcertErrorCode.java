package com.ject.highfive.concert.exception;

import com.ject.highfive.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ConcertErrorCode implements ErrorCode {
  CONCERT_NOT_FOUND(HttpStatus.NOT_FOUND, "CONCERT_001", "공연을 찾을 수 없습니다.");

  private final HttpStatus status;
  private final String code;
  private final String message;
}
