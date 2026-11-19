package com.ject.highfive.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.ject.highfive.auth.dto.TokenResponse;
import com.ject.highfive.auth.repository.RefreshTokenRepository;
import com.ject.highfive.global.exception.AuthErrorCode;
import com.ject.highfive.global.exception.BusinessException;
import com.ject.highfive.global.security.jwt.JwtProperties;
import com.ject.highfive.global.security.jwt.JwtTokenProvider;
import com.ject.highfive.global.security.jwt.TokenType;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AuthServiceTest {

  private static final Duration REFRESH_EXPIRY = Duration.ofDays(14);
  private static final JwtProperties PROPERTIES =
      new JwtProperties(
          Base64.getEncoder()
              .encodeToString("test-secret-key-that-is-at-least-32-bytes!!".getBytes()),
          Duration.ofMinutes(30),
          REFRESH_EXPIRY);

  private final JwtTokenProvider jwtTokenProvider =
      new JwtTokenProvider(PROPERTIES, Clock.systemUTC());
  private final RefreshTokenRepository refreshTokenRepository = mock(RefreshTokenRepository.class);
  private final AuthService authService =
      new AuthService(jwtTokenProvider, PROPERTIES, refreshTokenRepository);

  private static void assertRefreshRejected(Runnable action) {
    assertThatThrownBy(action::run)
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(AuthErrorCode.REFRESH_TOKEN_INVALID);
  }

  @Test
  @DisplayName("로그인하면 토큰을 발급하고 리프레시 토큰을 14일 동안 저장한다")
  void issueTokensSavesRefreshToken() {
    TokenResponse tokens = authService.issueTokens(7L);

    assertThat(jwtTokenProvider.parse(tokens.accessToken(), TokenType.ACCESS)).isEqualTo(7L);
    assertThat(jwtTokenProvider.parse(tokens.refreshToken(), TokenType.REFRESH)).isEqualTo(7L);
    verify(refreshTokenRepository).save(7L, tokens.refreshToken(), REFRESH_EXPIRY);
  }

  @Test
  @DisplayName("재발급하면 저장된 리프레시 토큰을 새 토큰으로 바꾼다")
  void refreshRotatesToken() {
    String refreshToken = jwtTokenProvider.createRefreshToken(7L);
    given(refreshTokenRepository.rotate(eq(7L), eq(refreshToken), anyString(), eq(REFRESH_EXPIRY)))
        .willReturn(true);

    TokenResponse tokens = authService.refresh(refreshToken);

    assertThat(tokens.refreshToken()).isNotEqualTo(refreshToken);
    assertThat(jwtTokenProvider.parse(tokens.accessToken(), TokenType.ACCESS)).isEqualTo(7L);
    verify(refreshTokenRepository).rotate(7L, refreshToken, tokens.refreshToken(), REFRESH_EXPIRY);
  }

  @Test
  @DisplayName("저장된 값과 다른 리프레시 토큰(다른 기기 로그인·이미 쓴 토큰)은 AUTH_004")
  void refreshWithReplacedTokenIsRejected() {
    String refreshToken = jwtTokenProvider.createRefreshToken(7L);
    given(refreshTokenRepository.rotate(any(), any(), any(), any())).willReturn(false);

    assertRefreshRejected(() -> authService.refresh(refreshToken));
  }

  @Test
  @DisplayName("잘못됐거나 만료됐거나 액세스 토큰을 보내면 저장소를 보지 않고 AUTH_004")
  void refreshWithInvalidTokenIsRejected() {
    String expired =
        new JwtTokenProvider(
                PROPERTIES, Clock.fixed(Instant.now().minus(Duration.ofDays(15)), ZoneOffset.UTC))
            .createRefreshToken(7L);

    assertRefreshRejected(() -> authService.refresh("not-a-jwt"));
    assertRefreshRejected(() -> authService.refresh(expired));
    assertRefreshRejected(() -> authService.refresh(jwtTokenProvider.createAccessToken(7L)));
    verify(refreshTokenRepository, never()).rotate(any(), any(), any(), any());
  }

  @Test
  @DisplayName("로그아웃하면 저장된 리프레시 토큰을 지운다")
  void logoutDeletesRefreshToken() {
    authService.logout(7L);

    verify(refreshTokenRepository).deleteByUserId(7L);
  }
}
