package com.ject.highfive.global.config;

import static org.hamcrest.Matchers.hasKey;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ApiDocsTest {

  @Autowired private MockMvc mockMvc;

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
        .perform(get("/api/v1/codes").param("types", "GENRE"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.GENRE[0].code").value("ROCK"))
        .andExpect(jsonPath("$.data.POSITION").doesNotExist());
  }
}
