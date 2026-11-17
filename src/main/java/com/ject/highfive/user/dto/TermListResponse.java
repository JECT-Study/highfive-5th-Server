package com.ject.highfive.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record TermListResponse(@Schema(description = "필수 약관 먼저, 그다음 선택 약관") List<TermItem> terms) {

  public record TermItem(
      @Schema(description = "약관 ID (동의 저장 시 사용)") Long id,
      @Schema(description = "SERVICE / PRIVACY / MARKETING …") String code,
      String title,
      String version,
      boolean required,
      @Schema(description = "약관 전문 URL (앱 내 웹뷰로 표시)") String contentUrl) {}
}
