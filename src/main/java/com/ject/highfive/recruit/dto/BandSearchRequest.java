package com.ject.highfive.recruit.dto;

import com.ject.highfive.concert.entity.Genre;
import com.ject.highfive.recruit.entity.WantedCareerLevel;
import java.util.List;

// 팀 검색(2.6) 조건. 컨트롤러가 쿼리 파라미터로 만든다. 모든 값은 없을 수 있다
public record BandSearchRequest(
    String keyword, String region, List<Genre> genres, List<WantedCareerLevel> careerLevels) {}
