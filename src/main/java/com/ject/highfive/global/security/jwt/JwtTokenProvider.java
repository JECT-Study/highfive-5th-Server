package com.ject.highfive.global.security.jwt;

import com.ject.highfive.global.exception.AuthErrorCode;
import com.ject.highfive.global.exception.BusinessException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

// 서비스 자체 JWT(HS256)를 만들고 검증한다.
// 클레임은 사용자 정보 없이 sub(userId)와 type만 넣는다 (그 밖에는 표준 클레임 jti·iat·exp)
public class JwtTokenProvider {

  private static final String TYPE_CLAIM = "type";
  private static final int MIN_SECRET_BYTES = 32;

  private final JwtEncoder encoder;
  private final JwtDecoder decoder;
  private final JwtProperties properties;
  private final Clock clock;

  public JwtTokenProvider(JwtProperties properties, Clock clock) {
    SecretKey key = toSecretKey(properties.secret());
    this.encoder = NimbusJwtEncoder.withSecretKey(key).build();
    NimbusJwtDecoder nimbusDecoder =
        NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
    // 기본 검증기는 만료를 다른 오류와 구분하지 않으므로 끄고, 만료(AUTH_002)는 parse에서 직접 확인한다
    nimbusDecoder.setJwtValidator(jwt -> OAuth2TokenValidatorResult.success());
    this.decoder = nimbusDecoder;
    this.properties = properties;
    this.clock = clock;
  }

  public String createAccessToken(Long userId) {
    return create(userId, TokenType.ACCESS, properties.accessTokenExpiry());
  }

  public String createRefreshToken(Long userId) {
    return create(userId, TokenType.REFRESH, properties.refreshTokenExpiry());
  }

  // 서명·형식이 잘못됐거나 기대한 종류가 아니면 AUTH_003, 만료됐으면 AUTH_002
  public Long parse(String token, TokenType expectedType) {
    Jwt jwt;
    try {
      jwt = decoder.decode(token);
    } catch (JwtException e) {
      throw new BusinessException(AuthErrorCode.INVALID_TOKEN);
    }
    if (!expectedType.name().equals(jwt.getClaimAsString(TYPE_CLAIM))) {
      throw new BusinessException(AuthErrorCode.INVALID_TOKEN);
    }
    Instant expiresAt = jwt.getExpiresAt();
    if (expiresAt == null || !clock.instant().isBefore(expiresAt)) {
      throw new BusinessException(AuthErrorCode.TOKEN_EXPIRED);
    }
    try {
      return Long.valueOf(jwt.getSubject());
    } catch (NumberFormatException e) {
      throw new BusinessException(AuthErrorCode.INVALID_TOKEN);
    }
  }

  private String create(Long userId, TokenType type, Duration expiry) {
    Instant now = clock.instant();
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .id(UUID.randomUUID().toString())
            .subject(String.valueOf(userId))
            .claim(TYPE_CLAIM, type.name())
            .issuedAt(now)
            .expiresAt(now.plus(expiry))
            .build();
    JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
    return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
  }

  private static SecretKey toSecretKey(String secret) {
    if (secret == null || secret.isBlank()) {
      throw new IllegalStateException("jwt.secret(환경변수 JWT_SECRET)이 설정되지 않았습니다.");
    }
    byte[] keyBytes;
    try {
      keyBytes = Base64.getDecoder().decode(secret);
    } catch (IllegalArgumentException e) {
      throw new IllegalStateException("jwt.secret은 Base64로 인코딩한 값이어야 합니다.", e);
    }
    if (keyBytes.length < MIN_SECRET_BYTES) {
      throw new IllegalStateException("jwt.secret은 32바이트(256비트) 이상이어야 합니다.");
    }
    return new SecretKeySpec(keyBytes, "HmacSHA256");
  }
}
