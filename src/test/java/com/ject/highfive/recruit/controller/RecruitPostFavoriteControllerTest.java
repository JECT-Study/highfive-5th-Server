package com.ject.highfive.recruit.controller;

import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ject.highfive.global.exception.BusinessException;
import com.ject.highfive.global.security.SecurityConfig;
import com.ject.highfive.recruit.exception.RecruitErrorCode;
import com.ject.highfive.recruit.service.RecruitPostFavoriteService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = RecruitPostFavoriteController.class)
@Import(SecurityConfig.class)
// TODO: JECT-249 로그인 사용자로 바꾸면 필터를 켜고 토큰으로 호출한다 (지금은 X-User-Id 헤더로 받음)
@AutoConfigureMockMvc(addFilters = false)
class RecruitPostFavoriteControllerTest {

  private static final String URL = "/api/v1/recruit-posts/5/favorite";

  @Autowired MockMvc mockMvc;
  @MockitoBean RecruitPostFavoriteService favoriteService;

  @Test
  @DisplayName("찜에 성공하면 200과 빈 data를 돌려주고, 경로의 공고 ID와 헤더의 사용자 ID를 넘긴다")
  void favoriteReturns200() throws Exception {
    mockMvc
        .perform(put(URL).header("X-User-Id", "7"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(200))
        .andExpect(jsonPath("$.code").value("COMMON_000"))
        .andExpect(jsonPath("$.data").isMap())
        .andExpect(jsonPath("$.data").isEmpty());
    verify(favoriteService).favorite(5L, 7L);
  }

  @Test
  @DisplayName("없는 공고를 찜하면 404 RECRUIT_001")
  void favoriteMissingPostReturns404() throws Exception {
    willThrow(new BusinessException(RecruitErrorCode.RECRUIT_POST_NOT_FOUND))
        .given(favoriteService)
        .favorite(5L, 7L);

    mockMvc
        .perform(put(URL).header("X-User-Id", "7"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.code").value("RECRUIT_001"));
  }

  @Test
  @DisplayName("찜 해제에 성공하면 200과 빈 data를 돌려준다")
  void unfavoriteReturns200() throws Exception {
    mockMvc
        .perform(delete(URL).header("X-User-Id", "7"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(200))
        .andExpect(jsonPath("$.data").isEmpty());
    verify(favoriteService).unfavorite(5L, 7L);
  }

  @Test
  @DisplayName("공고 ID가 숫자가 아니면 400 COMMON_004이고 서비스를 부르지 않는다")
  void nonNumericIdReturns400() throws Exception {
    mockMvc
        .perform(put("/api/v1/recruit-posts/abc/favorite").header("X-User-Id", "7"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("COMMON_004"));
    verifyNoInteractions(favoriteService);
  }
}
