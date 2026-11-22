package com.ject.highfive.global.config;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ject.highfive.global.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ApiDocsTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private JwtTokenProvider jwtTokenProvider;

  private String bearerToken() {
    return "Bearer " + jwtTokenProvider.createAccessToken(1L);
  }

  @Test
  @DisplayName("Swagger 문서에 온보딩 API가 노출된다")
  void apiDocsExposeOnboardingApis() throws Exception {
    mockMvc
        .perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.paths", hasKey("/api/v1/auth/login/{provider}")))
        .andExpect(jsonPath("$.paths", hasKey("/api/v1/auth/refresh")))
        .andExpect(jsonPath("$.paths", hasKey("/api/v1/auth/logout")))
        .andExpect(jsonPath("$.paths", hasKey("/api/v1/users/me")))
        .andExpect(jsonPath("$.paths", hasKey("/api/v1/users/me/term-agreements")))
        .andExpect(jsonPath("$.paths", hasKey("/api/v1/users/me/profile")))
        .andExpect(jsonPath("$.paths", hasKey("/api/v1/users/nickname-availability")))
        .andExpect(jsonPath("$.paths", hasKey("/api/v1/terms")))
        .andExpect(jsonPath("$.paths", hasKey("/api/v1/codes")))
        .andExpect(jsonPath("$.paths", hasKey("/api/v1/uploads/presigned-url")));
  }

  @Test
  @DisplayName("껍데기 API도 입력값을 검증한다")
  void stubApiValidatesInput() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/login/kakao")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\" \"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("COMMON_004"))
        .andExpect(jsonPath("$.errors[0].field").value("token"));
  }

  @Test
  @DisplayName("코드 목록은 요청한 종류만 돌려준다")
  void codesReturnOnlyRequestedTypes() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/codes")
                .header(HttpHeaders.AUTHORIZATION, bearerToken())
                .param("types", "GENRE"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.GENRE[0].code").value("ROCK"))
        .andExpect(jsonPath("$.data.POSITION").doesNotExist());
  }

  @Test
  @DisplayName("로그인·토큰 재발급만 Swagger에서 인증 없이 표시된다")
  void publicAuthApisHaveNoSecurityRequirement() throws Exception {
    mockMvc
        .perform(get("/v3/api-docs"))
        .andExpect(jsonPath("$.paths['/api/v1/auth/login/{provider}'].post.security").isEmpty())
        .andExpect(jsonPath("$.paths['/api/v1/auth/refresh'].post.security").isEmpty())
        .andExpect(jsonPath("$.paths['/api/v1/auth/logout'].post.security").doesNotExist())
        .andExpect(jsonPath("$.security[0].bearerAuth").exists());
  }

  @Test
  @DisplayName("코드 종류를 비워 보내면 COMMON 004다")
  void codesWithEmptyTypesIsInvalidInput() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/codes")
                .header(HttpHeaders.AUTHORIZATION, bearerToken())
                .param("types", ""))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("COMMON_004"))
        .andExpect(jsonPath("$.errors[0].field").value("types"));
  }

  @Test
  @DisplayName("코드 종류에 빈 항목이 섞여 있으면 COMMON 004다")
  void codesWithBlankTypeIsInvalidInput() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/codes")
                .header(HttpHeaders.AUTHORIZATION, bearerToken())
                .param("types", "GENRE,"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("COMMON_004"))
        .andExpect(jsonPath("$.errors[0].field").value("types"));
  }

  @Test
  @DisplayName("약관 동의 목록에 null 항목이 있으면 COMMON 004다")
  void termAgreementsWithNullItemIsInvalidInput() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/users/me/term-agreements")
                .header(HttpHeaders.AUTHORIZATION, bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"agreements\":[null]}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("COMMON_004"))
        .andExpect(jsonPath("$.errors[0].field").value("agreements[0]"));
  }

  @Test
  @DisplayName("약관 동의 항목의 필드도 검증한다")
  void termAgreementItemFieldsAreValidated() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/users/me/term-agreements")
                .header(HttpHeaders.AUTHORIZATION, bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"agreements\":[{\"termId\":1}]}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("COMMON_004"))
        .andExpect(jsonPath("$.errors[0].field").value("agreements[0].agreed"));
  }

  @Test
  @DisplayName("올바른 약관 동의 요청은 성공한다")
  void validTermAgreementsSucceed() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/users/me/term-agreements")
                .header(HttpHeaders.AUTHORIZATION, bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"agreements\":[{\"termId\":1,\"agreed\":true}]}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("COMMON_000"));
  }

  @Test
  @DisplayName("@ResponseStatus(CREATED)를 붙인 API는 Swagger에도 201로 표시된다")
  void responseStatusIsDocumentedInSwagger() throws Exception {
    // 테스트 전용 컨트롤러(ExceptionTestController)의 POST /test/created로 확인한다
    mockMvc
        .perform(get("/v3/api-docs"))
        .andExpect(jsonPath("$.paths['/test/created'].post.responses['201']").exists())
        .andExpect(jsonPath("$.paths['/test/created'].post.responses['200']").doesNotExist());
  }

  @Test
  @DisplayName("프로필 저장: 장르·포지션이 비었거나 닉네임에 특수문자가 있으면 COMMON 004")
  void profileValidatesArraysAndNickname() throws Exception {
    mockMvc
        .perform(
            put("/api/v1/users/me/profile")
                .header(HttpHeaders.AUTHORIZATION, bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nickname\":\"두현!\",\"genreCodes\":[],\"positionCodes\":[\" \"]}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("COMMON_004"))
        .andExpect(
            jsonPath("$.errors[*].field")
                .value(containsInAnyOrder("nickname", "genreCodes", "positionCodes[0]")));
  }

  @Test
  @DisplayName("코드 목록에 장르 기타(ETC)와 포지션 직접입력(CUSTOM)이 있다")
  void codesIncludeCustomInputOptions() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/codes")
                .header(HttpHeaders.AUTHORIZATION, bearerToken())
                .param("types", "GENRE,POSITION"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.GENRE[*].code").value(hasItem("ETC")))
        .andExpect(jsonPath("$.data.POSITION[*].code").value(hasItem("CUSTOM")));
  }
}
