package com.ject.highfive.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// 닉네임 허용 문자, 장르·포지션 코드 목록은 결정되면 검증을 추가한다 (JECT-214)
public record ProfileUpdateRequest(
    @NotBlank(message = "닉네임은 필수입니다.") @Size(min = 2, max = 10, message = "닉네임은 2~10자여야 합니다.")
        String nickname,
    @Schema(description = "코드 목록 조회 API의 GENRE 코드") @NotBlank(message = "선호 장르는 필수입니다.")
        String genreCode,
    @Schema(description = "코드 목록 조회 API의 POSITION 코드") @NotBlank(message = "연주 포지션은 필수입니다.")
        String positionCode,
    @Schema(description = "업로드 URL 발급 API로 올린 이미지의 fileUrl. 사진을 건너뛰면 null", nullable = true)
        String profileImageUrl) {}
