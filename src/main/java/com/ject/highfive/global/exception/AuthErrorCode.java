package com.ject.highfive.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {
  // AUTH_001~004는 JECT-211(인증)에서 추가한다
  FORBIDDEN(HttpStatus.FORBIDDEN, "AUTH_005", "권한이 없습니다.");

  private final HttpStatus status;
  private final String code;
  private final String message;
}
