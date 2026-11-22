package com.ject.highfive.user.exception;

import com.ject.highfive.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {
  // 저장하는 순간 다른 회원이 같은 닉네임을 쓰고 있음 (중복 확인 이후 먼저 가져간 경우 포함)
  NICKNAME_DUPLICATED(HttpStatus.CONFLICT, "USER_001", "이미 사용 중인 닉네임입니다."),
  // USER_002(닉네임 형식)는 요청 검증(COMMON_004)이 먼저 막아 쓰지 않는다. USER_003(필수 약관)은 JECT-213에서 추가
  USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_005", "존재하지 않는 사용자입니다.");

  private final HttpStatus status;
  private final String code;
  private final String message;
}
