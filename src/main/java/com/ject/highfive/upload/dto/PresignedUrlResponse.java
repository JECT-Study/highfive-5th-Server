package com.ject.highfive.upload.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record PresignedUrlResponse(
    @Schema(description = "앱이 파일을 PUT으로 올릴 주소 (Authorization 헤더 없이, Content-Type은 요청 때와 같게)")
        String uploadUrl,
    @Schema(description = "업로드 완료 후 프로필 저장 API에 넣을 주소") String fileUrl,
    @Schema(description = "uploadUrl 만료 시각") LocalDateTime expiresAt) {}
