package com.ject.highfive.global.exception;

import com.ject.highfive.global.common.response.ApiResponse;
import com.ject.highfive.global.common.response.FieldErrorResponse;
import java.util.List;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.ErrorResponse;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final String REQUIRED_REASON = "필수 값입니다.";
  private static final String TYPE_MISMATCH_REASON = "형식이 올바르지 않습니다.";

  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
    return toResponse(e.getErrorCode());
  }

  // @RequestBody @Valid 검증 실패
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValid(
      MethodArgumentNotValidException e) {
    List<FieldErrorResponse> errors =
        e.getBindingResult().getAllErrors().stream().map(this::toFieldError).toList();
    return toResponse(CommonErrorCode.INVALID_INPUT, errors);
  }

  // @RequestParam·@PathVariable 등에 붙인 제약 조건 검증 실패
  @ExceptionHandler(HandlerMethodValidationException.class)
  public ResponseEntity<ApiResponse<Void>> handleHandlerMethodValidation(
      HandlerMethodValidationException e) {
    List<FieldErrorResponse> errors =
        e.getParameterValidationResults().stream().flatMap(this::toFieldErrors).toList();
    return toResponse(CommonErrorCode.INVALID_INPUT, errors);
  }

  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ResponseEntity<ApiResponse<Void>> handleMissingParameter(
      MissingServletRequestParameterException e) {
    return toResponse(
        CommonErrorCode.INVALID_INPUT,
        List.of(new FieldErrorResponse(e.getParameterName(), REQUIRED_REASON)));
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(
      MethodArgumentTypeMismatchException e) {
    return toResponse(
        CommonErrorCode.INVALID_INPUT,
        List.of(new FieldErrorResponse(e.getName(), TYPE_MISMATCH_REASON)));
  }

  // JSON 형식 오류, 본문 누락, enum에 없는 값 등 요청 본문을 읽지 못한 경우
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ApiResponse<Void>> handleNotReadable(HttpMessageNotReadableException e) {
    return toResponse(CommonErrorCode.BAD_REQUEST);
  }

  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<ApiResponse<Void>> handleNoResource(NoResourceFoundException e) {
    return toResponse(CommonErrorCode.NOT_FOUND);
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(
      HttpRequestMethodNotSupportedException e) {
    return toResponse(CommonErrorCode.METHOD_NOT_ALLOWED);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
    // 위에서 따로 처리하지 않은 Spring MVC 예외(415 등)는 원래 상태 코드를 유지한다
    if (e instanceof ErrorResponse errorResponse
        && errorResponse.getStatusCode().is4xxClientError()) {
      CommonErrorCode errorCode = CommonErrorCode.BAD_REQUEST;
      ApiResponse<Void> body =
          new ApiResponse<>(
              errorResponse.getStatusCode().value(),
              errorCode.getCode(),
              errorCode.getMessage(),
              null,
              null);
      return ResponseEntity.status(errorResponse.getStatusCode()).body(body);
    }
    log.error("처리하지 못한 예외", e);
    return toResponse(CommonErrorCode.INTERNAL_SERVER_ERROR);
  }

  private ResponseEntity<ApiResponse<Void>> toResponse(ErrorCode errorCode) {
    return ResponseEntity.status(errorCode.getStatus()).body(ApiResponse.error(errorCode));
  }

  private ResponseEntity<ApiResponse<Void>> toResponse(
      ErrorCode errorCode, List<FieldErrorResponse> errors) {
    return ResponseEntity.status(errorCode.getStatus()).body(ApiResponse.error(errorCode, errors));
  }

  private FieldErrorResponse toFieldError(ObjectError error) {
    String field =
        error instanceof FieldError fieldError ? fieldError.getField() : error.getObjectName();
    return new FieldErrorResponse(field, error.getDefaultMessage());
  }

  private Stream<FieldErrorResponse> toFieldErrors(ParameterValidationResult result) {
    if (result instanceof ParameterErrors parameterErrors) {
      return parameterErrors.getAllErrors().stream().map(this::toFieldError);
    }
    String parameterName = result.getMethodParameter().getParameterName();
    return result.getResolvableErrors().stream()
        .map(error -> new FieldErrorResponse(parameterName, error.getDefaultMessage()));
  }
}
