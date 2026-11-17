package com.ject.highfive.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record TokenRefreshRequest(
    @Schema(description = "앱에 저장된 리프레시 토큰") @NotBlank(message = "리프레시 토큰은 필수입니다.")
        String refreshToken) {}
