package com.ject.highfive.code.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

// 선호 장르 선택지. 코드값은 공연 장르(concert.entity.Genre)와 같게 유지한다 (GenreCodeTest가 확인)
// TODO: 밴드·공고 필터도 같은 목록을 쓰는지 확정되면(결정 로그 #29) 한 곳으로 합친다
@Getter
@RequiredArgsConstructor
public enum GenreCode {
  ROCK("록"),
  INDIE("인디"),
  METAL("메탈"),
  PUNK("펑크"),
  POP("팝"),
  JAZZ("재즈"),
  // 고르면 직접 입력 값(custom_name)을 함께 받는다
  ETC("기타");

  private final String displayName;

  public boolean requiresCustomName() {
    return this == ETC;
  }
}
