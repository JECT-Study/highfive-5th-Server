package com.ject.highfive.user.dto;

// 로그인·내 정보 응답에서 앱이 이동할 화면. 위에서부터 먼저 맞는 것 하나로 정한다
public enum NextStep {
  // 최신 필수 약관 미동의
  TERMS,
  // 닉네임 없음 (프로필 미완료)
  PROFILE,
  // ACTIVE 상태로 속한 밴드 없음
  BAND_START,
  HOME
}
