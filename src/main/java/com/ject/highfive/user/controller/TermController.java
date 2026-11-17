package com.ject.highfive.user.controller;

import com.ject.highfive.global.common.response.ApiResponse;
import com.ject.highfive.user.dto.TermListResponse;
import com.ject.highfive.user.dto.TermListResponse.TermItem;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// TODO: 가짜 응답을 실제 구현으로 교체 (JECT-213)
@Tag(name = "사용자", description = "내 정보·약관 동의·개인 프로필")
@RestController
@RequestMapping("/api/v1/terms")
public class TermController {

  @Operation(
      summary = "약관 목록 조회",
      description =
          "현재 시행 중인 최신 버전 약관 목록을 필수 → 선택 순으로 돌려준다.\n\n"
              + "에러: AUTH_001, AUTH_002, AUTH_003\n\n"
              + "**현재는 가짜 응답 (약관 내용 미정)**")
  @GetMapping
  public ApiResponse<TermListResponse> getTerms() {
    List<TermItem> terms =
        List.of(
            new TermItem(
                1L, "SERVICE", "서비스 이용약관", "1.0", true, "https://example.com/terms/service"),
            new TermItem(
                2L, "PRIVACY", "개인정보 수집·이용 동의", "1.0", true, "https://example.com/terms/privacy"),
            new TermItem(
                3L,
                "MARKETING",
                "마케팅 정보 수신 동의",
                "1.0",
                false,
                "https://example.com/terms/marketing"));
    return ApiResponse.success(new TermListResponse(terms));
  }
}
