package com.ject.highfive.global.security.jwt;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

// secret은 Base64로 인코딩한 32바이트(256비트) 이상의 값. 환경변수 JWT_SECRET으로만 받는다
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
    String secret, Duration accessTokenExpiry, Duration refreshTokenExpiry) {}
