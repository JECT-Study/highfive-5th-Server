package com.ject.highfive.upload.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

// 허용 형식·최대 용량은 결정되면 검증을 추가한다 (JECT-214)
public record PresignedUrlRequest(
    @NotNull(message = "업로드 용도는 필수입니다.") UploadPurpose purpose,
    @Schema(description = "파일 형식", example = "image/jpeg") @NotBlank(message = "파일 형식은 필수입니다.")
        String contentType,
    @Schema(description = "파일 크기 (바이트)", example = "1048576")
        @NotNull(message = "파일 크기는 필수입니다.")
        @Positive(message = "파일 크기는 1바이트 이상이어야 합니다.")
        Long fileSize) {}
