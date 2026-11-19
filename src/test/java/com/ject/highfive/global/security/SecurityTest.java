package com.ject.highfive.global.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ject.highfive.auth.repository.RefreshTokenRepository;
import com.ject.highfive.global.security.jwt.JwtProperties;
import com.ject.highfive.global.security.jwt.JwtTokenProvider;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

// 실제 Security 필터 체인을 거쳐 401 응답 형식과 공개·보호 경로를 확인한다
@SpringBootTest
@AutoConfigureMockMvc
class SecurityTest {

  // 로그인이 필요한 API 아무거나 (내 정보 조회)
  private static final String PROTECTED_URL = "/api/v1/users/me";

  @Autowired MockMvc mockMvc;
  @Autowired JwtTokenProvider jwtTokenProvider;
  @Autowired JwtProperties jwtProperties;
  @MockitoBean RefreshTokenRepository refreshTokenRepository;

  private static String bearer(String token) {
    return "Bearer " + token;
  }

  private static void expectError(ResultActions result, String code) throws Exception {
    result
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status").value(401))
        .andExpect(jsonPath("$.code").value(code))
        .andExpect(jsonPath("$.data").doesNotExist());
  }

  @Test
  @DisplayName("토큰 없이 로그인이 필요한 API를 부르면 401 AUTH_001")
  void missingTokenIsAuth001() throws Exception {
    expectError(mockMvc.perform(get(PROTECTED_URL)), "AUTH_001");
  }

  @Test
  @DisplayName("올바른 액세스 토큰이면 통과한다")
  void validAccessTokenPasses() throws Exception {
    mockMvc
        .perform(
            get(PROTECTED_URL)
                .header(HttpHeaders.AUTHORIZATION, bearer(jwtTokenProvider.createAccessToken(7L))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("COMMON_000"));
  }

  @Test
  @DisplayName("만료된 액세스 토큰은 401 AUTH_002")
  void expiredTokenIsAuth002() throws Exception {
    String expired =
        new JwtTokenProvider(
                jwtProperties,
                Clock.fixed(Instant.now().minus(Duration.ofHours(1)), ZoneOffset.UTC))
            .createAccessToken(7L);

    expectError(
        mockMvc.perform(get(PROTECTED_URL).header(HttpHeaders.AUTHORIZATION, bearer(expired))),
        "AUTH_002");
  }

  @Test
  @DisplayName("잘못된 토큰·Bearer가 아닌 헤더·리프레시 토큰은 401 AUTH_003")
  void invalidTokenIsAuth003() throws Exception {
    expectError(
        mockMvc.perform(get(PROTECTED_URL).header(HttpHeaders.AUTHORIZATION, bearer("abc"))),
        "AUTH_003");
    expectError(
        mockMvc.perform(get(PROTECTED_URL).header(HttpHeaders.AUTHORIZATION, "Basic abc")),
        "AUTH_003");
    expectError(
        mockMvc.perform(get(PROTECTED_URL).header(HttpHeaders.AUTHORIZATION, "Bearer ")),
        "AUTH_003");
    expectError(
        mockMvc.perform(
            get(PROTECTED_URL)
                .header(
                    HttpHeaders.AUTHORIZATION, bearer(jwtTokenProvider.createRefreshToken(7L)))),
        "AUTH_003");
  }

  @Test
  @DisplayName("로그인은 토큰 없이, 잘못된 토큰을 함께 보내도 부를 수 있다")
  void loginIsPublic() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/login/kakao")
                .header(HttpHeaders.AUTHORIZATION, bearer("expired-or-broken"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"social-id-token\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
        .andExpect(jsonPath("$.data.refreshToken").isNotEmpty());
  }

  @Test
  @DisplayName("토큰 재발급은 토큰 없이 부를 수 있고 새 토큰 두 개를 돌려준다")
  void refreshIsPublic() throws Exception {
    String refreshToken = jwtTokenProvider.createRefreshToken(7L);
    given(refreshTokenRepository.rotate(eq(7L), eq(refreshToken), anyString(), any()))
        .willReturn(true);

    mockMvc
        .perform(
            post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
        .andExpect(jsonPath("$.data.refreshToken").isNotEmpty());
  }

  @Test
  @DisplayName("재발급할 수 없는 리프레시 토큰은 401 AUTH_004")
  void invalidRefreshTokenIsAuth004() throws Exception {
    given(refreshTokenRepository.rotate(any(), any(), any(), any())).willReturn(false);

    expectError(
        mockMvc.perform(
            post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + jwtTokenProvider.createRefreshToken(7L) + "\"}")),
        "AUTH_004");
  }

  @Test
  @DisplayName("로그아웃은 로그인한 사용자의 리프레시 토큰을 지운다")
  void logoutDeletesRefreshTokenOfLoginUser() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/logout")
                .header(HttpHeaders.AUTHORIZATION, bearer(jwtTokenProvider.createAccessToken(7L))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data").isEmpty());

    verify(refreshTokenRepository).deleteByUserId(7L);
  }

  @Test
  @DisplayName("로그아웃도 토큰이 없으면 401 AUTH_001")
  void logoutRequiresLogin() throws Exception {
    expectError(mockMvc.perform(post("/api/v1/auth/logout")), "AUTH_001");
  }

  @Test
  @DisplayName("Swagger 문서는 로그인 없이 볼 수 있고, 로그인 사용자 파라미터는 노출하지 않는다")
  void swaggerIsPublicAndHidesPrincipal() throws Exception {
    mockMvc
        .perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.paths['/api/v1/auth/logout'].post.parameters").doesNotExist());
  }
}
