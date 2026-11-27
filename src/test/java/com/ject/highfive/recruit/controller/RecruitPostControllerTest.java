package com.ject.highfive.recruit.controller;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
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
import com.ject.highfive.global.security.jwt.JwtTokenProvider;
import com.ject.highfive.recruit.dto.BandSearchRequest;
import com.ject.highfive.recruit.dto.BandSummaryResponse;
import com.ject.highfive.recruit.dto.RecruitPostCreateResponse;
import com.ject.highfive.recruit.dto.RecruitPostDetailResponse;
import com.ject.highfive.recruit.dto.RecruitPostSearchRequest;
import com.ject.highfive.recruit.dto.RecruitPostSummaryResponse;
import com.ject.highfive.recruit.entity.RecruitPostStatus;
import com.ject.highfive.recruit.entity.TeamType;
import com.ject.highfive.recruit.entity.WantedCareerLevel;
import com.ject.highfive.recruit.exception.RecruitErrorCode;
import com.ject.highfive.recruit.service.RecruitPostQueryService;
import com.ject.highfive.recruit.service.RecruitPostService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(controllers = RecruitPostController.class)
@Import(SecurityConfig.class)
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
  @Autowired JwtTokenProvider jwtTokenProvider;
  @MockitoBean RecruitPostService recruitPostService;
  @MockitoBean RecruitPostQueryService recruitPostQueryService;

  private String bearer() {
    return "Bearer " + jwtTokenProvider.createAccessToken(7L);
  }

  private ResultActions postRecruit(String body) throws Exception {
    return mockMvc.perform(
        post(URL)
            .header(HttpHeaders.AUTHORIZATION, bearer())
            .contentType(MediaType.APPLICATION_JSON)
            .content(body));
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
        put(url)
            .header(HttpHeaders.AUTHORIZATION, bearer())
            .contentType(MediaType.APPLICATION_JSON)
            .content(body));
  }

  @Test
  @DisplayName("수정에 성공하면 200과 빈 data를 돌려주고, 경로의 공고 ID와 토큰의 사용자 ID를 넘긴다")
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
        .perform(post(POST_URL + "/close").header(HttpHeaders.AUTHORIZATION, bearer()))
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
        .perform(post(POST_URL + "/close").header(HttpHeaders.AUTHORIZATION, bearer()))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.status").value(409))
        .andExpect(jsonPath("$.code").value("RECRUIT_004"));
  }

  @Test
  @DisplayName("삭제에 성공하면 200과 빈 data를 돌려준다")
  void deleteReturns200() throws Exception {
    mockMvc
        .perform(delete(POST_URL).header(HttpHeaders.AUTHORIZATION, bearer()))
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
        .perform(delete(POST_URL).header(HttpHeaders.AUTHORIZATION, bearer()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.code").value("RECRUIT_001"));
  }

  private static final String LIST_URL = "/api/v1/recruit-posts";
  private static final RecruitPostSearchRequest NO_CONDITION =
      new RecruitPostSearchRequest(null, null, null, null, false);

  private ResultActions getList(String query) throws Exception {
    return mockMvc.perform(get(LIST_URL + query).header(HttpHeaders.AUTHORIZATION, bearer()));
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
                    "홍대",
                    true),
                2,
                20,
                7L))
        .willReturn(onePage());

    getList(
            "?region=SEOUL&genre=ROCK&genre=INDIE&careerLevel=UNDER_1Y&keyword=홍대"
                + "&hideClosed=true&page=2&size=20")
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
  @DisplayName("page가 10000을 넘으면 400 COMMON_004. 10000까지는 받는다")
  void searchTooLargePageRejected() throws Exception {
    getList("?page=10001")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("COMMON_004"));
    verifyNoInteractions(recruitPostQueryService);

    getList("?page=10000").andExpect(status().isOk());
  }

  @Test
  @DisplayName("숫자가 아닌 page는 400 COMMON_004")
  void searchNonNumericPageRejected() throws Exception {
    getList("?page=abc")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("COMMON_004"));
    verifyNoInteractions(recruitPostQueryService);
  }

  @Test
  @DisplayName("hideClosed가 true/false가 아니면 400 COMMON_004")
  void searchInvalidHideClosedRejected() throws Exception {
    getList("?hideClosed=abc")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("COMMON_004"));
    verifyNoInteractions(recruitPostQueryService);
  }

  private RecruitPostDetailResponse detailResponse() {
    RecruitPostDetailResponse.ConcertInfo concert =
        new RecruitPostDetailResponse.ConcertInfo(
            12L,
            "겨울 합동공연",
            List.of(Genre.ROCK, Genre.INDIE),
            LocalDate.of(2999, 12, 20),
            null,
            null,
            "홍대 롤링홀",
            "서울 마포구 어울마당로 35",
            "SEOUL",
            500000,
            null);
    RecruitPostDetailResponse.TeamInfo host =
        new RecruitPostDetailResponse.TeamInfo(
            true,
            new RecruitPostDetailResponse.BandInfo(3L, "소음공장", null, List.of(Genre.ROCK), 4),
            List.of());
    return new RecruitPostDetailResponse(
        5L,
        RecruitPostStatus.RECRUITING,
        LocalDate.of(2999, 12, 1),
        WantedCareerLevel.Y1_TO_3,
        List.of(TeamType.FIRST_PERFORMANCE),
        "12월 홍대에서 함께할 팀을 찾습니다.",
        0,
        3,
        concert,
        List.of(host),
        true,
        false,
        true);
  }

  @Test
  @DisplayName("상세는 경로의 공고 ID와 헤더의 사용자 ID를 넘기고 v0.2 필드 이름 그대로 돌려준다")
  void detailReturnsFields() throws Exception {
    given(recruitPostQueryService.getDetail(5L, 7L)).willReturn(detailResponse());

    mockMvc
        .perform(get(POST_URL).header(HttpHeaders.AUTHORIZATION, bearer()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("COMMON_000"))
        .andExpect(jsonPath("$.data.recruitPostId").value(5))
        .andExpect(jsonPath("$.data.status").value("RECRUITING"))
        .andExpect(jsonPath("$.data.teamTypes[0]").value("FIRST_PERFORMANCE"))
        .andExpect(jsonPath("$.data.joinedTeamCount").value(0))
        .andExpect(jsonPath("$.data.recruitTeamCount").value(3))
        .andExpect(jsonPath("$.data.concert.genres[0]").value("ROCK"))
        .andExpect(jsonPath("$.data.concert.startTime").value(nullValue()))
        .andExpect(jsonPath("$.data.teams[0].isHost").value(true))
        .andExpect(jsonPath("$.data.teams[0].band.bandId").value(3))
        .andExpect(jsonPath("$.data.teams[0].band.memberCount").value(4))
        .andExpect(jsonPath("$.data.teams[0].setlist").isEmpty())
        .andExpect(jsonPath("$.data.isFavorite").value(true))
        .andExpect(jsonPath("$.data.isMine").value(false))
        .andExpect(jsonPath("$.data.canInquire").value(true))
        .andExpect(jsonPath("$.data", not(hasKey("inquiryChatRoomId"))))
        .andExpect(jsonPath("$.data.concert", not(hasKey("customGenre"))))
        .andExpect(jsonPath("$.data.teams[0].band", not(hasKey("customGenre"))));
  }

  @Test
  @DisplayName("공고가 없으면 404 RECRUIT_001")
  void detailNotFound() throws Exception {
    given(recruitPostQueryService.getDetail(5L, 7L))
        .willThrow(new BusinessException(RecruitErrorCode.RECRUIT_POST_NOT_FOUND));

    mockMvc
        .perform(get(POST_URL).header(HttpHeaders.AUTHORIZATION, bearer()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("RECRUIT_001"));
  }

  @Test
  @DisplayName("공고 ID가 숫자가 아니면 400 COMMON_004")
  void detailNonNumericId() throws Exception {
    mockMvc
        .perform(get("/api/v1/recruit-posts/abc").header(HttpHeaders.AUTHORIZATION, bearer()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("COMMON_004"));
    verifyNoInteractions(recruitPostQueryService);
  }

  @Test
  @DisplayName("토큰 없이 부르면 401 AUTH_001이고 서비스를 부르지 않는다")
  void withoutTokenIsUnauthorized() throws Exception {
    mockMvc
        .perform(get(POST_URL))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("AUTH_001"));
    verifyNoInteractions(recruitPostQueryService);
  }

  private static final String BANDS_URL = "/api/v1/recruit-posts/bands";

  private ResultActions getBands(String query) throws Exception {
    return mockMvc.perform(get(BANDS_URL + query).header(HttpHeaders.AUTHORIZATION, bearer()));
  }

  @Test
  @DisplayName("팀 검색은 다중 파라미터를 묶어 넘기고 팀 카드와 페이징 필드를 돌려준다")
  void searchBandsReturnsPageFields() throws Exception {
    BandSummaryResponse band =
        new BandSummaryResponse(
            1L,
            "새벽세시",
            null,
            List.of(Genre.ROCK, Genre.INDIE),
            WantedCareerLevel.Y1_TO_3,
            "SEOUL",
            4);
    given(
            recruitPostQueryService.searchBands(
                new BandSearchRequest(
                    "새벽",
                    "SEOUL",
                    List.of(Genre.ROCK, Genre.INDIE),
                    List.of(WantedCareerLevel.ANY, WantedCareerLevel.Y1_TO_3)),
                2,
                20))
        .willReturn(new PageResponse<>(List.of(band), 2, 20, 21, 2, false));

    getBands(
            "?keyword=새벽&region=SEOUL&genre=ROCK&genre=INDIE"
                + "&careerLevel=ANY&careerLevel=Y1_TO_3&page=2&size=20")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("COMMON_000"))
        .andExpect(jsonPath("$.data.page").value(2))
        .andExpect(jsonPath("$.data.size").value(20))
        .andExpect(jsonPath("$.data.totalElements").value(21))
        .andExpect(jsonPath("$.data.totalPages").value(2))
        .andExpect(jsonPath("$.data.hasNext").value(false))
        .andExpect(jsonPath("$.data.content[0].bandId").value(1))
        .andExpect(jsonPath("$.data.content[0].name").value("새벽세시"))
        .andExpect(jsonPath("$.data.content[0].profileImageUrl").value(nullValue()))
        .andExpect(jsonPath("$.data.content[0].genres[1]").value("INDIE"))
        .andExpect(jsonPath("$.data.content[0].careerLevel").value("Y1_TO_3"))
        .andExpect(jsonPath("$.data.content[0].region").value("SEOUL"))
        .andExpect(jsonPath("$.data.content[0].memberCount").value(4))
        .andExpect(jsonPath("$.data.content[0]", not(hasKey("customGenre"))));
  }

  @Test
  @DisplayName("팀 검색은 page가 1보다 작으면 1, size가 1보다 작으면 10, 100보다 크면 100으로 바꾼다")
  void searchBandsCorrectsPaging() throws Exception {
    getBands("?keyword=새벽&page=0&size=0").andExpect(status().isOk());
    getBands("?keyword=새벽&page=-3&size=101").andExpect(status().isOk());

    BandSearchRequest request = new BandSearchRequest("새벽", null, null, null);
    verify(recruitPostQueryService).searchBands(request, 1, 10);
    verify(recruitPostQueryService).searchBands(request, 1, 100);
  }

  @Test
  @DisplayName("팀 검색은 파라미터가 없으면 조건 없이 page 1, size 10으로 조회한다")
  void searchBandsDefaults() throws Exception {
    getBands("").andExpect(status().isOk());

    verify(recruitPostQueryService)
        .searchBands(new BandSearchRequest(null, null, null, null), 1, 10);
  }

  @Test
  @DisplayName("팀 검색 keyword가 50자를 넘으면 400 COMMON_004. 50자까지는 받는다")
  void searchBandsTooLongKeywordRejected() throws Exception {
    getBands("?keyword=" + "가".repeat(51))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("COMMON_004"));
    verifyNoInteractions(recruitPostQueryService);

    getBands("?keyword=" + "가".repeat(50)).andExpect(status().isOk());
  }

  @Test
  @DisplayName("팀 검색에 목록에 없는 genre·careerLevel 코드는 400 COMMON_004")
  void searchBandsInvalidCodeRejected() throws Exception {
    getBands("?keyword=새벽&genre=HIPHOP")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("COMMON_004"));
    getBands("?keyword=새벽&careerLevel=FOREVER")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("COMMON_004"));
    verifyNoInteractions(recruitPostQueryService);
  }

  @Test
  @DisplayName("팀 검색에 page가 10000을 넘으면 400 COMMON_004")
  void searchBandsTooLargePageRejected() throws Exception {
    getBands("?keyword=새벽&page=10001")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("COMMON_004"));
    verifyNoInteractions(recruitPostQueryService);
  }

  @Test
  @DisplayName("팀 검색을 토큰 없이 부르면 401 AUTH_001")
  void searchBandsWithoutTokenIsUnauthorized() throws Exception {
    mockMvc
        .perform(get(BANDS_URL + "?keyword=새벽"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("AUTH_001"));
    verifyNoInteractions(recruitPostQueryService);
  }
}
