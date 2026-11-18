package com.ject.highfive.recruit.dto;

import com.ject.highfive.concert.entity.Concert;
import com.ject.highfive.recruit.entity.RecruitPost;
import com.ject.highfive.recruit.entity.TeamType;
import com.ject.highfive.recruit.entity.WantedCareerLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

// 2.1 작성, 2.2 수정(220) 공용. 마감일이 오늘 이전인지는 RECRUIT_003으로 돌려줘야 해서 서비스에서 본다
public record RecruitPostRequest(
    @Schema(description = "원하는 팀의 연주 기간", example = "Y1_TO_3")
        @NotNull(message = "모집 팀 연주 기간은 필수입니다.")
        WantedCareerLevel wantedCareerLevel,
    @Schema(description = "이런 팀이면 좋아요 (중복은 하나로 합침)", example = "[\"FIRST_PERFORMANCE\"]")
        @NotEmpty(message = "모집 팀 유형을 하나 이상 골라 주세요.")
        List<@NotNull(message = "모집 팀 유형이 비어 있습니다.") TeamType> teamTypes,
    @Schema(description = "공고 마감일 (그날 23:59:59까지 모집)", example = "2026-12-01")
        @NotNull(message = "공고 마감일은 필수입니다.")
        LocalDate deadline,
    @Schema(description = "공고 소개", example = "12월 홍대에서 함께할 팀을 찾습니다.")
        @NotBlank(message = "공고 소개는 필수입니다.")
        @Size(max = 500, message = "공고 소개는 500자 이하여야 합니다.")
        String description) {

  public RecruitPost toEntity(Concert concert, Long authorUserId) {
    return RecruitPost.builder()
        .concert(concert)
        .authorUserId(authorUserId)
        .wantedCareerLevel(wantedCareerLevel)
        .teamTypes(teamTypes)
        .deadline(deadline)
        .description(description)
        .build();
  }
}
