package com.ject.highfive.global.exception;

import org.springframework.http.HttpStatus;

// 에러 코드 공통 인터페이스. 도메인마다 XxxErrorCode enum으로 구현한다 (코드 형식: {DOMAIN}_{세 자리})
public interface ErrorCode {

  HttpStatus getStatus();

  String getCode();

  String getMessage();
}
