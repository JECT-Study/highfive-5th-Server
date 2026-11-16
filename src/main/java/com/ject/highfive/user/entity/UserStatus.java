package com.ject.highfive.user.entity;

public enum UserStatus {
  // 소셜 로그인만 하고 약관·프로필을 아직 끝내지 않음
  PENDING,
  ACTIVE,
  // 탈퇴 (행은 남기는 soft delete)
  WITHDRAWN
}
