package com.ject.highfive.recruit.controller;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ject.highfive.concert.entity.Genre;
import com.ject.highfive.global.common.response.PageResponse;
import com.ject.highfive.global.exception.BusinessException;
import com.ject.highfive.global.security.SecurityConfig;
import com.ject.highfive.recruit.dto.RecruitPostCreateResponse;
import com.ject.highfive.recruit.dto.RecruitPostSearchRequest;
import com.ject.highfive.recruit.dto.RecruitPostSummaryResponse;
import com.ject.highfive.recruit.entity.RecruitPostStatus;
import com.ject.highfive.recruit.entity.WantedCareerLevel;
import com.ject.highfive.recruit.exception.RecruitErrorCode;
import com.ject.highfive.recruit.service.RecruitPostQueryService;
import com.ject.highfive.recruit.service.RecruitPostService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(controllers = RecruitPostController.class)
@Import(SecurityConfig.class)
// TODO: JECT-249 로그인 사용자로 바꾸면 필터를 켜고 토큰으로 호출한다 (지금은 X-User-Id 헤더로 받음)
@AutoConfigureMockMvc(addFilters = false)
class RecruitPostControllerTest {

  private static final String URL = "/api/v1/concerts/12/recruit-posts";
  private static final String POST_URL = "/api/v1/recruit-posts/5";
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
  @MockitoBean RecruitPostQueryService recruitPostQueryService;

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

  private ResultActions putRecruit(String url, String body) throws Exception {
    return mockMvc.perform(
        put(url).header("X-User-Id", "7").contentType(MediaType.APPLICATION_JSON).content(body));
  }

  @Test
  @DisplayName("수정에 성공하면 200과 빈 data를 돌려주고, 경로의 공고 ID와 헤더의 사용자 ID를 넘긴다")
  void updateReturns200() throws Exception {
    putRecruit(POST_URL, VALID_BODY)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(200))
        .andExpect(jsonPath("$.code").value("COMMON_000"))
        .andExpect(jsonPath("$.data").isMap())
        .andExpect(jsonPath("$.data").isEmpty());
    verify(recruitPostService).update(eq(5L), eq(7L), any());
  }

  @Test
  @DisplayName("수정도 필수값이 모두 빠지면 COMMON_004와 네 필드의 오류를 돌려준다")
  void updateMissingFieldsReturnFieldErrors() throws Exception {
    putRecruit(POST_URL, "{}")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("COMMON_004"))
        .andExpect(
            jsonPath("$.errors[*].field")
                .value(
                    containsInAnyOrder(
                        "wantedCareerLevel", "teamTypes", "deadline", "description")));
    verifyNoInteractions(recruitPostService);
  }

  @Test
  @DisplayName("공고 ID가 숫자가 아니면 500이 아니라 400 COMMON_004")
  void nonNumericRecruitPostIdIsBadRequest() throws Exception {
    putRecruit("/api/v1/recruit-posts/abc", VALID_BODY)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("COMMON_004"));
    verifyNoInteractions(recruitPostService);
  }

  @Test
  @DisplayName("마감에 성공하면 200과 빈 data를 돌려준다")
  void closeReturns200() throws Exception {
    mockMvc
        .perform(post(POST_URL + "/close").header("X-User-Id", "7"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(200))
        .andExpect(jsonPath("$.data").isEmpty());
    verify(recruitPostService).close(5L, 7L);
  }

  @Test
  @DisplayName("이미 마감된 공고를 마감하면 409 RECRUIT_004가 그대로 내려간다")
  void closeAlreadyClosedReturns409() throws Exception {
    willThrow(new BusinessException(RecruitErrorCode.RECRUIT_POST_ALREADY_CLOSED))
        .given(recruitPostService)
        .close(5L, 7L);

    mockMvc
        .perform(post(POST_URL + "/close").header("X-User-Id", "7"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.status").value(409))
        .andExpect(jsonPath("$.code").value("RECRUIT_004"));
  }

  @Test
  @DisplayName("삭제에 성공하면 200과 빈 data를 돌려준다")
  void deleteReturns200() throws Exception {
    mockMvc
        .perform(delete(POST_URL).header("X-User-Id", "7"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(200))
        .andExpect(jsonPath("$.data").isEmpty());
    verify(recruitPostService).delete(5L, 7L);
  }

  @Test
  @DisplayName("없는 공고를 삭제하면 404 RECRUIT_001이 그대로 내려간다")
  void deleteMissingReturns404() throws Exception {
    willThrow(new BusinessException(RecruitErrorCode.RECRUIT_POST_NOT_FOUND))
        .given(recruitPostService)
        .delete(5L, 7L);

    mockMvc
        .perform(delete(POST_URL).header("X-User-Id", "7"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.code").value("RECRUIT_001"));
  }

  private static final String LIST_URL = "/api/v1/recruit-posts";
  private static final RecruitPostSearchRequest NO_CONDITION =
      new RecruitPostSearchRequest(null, null, null, null);

  private ResultActions getList(String query) throws Exception {
    return mockMvc.perform(get(LIST_URL + query).header("X-User-Id", "7"));
  }

  private PageResponse<RecruitPostSummaryResponse> onePage() {
    RecruitPostSummaryResponse summary =
        new RecruitPostSummaryResponse(
            5L,
            12L,
            "겨울 합동공연",
            null,
            List.of(Genre.ROCK, Genre.INDIE),
            "SEOUL",
            LocalDate.of(2999, 12, 20),
            null,
            RecruitPostStatus.RECRUITING,
            LocalDate.of(2999, 12, 1),
            WantedCareerLevel.Y1_TO_3,
            1,
            3,
            true,
            false);
    return new PageResponse<>(List.of(summary), 2, 20, 21, 2, false);
  }

  @Test
  @DisplayName("목록은 다중 파라미터를 묶어 넘기고 페이징 응답과 isFavorite·isMine 이름을 그대로 돌려준다")
  void searchReturnsPageFields() throws Exception {
    given(
            recruitPostQueryService.search(
                new RecruitPostSearchRequest(
                    "SEOUL",
                    List.of(Genre.ROCK, Genre.INDIE),
                    List.of(WantedCareerLevel.UNDER_1Y),
                    "홍대"),
                2,
                20,
                7L))
        .willReturn(onePage());

    getList("?region=SEOUL&genre=ROCK&genre=INDIE&careerLevel=UNDER_1Y&keyword=홍대&page=2&size=20")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(200))
        .andExpect(jsonPath("$.data.page").value(2))
        .andExpect(jsonPath("$.data.size").value(20))
        .andExpect(jsonPath("$.data.totalElements").value(21))
        .andExpect(jsonPath("$.data.totalPages").value(2))
        .andExpect(jsonPath("$.data.hasNext").value(false))
        .andExpect(jsonPath("$.data.content[0].recruitPostId").value(5))
        .andExpect(jsonPath("$.data.content[0].genres[0]").value("ROCK"))
        .andExpect(jsonPath("$.data.content[0].wantedCareerLevel").value("Y1_TO_3"))
        .andExpect(jsonPath("$.data.content[0].isFavorite").value(true))
        .andExpect(jsonPath("$.data.content[0].isMine").value(false));
  }

  @Test
  @DisplayName("파라미터가 없으면 조건 없이 page 1, size 10으로 조회한다")
  void searchDefaults() throws Exception {
    getList("").andExpect(status().isOk());

    verify(recruitPostQueryService).search(NO_CONDITION, 1, 10, 7L);
  }

  @Test
  @DisplayName("page가 1보다 작으면 1, size가 1보다 작으면 10, 100보다 크면 100으로 바꾼다")
  void searchCorrectsPaging() throws Exception {
    getList("?page=0&size=0").andExpect(status().isOk());
    getList("?page=-3&size=101").andExpect(status().isOk());

    verify(recruitPostQueryService).search(NO_CONDITION, 1, 10, 7L);
    verify(recruitPostQueryService).search(NO_CONDITION, 1, 100, 7L);
  }

  @Test
  @DisplayName("목록에 없는 장르 코드는 400 COMMON_004이고 서비스를 부르지 않는다")
  void searchInvalidGenreRejected() throws Exception {
    getList("?genre=HIPHOP")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("COMMON_004"))
        .andExpect(jsonPath("$.errors[0].field").value("genre"));
    verifyNoInteractions(recruitPostQueryService);
  }

  @Test
  @DisplayName("키워드가 50자를 넘으면 400 COMMON_004")
  void searchTooLongKeywordRejected() throws Exception {
    getList("?keyword=" + "가".repeat(51))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("COMMON_004"));
    verifyNoInteractions(recruitPostQueryService);
  }

  @Test
  @DisplayName("숫자가 아닌 page는 400 COMMON_004")
  void searchNonNumericPageRejected() throws Exception {
    getList("?page=abc")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("COMMON_004"));
    verifyNoInteractions(recruitPostQueryService);
  }
}
