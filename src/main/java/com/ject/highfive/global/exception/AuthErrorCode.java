package com.ject.highfive.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {
  TOKEN_MISSING(HttpStatus.UNAUTHORIZED, "AUTH_001", "인증 토큰이 없습니다."),
  // 앱은 이 코드일 때만 토큰 재발급을 시도한다
  TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "AUTH_002", "인증 토큰이 만료되었습니다."),
  INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_003", "유효하지 않은 토큰입니다."),
  // 리프레시 토큰 만료·불일치(다른 기기 로그인 포함). 앱은 로그인 화면으로 보낸다
  REFRESH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "AUTH_004", "다시 로그인해 주세요."),
  FORBIDDEN(HttpStatus.FORBIDDEN, "AUTH_005", "권한이 없습니다."),
  // 소셜 ID 토큰의 서명·iss·aud·만료 검증 실패
  SOCIAL_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "AUTH_006", "소셜 로그인 인증에 실패했습니다."),
  UNSUPPORTED_PROVIDER(HttpStatus.BAD_REQUEST, "AUTH_007", "지원하지 않는 로그인 방식입니다.");

  private final HttpStatus status;
  private final String code;
  private final String message;
}
