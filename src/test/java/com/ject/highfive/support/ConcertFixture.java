package com.ject.highfive.support;

import com.ject.highfive.concert.entity.Concert;
import com.ject.highfive.concert.entity.Genre;
import java.time.LocalDate;
import java.util.List;

// 여러 테스트가 같은 기본 공연을 쓴다: 주최 밴드 3, 공연 2999-12-20, 장르 ROCK
public final class ConcertFixture {

  public static final Long HOST_BAND_ID = 3L;

  private ConcertFixture() {}

  public static Concert concert() {
    return Concert.builder()
        .hostBandId(HOST_BAND_ID)
        .createdBy(1L)
        .title("겨울 합동공연")
        .recruitTeamCount(3)
        .performDate(LocalDate.of(2999, 12, 20))
        .genres(List.of(Genre.ROCK))
        .build();
  }
}
