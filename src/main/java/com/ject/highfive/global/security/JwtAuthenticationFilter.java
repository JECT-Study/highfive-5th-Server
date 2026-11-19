package com.ject.highfive.global.security;

import com.ject.highfive.global.exception.AuthErrorCode;
import com.ject.highfive.global.exception.BusinessException;
import com.ject.highfive.global.security.jwt.JwtTokenProvider;
import com.ject.highfive.global.security.jwt.TokenType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

// Authorization: Bearer {액세스 토큰}을 검증해 principal이 userId(Long)인 인증 정보를 만든다.
// 토큰이 잘못돼도 여기서 막지 않고 오류 코드만 남긴다. 공개 API는 그대로 처리하고,
// 로그인이 필요한 API에서만 진입점(SecurityConfig)이 그 코드로 401을 내려준다
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  public static final String AUTH_ERROR_ATTRIBUTE =
      JwtAuthenticationFilter.class.getName() + ".error";

  private static final String BEARER_PREFIX = "Bearer ";

  private final JwtTokenProvider jwtTokenProvider;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String header = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (header != null) {
      try {
        Long userId = jwtTokenProvider.parse(extractToken(header), TokenType.ACCESS);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(userId, null, List.of()));
        SecurityContextHolder.setContext(context);
      } catch (BusinessException e) {
        request.setAttribute(AUTH_ERROR_ATTRIBUTE, e.getErrorCode());
      }
    }
    filterChain.doFilter(request, response);
  }

  private String extractToken(String header) {
    if (!header.startsWith(BEARER_PREFIX)) {
      throw new BusinessException(AuthErrorCode.INVALID_TOKEN);
    }
    String token = header.substring(BEARER_PREFIX.length()).trim();
    if (token.isEmpty()) {
      throw new BusinessException(AuthErrorCode.INVALID_TOKEN);
    }
    return token;
  }
}
