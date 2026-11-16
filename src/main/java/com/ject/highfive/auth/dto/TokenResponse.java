package com.ject.highfive.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record TokenResponse(
    @Schema(description = "새 액세스 토큰 (30분)") String accessToken,
    @Schema(description = "새 리프레시 토큰 (14일). 앱은 기존 값을 이걸로 교체") String refreshToken) {}
