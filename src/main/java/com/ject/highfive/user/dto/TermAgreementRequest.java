package com.ject.highfive.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record TermAgreementRequest(
    @Schema(description = "약관 목록 조회에서 받은 모든 약관의 동의 여부") @NotEmpty(message = "약관 동의 목록은 필수입니다.")
        List<@NotNull(message = "약관 동의 항목이 비어 있습니다.") @Valid Agreement> agreements) {

  public record Agreement(
      @NotNull(message = "약관 ID는 필수입니다.") Long termId,
      @Schema(description = "동의 여부 (선택 약관은 false 가능)") @NotNull(message = "동의 여부는 필수입니다.")
          Boolean agreed) {}
}
