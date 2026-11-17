package com.ject.highfive.global.exception;

import com.ject.highfive.global.common.response.ApiResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// GlobalExceptionHandlerTest에서만 쓰는 컨트롤러
@RestController
@RequestMapping("/test")
class ExceptionTestController {

  record TestRequest(@NotBlank(message = "이름은 필수입니다.") String name) {}

  record TestData(String value) {}

  // @Validated 서비스처럼 메서드 파라미터 검증이 실패하는 상황을 만들기 위한 클래스
  static class TestService {
    void save(@Valid TestRequest request) {}
  }

  @Getter
  @RequiredArgsConstructor
  enum TestErrorCode implements ErrorCode {
    CONFLICT(HttpStatus.CONFLICT, "TEST_001", "테스트 충돌입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
  }

  @GetMapping("/success")
  ApiResponse<TestData> success() {
    return ApiResponse.success(new TestData(null));
  }

  @PostMapping("/created")
  @ResponseStatus(HttpStatus.CREATED)
  ApiResponse<TestData> created() {
    return ApiResponse.success(new TestData("new"));
  }

  @PostMapping("/accepted")
  ResponseEntity<ApiResponse<Map<String, Object>>> accepted() {
    return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.success());
  }

  @GetMapping("/empty")
  ApiResponse<Map<String, Object>> empty() {
    return ApiResponse.success();
  }

  @PostMapping("/body")
  ApiResponse<Map<String, Object>> body(@RequestBody @Valid TestRequest request) {
    return ApiResponse.success();
  }

  @GetMapping("/param")
  ApiResponse<Map<String, Object>> param(@RequestParam Integer value) {
    return ApiResponse.success();
  }

  @GetMapping("/constrained")
  ApiResponse<Map<String, Object>> constrained(
      @RequestParam @Max(value = 100, message = "100 이하여야 합니다.") Integer size) {
    return ApiResponse.success();
  }

  @GetMapping("/business")
  ApiResponse<Void> business() {
    throw new BusinessException(TestErrorCode.CONFLICT);
  }

  @GetMapping("/unexpected")
  ApiResponse<Void> unexpected() {
    throw new IllegalStateException("내부 정보가 담긴 메시지");
  }

  @GetMapping("/violation")
  ApiResponse<Void> violation() throws NoSuchMethodException {
    Method save = TestService.class.getDeclaredMethod("save", TestRequest.class);
    try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
      Set<ConstraintViolation<TestService>> violations =
          factory
              .getValidator()
              .forExecutables()
              .validateParameters(new TestService(), save, new Object[] {new TestRequest("")});
      throw new ConstraintViolationException(violations);
    }
  }
}
