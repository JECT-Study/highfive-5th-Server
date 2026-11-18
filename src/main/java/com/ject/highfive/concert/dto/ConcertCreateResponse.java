package com.ject.highfive.concert.dto;

import com.ject.highfive.concert.entity.Concert;

public record ConcertCreateResponse(Long concertId) {

  public static ConcertCreateResponse from(Concert concert) {
    return new ConcertCreateResponse(concert.getId());
  }
}
