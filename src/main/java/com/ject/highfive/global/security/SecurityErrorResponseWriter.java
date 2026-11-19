package com.ject.highfive.global.security;

import com.ject.highfive.global.common.response.ApiResponse;
import com.ject.highfive.global.exception.ErrorCode;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import tools.jackson.databind.json.JsonMapper;

// Security 필터 단계의 401·403은 컨트롤러를 거치지 않으므로 공통 응답 형식으로 직접 쓴다
@RequiredArgsConstructor
class SecurityErrorResponseWriter {

  private final JsonMapper jsonMapper;

  void write(HttpServletResponse response, ErrorCode errorCode) throws IOException {
    response.setStatus(errorCode.getStatus().value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    jsonMapper.writeValue(response.getWriter(), ApiResponse.error(errorCode));
  }
}
