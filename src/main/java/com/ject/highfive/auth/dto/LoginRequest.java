package com.ject.highfive.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @Schema(description = "소셜 SDK에서 받은 OIDC ID 토큰 (카카오 idToken / 구글 idToken / 애플 identityToken)")
        @NotBlank(message = "토큰은 필수입니다.")
        String token) {}
