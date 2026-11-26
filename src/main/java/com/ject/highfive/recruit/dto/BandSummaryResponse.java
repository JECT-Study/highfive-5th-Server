package com.ject.highfive.recruit.dto;

import com.ject.highfive.concert.dummy.DummyBand;
import com.ject.highfive.concert.entity.Genre;
import com.ject.highfive.recruit.entity.WantedCareerLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

// 팀 탭 카드 필드는 명세 예시대로. Figma 팀 탭 확인 후 맞춘다(명세 [합의 필요])
public record BandSummaryResponse(
    Long bandId,
    String name,
    String profileImageUrl,
    List<Genre> genres,
    @Schema(description = "밴드 연주 기간. ANY는 오지 않는다") WantedCareerLevel careerLevel,
    @Schema(description = "밴드 지역 코드") String region,
    @Schema(description = "밴드를 만들 때 입력한 팀원 수") int memberCount) {

  public static BandSummaryResponse from(DummyBand band) {
    return new BandSummaryResponse(
        band.id(),
        band.name(),
        band.profileImageUrl(),
        band.genres(),
        band.careerLevel(),
        band.region(),
        band.memberCount());
  }
}
