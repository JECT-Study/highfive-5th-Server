package com.ject.highfive.user.dto;

import com.ject.highfive.user.entity.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;

public record MyInfoResponse(
    Long id,
    UserStatus status,
    @Schema(description = "프로필 생성 전에는 null", nullable = true) String nickname,
    @Schema(description = "없으면 null (앱이 기본 이미지 표시)", nullable = true) String profileImageUrl,
    @Schema(description = "선호 장르 코드. 프로필 생성 전에는 null", nullable = true) String genreCode,
    @Schema(description = "연주 포지션 코드. 프로필 생성 전에는 null", nullable = true) String positionCode,
    @Schema(description = "ACTIVE 상태로 속한 밴드가 있는지") boolean hasBand,
    NextStep nextStep) {}
