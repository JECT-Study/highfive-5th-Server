package com.ject.highfive.support;

import com.ject.highfive.concert.entity.Concert;
import com.ject.highfive.recruit.entity.RecruitPost;
import com.ject.highfive.recruit.entity.TeamType;
import com.ject.highfive.recruit.entity.WantedCareerLevel;
import java.time.LocalDate;
import java.util.List;

// 여러 테스트가 같은 기본 공고를 쓴다: 작성자 10, 연주 기간 1~3년, 누구나 환영, 마감 2999-12-01
public final class RecruitPostFixture {

  private RecruitPostFixture() {}

  public static RecruitPost recruitPost(Concert concert) {
    return RecruitPost.builder()
        .concert(concert)
        .authorUserId(10L)
        .wantedCareerLevel(WantedCareerLevel.Y1_TO_3)
        .teamTypes(List.of(TeamType.ANYONE))
        .deadline(LocalDate.of(2999, 12, 1))
        .description("12월 홍대에서 함께할 팀을 찾습니다.")
        .build();
  }
}
