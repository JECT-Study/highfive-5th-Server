package com.ject.highfive.global.exception;

import lombok.Getter;

// 서비스 계층에서 정해진 에러 코드로 실패를 알릴 때 던진다
@Getter
public class BusinessException extends RuntimeException {

  private final ErrorCode errorCode;

  public BusinessException(ErrorCode errorCode) {
    super(errorCode.getMessage());
    this.errorCode = errorCode;
  }
}
