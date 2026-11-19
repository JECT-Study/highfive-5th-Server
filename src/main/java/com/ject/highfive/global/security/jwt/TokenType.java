package com.ject.highfive.global.security.jwt;

// JWT의 type 클레임. 리프레시 토큰으로 API를 호출하거나 그 반대를 막는 데 쓴다
public enum TokenType {
  ACCESS,
  REFRESH
}
