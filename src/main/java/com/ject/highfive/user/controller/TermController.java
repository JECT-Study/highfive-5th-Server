package com.ject.highfive.user.controller;

import com.ject.highfive.global.common.response.ApiResponse;
import com.ject.highfive.user.dto.TermListResponse;
import com.ject.highfive.user.service.TermService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "사용자", description = "내 정보·약관 동의·개인 프로필")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/terms")
public class TermController {

  private final TermService termService;

  @Operation(
      summary = "약관 목록 조회",
      description =
          "현재 시행 중인 약관(종류별 최신 버전) 목록을 필수 → 선택 순으로 돌려준다. "
              + "동의 저장에는 여기서 받은 id를 그대로 쓴다.\n\n"
              + "contentUrl은 약관 전문 주소가 정해지기 전까지 null일 수 있다.\n\n"
              + "에러: AUTH_001, AUTH_002, AUTH_003")
  @GetMapping
  public ApiResponse<TermListResponse> getTerms() {
    return ApiResponse.success(termService.getTerms());
  }
}
