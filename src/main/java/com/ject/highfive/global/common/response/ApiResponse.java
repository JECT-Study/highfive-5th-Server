package com.ject.highfive.global.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ject.highfive.global.exception.ErrorCode;
import java.util.List;
import java.util.Map;

// 모든 API의 공통 응답 형식.
// 성공이면 data가, 입력값 검증 실패면 errors가 채워지고 나머지 실패는 둘 다 응답에서 빠진다.
// (NON_NULL은 이 봉투에만 적용되고 data 안의 DTO 필드는 null이어도 그대로 내려간다)
// status는 응답 직전에 ApiResponseStatusAdvice가 실제 HTTP 상태 코드로 맞춘다
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
    int status, String code, String message, T data, List<FieldErrorResponse> errors) {

  private static final int SUCCESS_STATUS = 200;
  private static final String SUCCESS_CODE = "COMMON_000";
  private static final String SUCCESS_MESSAGE = "요청이 성공했습니다.";

  public static <T> ApiResponse<T> success(T data) {
    return new ApiResponse<>(SUCCESS_STATUS, SUCCESS_CODE, SUCCESS_MESSAGE, data, null);
  }

  // 돌려줄 결과가 없는 성공 응답. 컨벤션대로 data는 빈 객체 {}
  public static ApiResponse<Map<String, Object>> success() {
    return success(Map.of());
  }

  public static ApiResponse<Void> error(ErrorCode errorCode) {
    return new ApiResponse<>(
        errorCode.getStatus().value(), errorCode.getCode(), errorCode.getMessage(), null, null);
  }

  public static ApiResponse<Void> error(ErrorCode errorCode, List<FieldErrorResponse> errors) {
    return new ApiResponse<>(
        errorCode.getStatus().value(), errorCode.getCode(), errorCode.getMessage(), null, errors);
  }

  ApiResponse<T> withStatus(int status) {
    if (this.status == status) {
      return this;
    }
    return new ApiResponse<>(status, code, message, data, errors);
  }
}
