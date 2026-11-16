package com.ject.highfive.upload.controller;

import com.ject.highfive.global.common.response.ApiResponse;
import com.ject.highfive.upload.dto.PresignedUrlRequest;
import com.ject.highfive.upload.dto.PresignedUrlResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// TODO: 업로드 방식·용량이 확정되면 S3 presigned URL 발급으로 교체 (JECT-214)
@Tag(name = "공통", description = "코드 목록·이미지 업로드")
@RestController
@RequestMapping("/api/v1/uploads")
public class UploadController {

  @Operation(
      summary = "이미지 업로드 URL 발급",
      description =
          "앱이 S3에 직접 올릴 임시 업로드 주소(presigned URL)를 발급한다. 온보딩에서는 purpose = USER_PROFILE만 쓴다.\n\n"
              + "에러: COMMON_004(형식·용량·필드 누락)\n\n"
              + "**현재는 가짜 응답 (실제 업로드 불가)**")
  @PostMapping("/presigned-url")
  public ApiResponse<PresignedUrlResponse> issuePresignedUrl(
      @RequestBody @Valid PresignedUrlRequest request) {
    PresignedUrlResponse response =
        new PresignedUrlResponse(
            "https://example.com/mock-upload-url",
            "https://example.com/users/1/profile/mock.jpg",
            LocalDateTime.now().plusMinutes(5).withNano(0));
    return ApiResponse.success(response);
  }
}
