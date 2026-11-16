package com.ject.highfive.global.exception;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ject.highfive.global.security.SecurityConfig;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ExceptionTestController.class)
@Import(SecurityConfig.class)
class GlobalExceptionHandlerTest {

  @Autowired private MockMvc mockMvc;

  @Test
  @DisplayName("성공 응답은 data를 담고 errors는 내보내지 않는다")
  void successResponseHasDataWithoutErrors() throws Exception {
    mockMvc
        .perform(get("/test/success"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(200))
        .andExpect(jsonPath("$.code").value("COMMON_000"))
        .andExpect(jsonPath("$.message").value("요청이 성공했습니다."))
        // data 안의 값 없는 필드는 생략하지 않고 null로 내려간다
        .andExpect(jsonPath("$.data.value").value(nullValue()))
        .andExpect(jsonPath("$.errors").doesNotExist());
  }

  @Test
  @DisplayName("결과가 없는 성공 응답의 data는 빈 객체다")
  void emptySuccessResponseHasEmptyData() throws Exception {
    mockMvc
        .perform(get("/test/empty"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data").isMap())
        .andExpect(jsonPath("$.data").isEmpty());
  }

  @Test
  @DisplayName("요청 본문 검증 실패는 COMMON 004와 필드별 사유를 돌려준다")
  void invalidBodyReturnsFieldErrors() throws Exception {
    mockMvc
        .perform(
            post("/test/body").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.code").value("COMMON_004"))
        .andExpect(jsonPath("$.errors[0].field").value("name"))
        .andExpect(jsonPath("$.errors[0].reason").value("이름은 필수입니다."))
        .andExpect(jsonPath("$.data").doesNotExist());
  }

  @Test
  @DisplayName("파라미터 제약 조건 위반은 COMMON 004다")
  void constraintViolationOnParamIsInvalidInput() throws Exception {
    mockMvc
        .perform(get("/test/constrained").param("size", "101"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("COMMON_004"))
        .andExpect(jsonPath("$.errors[0].field").value("size"))
        .andExpect(jsonPath("$.errors[0].reason").value("100 이하여야 합니다."));
  }

  @Test
  @DisplayName("필수 파라미터 누락은 COMMON 004다")
  void missingParamIsInvalidInput() throws Exception {
    mockMvc
        .perform(get("/test/param"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("COMMON_004"))
        .andExpect(jsonPath("$.errors[0].field").value("value"));
  }

  @Test
  @DisplayName("파라미터 타입 불일치는 COMMON 004다")
  void typeMismatchIsInvalidInput() throws Exception {
    mockMvc
        .perform(get("/test/param").param("value", "abc"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("COMMON_004"))
        .andExpect(jsonPath("$.errors[0].field").value("value"));
  }

  @Test
  @DisplayName("읽을 수 없는 요청 본문은 COMMON 001이다")
  void unreadableBodyIsBadRequest() throws Exception {
    mockMvc
        .perform(post("/test/body").contentType(MediaType.APPLICATION_JSON).content("{"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("COMMON_001"));
  }

  @Test
  @DisplayName("지원하지 않는 Content Type은 원래 상태 코드를 유지한다")
  void unsupportedMediaTypeKeepsStatus() throws Exception {
    mockMvc
        .perform(post("/test/body").contentType(MediaType.TEXT_PLAIN).content("hello"))
        .andExpect(status().isUnsupportedMediaType())
        .andExpect(jsonPath("$.status").value(415))
        .andExpect(jsonPath("$.code").value("COMMON_001"));
  }

  @Test
  @DisplayName("없는 경로는 COMMON 002다")
  void unknownPathIsNotFound() throws Exception {
    mockMvc
        .perform(get("/test/nowhere"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("COMMON_002"));
  }

  @Test
  @DisplayName("지원하지 않는 메서드는 COMMON 003이다")
  void unsupportedMethodIsMethodNotAllowed() throws Exception {
    mockMvc
        .perform(delete("/test/success"))
        .andExpect(status().isMethodNotAllowed())
        .andExpect(jsonPath("$.code").value("COMMON_003"));
  }

  @Test
  @DisplayName("BusinessException은 에러 코드의 상태와 코드를 그대로 쓴다")
  void businessExceptionUsesItsErrorCode() throws Exception {
    mockMvc
        .perform(get("/test/business"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.status").value(409))
        .andExpect(jsonPath("$.code").value("TEST_001"))
        .andExpect(jsonPath("$.message").value("테스트 충돌입니다."))
        .andExpect(jsonPath("$.errors").doesNotExist());
  }

  @Test
  @DisplayName("예상하지 못한 예외는 COMMON 005이고 내부 메시지를 노출하지 않는다")
  void unexpectedExceptionHidesInternalMessage() throws Exception {
    mockMvc
        .perform(get("/test/unexpected"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.code").value("COMMON_005"))
        .andExpect(content().string(Matchers.not(Matchers.containsString("내부 정보"))));
  }
}
