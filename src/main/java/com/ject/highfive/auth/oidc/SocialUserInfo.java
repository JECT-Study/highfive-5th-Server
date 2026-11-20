package com.ject.highfive.auth.oidc;

// 검증을 통과한 소셜 ID 토큰에서 꺼낸 값. email은 소셜·동의 여부에 따라 없을 수 있다
public record SocialUserInfo(String providerUserId, String email) {}
