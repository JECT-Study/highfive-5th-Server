package com.ject.highfive.user.dto;

import com.ject.highfive.user.entity.Term;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record TermListResponse(@Schema(description = "필수 약관 먼저, 그다음 선택 약관") List<TermItem> terms) {

  public static TermListResponse from(List<Term> terms) {
    return new TermListResponse(terms.stream().map(TermItem::from).toList());
  }

  public record TermItem(
      @Schema(description = "약관 ID (동의 저장 시 사용)") Long id,
      @Schema(description = "SERVICE(이용약관) / PRIVACY(개인정보 수집·이용) / AGE_OVER_14(만 14세 이상) …")
          String code,
      String title,
      String version,
      boolean required,
      @Schema(description = "약관 전문 URL (앱 내 웹뷰로 표시). 아직 없으면 null", nullable = true)
          String contentUrl) {

    public static TermItem from(Term term) {
      return new TermItem(
          term.getId(),
          term.getCode(),
          term.getTitle(),
          term.getVersion(),
          term.isRequired(),
          term.getContentUrl());
    }
  }
}
