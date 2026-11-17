package com.ject.highfive.global.common.response;

import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

// 응답 본문의 status를 실제 HTTP 상태 코드와 항상 같게 맞춘다.
// 컨트롤러는 HTTP 상태만 정하면 된다 (@ResponseStatus(HttpStatus.CREATED) 또는 ResponseEntity.status(...))
@RestControllerAdvice
public class ApiResponseStatusAdvice implements ResponseBodyAdvice<Object> {

  @Override
  public boolean supports(
      MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
    return true;
  }

  @Override
  public Object beforeBodyWrite(
      Object body,
      MethodParameter returnType,
      MediaType selectedContentType,
      Class<? extends HttpMessageConverter<?>> selectedConverterType,
      ServerHttpRequest request,
      ServerHttpResponse response) {
    if (body instanceof ApiResponse<?> apiResponse
        && response instanceof ServletServerHttpResponse servletResponse) {
      return apiResponse.withStatus(servletResponse.getServletResponse().getStatus());
    }
    return body;
  }
}
