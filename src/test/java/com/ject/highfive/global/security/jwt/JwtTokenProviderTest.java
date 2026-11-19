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
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

class JwtTokenProviderTest {

  // HS512로도 서명해 볼 수 있게 64바이트 이상으로 둔다 (알고리즘만 다른 토큰을 거부하는지 확인)
  private static final String SECRET =
      base64("test-secret-key-that-is-long-enough-to-sign-with-hs256-and-hs512!!");
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

  @Test
  @DisplayName("서명 없는 토큰(alg: none)과 HS256이 아닌 알고리즘은 AUTH_003")
  void rejectsUnsignedAndOtherAlgorithms() {
    Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
    long exp = NOW.plus(Duration.ofMinutes(30)).getEpochSecond();
    String payload =
        encoder.encodeToString(
            ("{\"sub\":\"7\",\"type\":\"ACCESS\",\"exp\":" + exp + "}").getBytes());
    String unsigned = encoder.encodeToString("{\"alg\":\"none\"}".getBytes()) + "." + payload + ".";
    String hs512 =
        encodeWith(
            JwsHeader.with(MacAlgorithm.HS512).build(),
            JwtClaimsSet.builder()
                .subject("7")
                .claim("type", "ACCESS")
                .expiresAt(NOW.plus(Duration.ofMinutes(30)))
                .build());

    JwtTokenProvider provider = providerAt(NOW);
    assertErrorCode(() -> provider.parse(unsigned, TokenType.ACCESS), AuthErrorCode.INVALID_TOKEN);
    assertErrorCode(() -> provider.parse(hs512, TokenType.ACCESS), AuthErrorCode.INVALID_TOKEN);
  }

  @Test
  @DisplayName("exp가 없는 토큰은 만료(AUTH_002)가 아니라 AUTH_003")
  void tokenWithoutExpiryIsInvalid() {
    String noExp =
        encodeWith(
            JwsHeader.with(MacAlgorithm.HS256).build(),
            JwtClaimsSet.builder().subject("7").claim("type", "ACCESS").build());

    assertErrorCode(
        () -> providerAt(NOW).parse(noExp, TokenType.ACCESS), AuthErrorCode.INVALID_TOKEN);
  }

  @Test
  @DisplayName("null이나 빈 토큰은 AUTH_003")
  void nullOrBlankTokenIsInvalid() {
    JwtTokenProvider provider = providerAt(NOW);

    assertErrorCode(() -> provider.parse(null, TokenType.ACCESS), AuthErrorCode.INVALID_TOKEN);
    assertErrorCode(() -> provider.parse(" ", TokenType.ACCESS), AuthErrorCode.INVALID_TOKEN);
  }

  @Test
  @DisplayName("만료 시간 설정이 없거나 0 이하면 서버가 뜨지 않는다")
  void rejectsInvalidExpiry() {
    Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    assertThatThrownBy(
            () -> new JwtTokenProvider(new JwtProperties(SECRET, null, Duration.ofDays(14)), clock))
        .isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(
            () ->
                new JwtTokenProvider(
                    new JwtProperties(SECRET, Duration.ofMinutes(30), Duration.ZERO), clock))
        .isInstanceOf(IllegalStateException.class);
  }

  // 같은 키로 서명하되 헤더·클레임을 마음대로 정한 토큰
  private static String encodeWith(JwsHeader header, JwtClaimsSet claims) {
    SecretKey key = new SecretKeySpec(Base64.getDecoder().decode(SECRET), "HmacSHA256");
    return NimbusJwtEncoder.withSecretKey(key)
        .algorithm((MacAlgorithm) header.getAlgorithm())
        .build()
        .encode(JwtEncoderParameters.from(header, claims))
        .getTokenValue();
  }
}
