package com.ject.highfive.auth.service;

import com.ject.highfive.auth.dto.TokenResponse;
import com.ject.highfive.auth.repository.RefreshTokenRepository;
import com.ject.highfive.global.exception.AuthErrorCode;
import com.ject.highfive.global.exception.BusinessException;
import com.ject.highfive.global.security.jwt.JwtProperties;
import com.ject.highfive.global.security.jwt.JwtTokenProvider;
import com.ject.highfive.global.security.jwt.TokenType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

  private final JwtTokenProvider jwtTokenProvider;
  private final JwtProperties jwtProperties;
  private final RefreshTokenRepository refreshTokenRepository;

  // 로그인 성공 시 토큰을 발급한다. 기존 리프레시 토큰을 덮어써 다른 기기는 로그아웃된다
  public TokenResponse issueTokens(Long userId) {
    TokenResponse tokens = createTokens(userId);
    refreshTokenRepository.save(userId, tokens.refreshToken(), jwtProperties.refreshTokenExpiry());
    return tokens;
  }

  // 리프레시 토큰이 잘못됐거나 만료됐거나 저장된 값과 다르면(다른 기기 로그인·이미 쓴 토큰) AUTH_004
  public TokenResponse refresh(String refreshToken) {
    Long userId = parseRefreshToken(refreshToken);
    TokenResponse tokens = createTokens(userId);
    boolean rotated =
        refreshTokenRepository.rotate(
            userId, refreshToken, tokens.refreshToken(), jwtProperties.refreshTokenExpiry());
    if (!rotated) {
      throw new BusinessException(AuthErrorCode.REFRESH_TOKEN_INVALID);
    }
    return tokens;
  }

  public void logout(Long userId) {
    refreshTokenRepository.deleteByUserId(userId);
  }

  private TokenResponse createTokens(Long userId) {
    return new TokenResponse(
        jwtTokenProvider.createAccessToken(userId), jwtTokenProvider.createRefreshToken(userId));
  }

  private Long parseRefreshToken(String refreshToken) {
    try {
      return jwtTokenProvider.parse(refreshToken, TokenType.REFRESH);
    } catch (BusinessException e) {
      throw new BusinessException(AuthErrorCode.REFRESH_TOKEN_INVALID);
    }
  }
}
