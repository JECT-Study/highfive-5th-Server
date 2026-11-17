package com.ject.highfive.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record NicknameAvailabilityResponse(
    boolean available,
    @Schema(
            description = "사용할 수 없는 이유. 사용 가능하면 null",
            nullable = true,
            allowableValues = {"INVALID_LENGTH", "INVALID_CHARACTER", "DUPLICATED"})
        String reason) {}
