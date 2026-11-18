package com.ject.highfive.recruit.controller;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ject.highfive.global.exception.BusinessException;
import com.ject.highfive.global.security.SecurityConfig;
import com.ject.highfive.recruit.dto.RecruitPostCreateResponse;
import com.ject.highfive.recruit.exception.RecruitErrorCode;
import com.ject.highfive.recruit.service.RecruitPostService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(controllers = RecruitPostController.class)
@Import(SecurityConfig.class)
class RecruitPostControllerTest {

  private static final String URL = "/api/v1/concerts/12/recruit-posts";
  private static final String VALID_BODY =
      """
      {
        "wantedCareerLevel": "Y1_TO_3",
        "teamTypes": ["FIRST_PERFORMANCE", "ORIGINAL_SONGS"],
        "deadline": "2999-12-01",
        "description": "12월 홍대에서 함께할 팀을 찾습니다."
      }
      """;

  @Autowired MockMvc mockMvc;
  @MockitoBean RecruitPostService recruitPostService;

  private ResultActions postRecruit(String body) throws Exception {
    return mockMvc.perform(
        post(URL).header("X-User-Id", "7").contentType(MediaType.APPLICATION_JSON).content(body));
  }

  @Test
  @DisplayName("작성에 성공하면 201과 recruitPostId를 돌려주고, 경로의 공연 ID와 헤더의 사용자 ID를 넘긴다")
  void createReturns201() throws Exception {
    given(recruitPostService.create(eq(12L), eq(7L), any()))
        .willReturn(new RecruitPostCreateResponse(5L));

    postRecruit(VALID_BODY)
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value(201))
        .andExpect(jsonPath("$.code").value("COMMON_000"))
        .andExpect(jsonPath("$.data.recruitPostId").value(5));
    verify(recruitPostService).create(eq(12L), eq(7L), any());
  }

  @Test
  @DisplayName("필수값이 모두 빠지면 COMMON_004와 네 필드의 오류를 돌려준다")
  void missingFieldsReturnFieldErrors() throws Exception {
    postRecruit("{}")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.code").value("COMMON_004"))
        .andExpect(
            jsonPath("$.errors[*].field")
                .value(
                    containsInAnyOrder(
                        "wantedCareerLevel", "teamTypes", "deadline", "description")));
    verifyNoInteractions(recruitPostService);
  }

  @Test
  @DisplayName("공고 소개가 500자를 넘으면 COMMON_004")
  void descriptionOver500Rejected() throws Exception {
    String body = VALID_BODY.replace("12월 홍대에서 함께할 팀을 찾습니다.", "가".repeat(501));

    postRecruit(body)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("COMMON_004"))
        .andExpect(jsonPath("$.errors[0].field").value("description"));
    verifyNoInteractions(recruitPostService);
  }

  @Test
  @DisplayName("공고 소개가 정확히 500자면 통과한다")
  void description500Accepted() throws Exception {
    given(recruitPostService.create(eq(12L), eq(7L), any()))
        .willReturn(new RecruitPostCreateResponse(5L));
    String body = VALID_BODY.replace("12월 홍대에서 함께할 팀을 찾습니다.", "가".repeat(500));

    postRecruit(body).andExpect(status().isCreated());
  }

  @Test
  @DisplayName("팀 유형이 빈 배열이면 COMMON_004")
  void emptyTeamTypesRejected() throws Exception {
    String body = VALID_BODY.replace("[\"FIRST_PERFORMANCE\", \"ORIGINAL_SONGS\"]", "[]");

    postRecruit(body)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("COMMON_004"))
        .andExpect(jsonPath("$.errors[0].field").value("teamTypes"));
    verifyNoInteractions(recruitPostService);
  }

  @Test
  @DisplayName("목록에 없는 팀 유형 코드는 500이 아니라 400 COMMON_001")
  void unknownTeamTypeIsBadRequest() throws Exception {
    postRecruit(VALID_BODY.replace("\"FIRST_PERFORMANCE\"", "\"UNKNOWN\""))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("COMMON_001"));
    verifyNoInteractions(recruitPostService);
  }

  @Test
  @DisplayName("팀 유형에 null 원소가 있으면 COMMON_004")
  void nullTeamTypeElementRejected() throws Exception {
    postRecruit(VALID_BODY.replace("\"FIRST_PERFORMANCE\"", "null"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("COMMON_004"))
        .andExpect(jsonPath("$.errors[0].field").value("teamTypes[0]"));
    verifyNoInteractions(recruitPostService);
  }

  @Test
  @DisplayName("X-User-Id가 숫자가 아니면 500이 아니라 400 COMMON_004")
  void nonNumericUserIdHeaderIsBadRequest() throws Exception {
    mockMvc
        .perform(
            post(URL)
                .header("X-User-Id", "abc")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BODY))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("COMMON_004"));
    verifyNoInteractions(recruitPostService);
  }

  @Test
  @DisplayName("서비스의 업무 오류는 그 코드와 상태로 내려간다")
  void businessErrorPassesThrough() throws Exception {
    given(recruitPostService.create(eq(12L), eq(7L), any()))
        .willThrow(new BusinessException(RecruitErrorCode.RECRUIT_POST_ALREADY_EXISTS));

    postRecruit(VALID_BODY)
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.status").value(409))
        .andExpect(jsonPath("$.code").value("RECRUIT_002"));
  }
}
