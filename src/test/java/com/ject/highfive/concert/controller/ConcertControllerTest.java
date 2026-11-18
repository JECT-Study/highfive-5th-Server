package com.ject.highfive.concert.controller;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ject.highfive.concert.dto.ConcertCreateResponse;
import com.ject.highfive.concert.exception.ConcertErrorCode;
import com.ject.highfive.concert.service.ConcertService;
import com.ject.highfive.global.exception.BusinessException;
import com.ject.highfive.global.security.SecurityConfig;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(controllers = ConcertController.class)
@Import(SecurityConfig.class)
class ConcertControllerTest {

  private static final String URL = "/api/v1/concerts";

  @Autowired MockMvc mockMvc;
  @MockitoBean ConcertService concertService;

  // 유효한 요청 본문에서 한 필드만 바꾼다. rawValue는 JSON 값 그대로 ("\"문자열\"", "3", "null", "[...]")
  private static String bodyWith(String field, String rawValue) {
    Map<String, String> fields = new LinkedHashMap<>();
    fields.put("bandId", "3");
    fields.put("title", "\"겨울 합동공연\"");
    fields.put("genres", "[\"ROCK\", \"INDIE\"]");
    fields.put("recruitTeamCount", "3");
    fields.put("performDate", "\"2999-12-20\"");
    fields.put("startTime", "\"19:00:00\"");
    fields.put("endTime", "\"21:00:00\"");
    fields.put("place", "\"홍대 롤링홀\"");
    fields.put("address", "\"서울 마포구 어울마당로 35\"");
    fields.put("region", "\"SEOUL\"");
    fields.put("rentalFee", "500000");
    fields.put("posterImageUrl", "null");
    if (field != null) {
      fields.put(field, rawValue);
    }
    return fields.entrySet().stream()
        .map(e -> "\"" + e.getKey() + "\": " + e.getValue())
        .collect(Collectors.joining(", ", "{", "}"));
  }

  private ResultActions postConcert(String body) throws Exception {
    return mockMvc.perform(
        post(URL).header("X-User-Id", "7").contentType(MediaType.APPLICATION_JSON).content(body));
  }

  @Test
  @DisplayName("생성에 성공하면 201과 concertId를 돌려주고, 헤더의 사용자 ID를 서비스에 넘긴다")
  void createReturns201() throws Exception {
    given(concertService.create(eq(7L), any())).willReturn(new ConcertCreateResponse(12L));

    postConcert(bodyWith(null, null))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value(201))
        .andExpect(jsonPath("$.code").value("COMMON_000"))
        .andExpect(jsonPath("$.data.concertId").value(12));
    verify(concertService).create(eq(7L), any());
  }

  @Test
  @DisplayName("필수값이 모두 빠지면 COMMON_004와 네 필드의 오류를 돌려준다")
  void missingRequiredFieldsReturnFieldErrors() throws Exception {
    postConcert("{}")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.code").value("COMMON_004"))
        .andExpect(
            jsonPath("$.errors[*].field")
                .value(containsInAnyOrder("bandId", "title", "genres", "recruitTeamCount")));
    verifyNoInteractions(concertService);
  }

  static Stream<Arguments> invalidFields() {
    return Stream.of(
        Arguments.of("title", "\"   \""),
        Arguments.of("title", "\"" + "가".repeat(31) + "\""),
        Arguments.of("genres", "[]"),
        Arguments.of("recruitTeamCount", "0"),
        Arguments.of("rentalFee", "-1"),
        Arguments.of("place", "\"" + "가".repeat(101) + "\""),
        Arguments.of("address", "\"" + "가".repeat(201) + "\""),
        Arguments.of("region", "\"" + "A".repeat(51) + "\""),
        Arguments.of("posterImageUrl", "\"" + "a".repeat(501) + "\""));
  }

  @ParameterizedTest(name = "{0} = {1}")
  @MethodSource("invalidFields")
  @DisplayName("한도를 넘거나 형식이 틀린 필드는 COMMON_004와 그 필드 이름을 돌려준다")
  void invalidFieldRejected(String field, String rawValue) throws Exception {
    postConcert(bodyWith(field, rawValue))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("COMMON_004"))
        .andExpect(jsonPath("$.errors[0].field").value(field));
    verifyNoInteractions(concertService);
  }

  @Test
  @DisplayName("한도에 딱 맞는 값(제목 30자, 모집 팀 1, 대관비 0)은 통과한다")
  void boundaryValuesAccepted() throws Exception {
    given(concertService.create(eq(7L), any())).willReturn(new ConcertCreateResponse(12L));
    String body =
        bodyWith("title", "\"" + "가".repeat(30) + "\"")
            .replace("\"recruitTeamCount\": 3", "\"recruitTeamCount\": 1")
            .replace("\"rentalFee\": 500000", "\"rentalFee\": 0");

    postConcert(body).andExpect(status().isCreated());
  }

  @Test
  @DisplayName("목록에 없는 장르 코드는 500이 아니라 400 COMMON_001")
  void unknownGenreIsBadRequest() throws Exception {
    postConcert(bodyWith("genres", "[\"UNKNOWN\"]"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("COMMON_001"));
    verifyNoInteractions(concertService);
  }

  @Test
  @DisplayName("장르에 null 원소가 있으면 COMMON_004")
  void nullGenreElementRejected() throws Exception {
    postConcert(bodyWith("genres", "[\"ROCK\", null]"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("COMMON_001"));
    verifyNoInteractions(concertService);
  }

  @Test
  @DisplayName("X-User-Id가 숫자가 아니면 500이 아니라 400 COMMON_004")
  void nonNumericUserIdHeaderIsBadRequest() throws Exception {
    mockMvc
        .perform(
            post(URL)
                .header("X-User-Id", "abc")
                .contentType(MediaType.APPLICATION_JSON)
                .content(bodyWith(null, null)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("COMMON_004"));
    verifyNoInteractions(concertService);
  }

  @Test
  @DisplayName("서비스의 업무 오류는 그 코드와 상태로 내려간다")
  void businessErrorPassesThrough() throws Exception {
    given(concertService.create(eq(7L), any()))
        .willThrow(new BusinessException(ConcertErrorCode.PERFORM_TIME_INCOMPLETE));

    postConcert(bodyWith(null, null))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.code").value("CONCERT_010"));
  }
}
