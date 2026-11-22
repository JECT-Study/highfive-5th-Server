package com.ject.highfive.recruit.dto;

import com.ject.highfive.concert.entity.Genre;
import com.ject.highfive.recruit.entity.WantedCareerLevel;
import java.util.List;

// 공고 목록(2.5) 검색 조건. 컨트롤러가 쿼리 파라미터로 만든다. 모든 값은 없을 수 있다
public record RecruitPostSearchRequest(
    String region, List<Genre> genres, List<WantedCareerLevel> careerLevels, String keyword) {}
