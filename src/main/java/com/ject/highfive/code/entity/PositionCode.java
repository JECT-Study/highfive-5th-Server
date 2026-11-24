package com.ject.highfive.code.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

// 연주 포지션 선택지
@Getter
@RequiredArgsConstructor
public enum PositionCode {
  VOCAL("보컬"),
  GUITAR("기타"),
  BASS("베이스"),
  DRUM("드럼"),
  KEYBOARD("키보드"),
  // 고르면 직접 입력 값(custom_name)을 함께 받는다
  CUSTOM("직접입력");

  private final String displayName;

  public boolean requiresCustomName() {
    return this == CUSTOM;
  }
}
