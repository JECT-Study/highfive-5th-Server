package com.ject.highfive.concert.dummy;

import com.ject.highfive.concert.entity.Genre;
import com.ject.highfive.recruit.entity.WantedCareerLevel;
import java.time.LocalDateTime;
import java.util.List;

// 필드 이름은 ERD bands·band_genres와 맞춘다. memberCount = declared_member_count, region = region_code.
// careerLevel은 밴드 전용 enum이 없어 공고의 WantedCareerLevel을 빌려 쓴다(ANY는 넣지 않음). JECT-253에서 실제 Band로 교체
public record DummyBand(
    Long id,
    String name,
    String profileImageUrl,
    List<Genre> genres,
    int memberCount,
    String region,
    WantedCareerLevel careerLevel,
    LocalDateTime createdAt) {}
