package com.ject.highfive.global.security;

import com.ject.highfive.global.exception.AuthErrorCode;
import com.ject.highfive.global.exception.ErrorCode;
import com.ject.highfive.global.security.jwt.JwtProperties;
import com.ject.highfive.global.security.jwt.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import tools.jackson.databind.json.JsonMapper;

// Security는 로그인 여부(401)만 판단한다. 밴드·공연별 역할(403 AUTH_005)은 서비스 계층에서 확인한다.
// 컨트롤러는 @AuthenticationPrincipal Long userId로 로그인 사용자를 받는다.
// JWT 관련 빈을 여기서 직접 만들어 @WebMvcTest에서 @Import(SecurityConfig.class)만으로 동작하게 한다
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

  private static final String[] PUBLIC_PATHS = {
    "/api/v1/auth/login/**",
    "/api/v1/auth/refresh",
    "/api/v1/public/**",
    "/swagger-ui.html",
    "/swagger-ui/**",
    "/v3/api-docs/**",
    // 배포 헬스 체크용 (actuator는 JECT-242에서 추가)
    "/actuator/health",
    "/error"
  };

  @Bean
  public JwtTokenProvider jwtTokenProvider(JwtProperties jwtProperties) {
    return new JwtTokenProvider(jwtProperties, Clock.systemUTC());
  }

  @Bean
  public SecurityFilterChain securityFilterChain(
      HttpSecurity http, JwtTokenProvider jwtTokenProvider, JsonMapper jsonMapper)
      throws Exception {
    SecurityErrorResponseWriter errorWriter = new SecurityErrorResponseWriter(jsonMapper);
    http.csrf(AbstractHttpConfigurer::disable)
        .formLogin(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable)
        .logout(AbstractHttpConfigurer::disable)
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth -> auth.requestMatchers(PUBLIC_PATHS).permitAll().anyRequest().authenticated())
        .exceptionHandling(
            exception ->
                exception
                    .authenticationEntryPoint(
                        (request, response, e) ->
                            errorWriter.write(response, authenticationErrorOf(request)))
                    .accessDeniedHandler(
                        (request, response, e) ->
                            errorWriter.write(response, AuthErrorCode.FORBIDDEN)))
        .addFilterBefore(
            new JwtAuthenticationFilter(jwtTokenProvider),
            UsernamePasswordAuthenticationFilter.class);
    return http.build();
  }

  // 필터가 남긴 오류(만료 AUTH_002, 잘못된 토큰 AUTH_003)가 없으면 토큰을 안 보낸 것(AUTH_001)
  private ErrorCode authenticationErrorOf(HttpServletRequest request) {
    Object error = request.getAttribute(JwtAuthenticationFilter.AUTH_ERROR_ATTRIBUTE);
    return error instanceof ErrorCode errorCode ? errorCode : AuthErrorCode.TOKEN_MISSING;
  }
}
