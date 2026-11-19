package com.ject.highfive.global.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ject.highfive.global.exception.AuthErrorCode;
import com.ject.highfive.global.exception.BusinessException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

  private static final String SECRET = base64("test-secret-key-that-is-at-least-32-bytes!!");
  private static final Instant NOW = Instant.parse("2026-11-19T10:00:00Z");

  private static String base64(String value) {
    return Base64.getEncoder().encodeToString(value.getBytes());
  }

  private static JwtTokenProvider providerAt(Instant instant) {
    return providerAt(instant, SECRET);
  }

  private static JwtTokenProvider providerAt(Instant instant, String secret) {
    JwtProperties properties =
        new JwtProperties(secret, Duration.ofMinutes(30), Duration.ofDays(14));
    return new JwtTokenProvider(properties, Clock.fixed(instant, ZoneOffset.UTC));
  }

  private static void assertErrorCode(Runnable action, AuthErrorCode expected) {
    assertThatThrownBy(action::run)
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(expected);
  }

  @Test
  @DisplayName("액세스·리프레시 토큰에서 userId를 꺼낸다")
  void parseReturnsUserId() {
    JwtTokenProvider provider = providerAt(NOW);

    assertThat(provider.parse(provider.createAccessToken(7L), TokenType.ACCESS)).isEqualTo(7L);
    assertThat(provider.parse(provider.createRefreshToken(7L), TokenType.REFRESH)).isEqualTo(7L);
  }

  @Test
  @DisplayName("같은 사용자라도 발급할 때마다 다른 토큰이다")
  void tokensAreUnique() {
    JwtTokenProvider provider = providerAt(NOW);

    assertThat(provider.createRefreshToken(7L)).isNotEqualTo(provider.createRefreshToken(7L));
  }

  @Test
  @DisplayName("액세스 토큰은 30분 뒤 만료되어 AUTH_002")
  void accessTokenExpiresAfter30Minutes() {
    String token = providerAt(NOW).createAccessToken(7L);

    assertThat(providerAt(NOW.plus(Duration.ofMinutes(29))).parse(token, TokenType.ACCESS))
        .isEqualTo(7L);
    assertErrorCode(
        () -> providerAt(NOW.plus(Duration.ofMinutes(30))).parse(token, TokenType.ACCESS),
        AuthErrorCode.TOKEN_EXPIRED);
  }

  @Test
  @DisplayName("리프레시 토큰은 14일 뒤 만료되어 AUTH_002")
  void refreshTokenExpiresAfter14Days() {
    String token = providerAt(NOW).createRefreshToken(7L);

    assertThat(providerAt(NOW.plus(Duration.ofDays(13))).parse(token, TokenType.REFRESH))
        .isEqualTo(7L);
    assertErrorCode(
        () -> providerAt(NOW.plus(Duration.ofDays(14))).parse(token, TokenType.REFRESH),
        AuthErrorCode.TOKEN_EXPIRED);
  }

  @Test
  @DisplayName("종류가 다른 토큰은 AUTH_003")
  void wrongTypeIsInvalid() {
    JwtTokenProvider provider = providerAt(NOW);

    assertErrorCode(
        () -> provider.parse(provider.createRefreshToken(7L), TokenType.ACCESS),
        AuthErrorCode.INVALID_TOKEN);
    assertErrorCode(
        () -> provider.parse(provider.createAccessToken(7L), TokenType.REFRESH),
        AuthErrorCode.INVALID_TOKEN);
  }

  @Test
  @DisplayName("다른 키로 서명했거나 내용을 바꾼 토큰은 AUTH_003")
  void tamperedTokenIsInvalid() {
    String otherKeyToken =
        providerAt(NOW, base64("another-secret-key-that-is-at-least-32-bytes"))
            .createAccessToken(7L);
    String token = providerAt(NOW).createAccessToken(7L);
    String[] parts = token.split("\\.");
    String forgedPayload =
        Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(
                new String(Base64.getUrlDecoder().decode(parts[1]))
                    .replace("\"7\"", "\"8\"")
                    .getBytes());
    String forged = parts[0] + "." + forgedPayload + "." + parts[2];

    JwtTokenProvider provider = providerAt(NOW);
    assertErrorCode(
        () -> provider.parse(otherKeyToken, TokenType.ACCESS), AuthErrorCode.INVALID_TOKEN);
    assertErrorCode(() -> provider.parse(forged, TokenType.ACCESS), AuthErrorCode.INVALID_TOKEN);
    assertErrorCode(
        () -> provider.parse("not-a-jwt", TokenType.ACCESS), AuthErrorCode.INVALID_TOKEN);
  }

  @Test
  @DisplayName("서명 키가 없거나 32바이트보다 짧으면 서버가 뜨지 않는다")
  void rejectsWeakSecret() {
    assertThatThrownBy(() -> providerAt(NOW, null)).isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(() -> providerAt(NOW, base64("short-secret")))
        .isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(() -> providerAt(NOW, "not base64!"))
        .isInstanceOf(IllegalStateException.class);
  }
}
