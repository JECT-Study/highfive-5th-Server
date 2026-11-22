package com.ject.highfive.user.entity;

import com.ject.highfive.global.exception.AuthErrorCode;
import com.ject.highfive.global.exception.BusinessException;
import java.util.Arrays;

public enum SocialProvider {
  KAKAO,
  GOOGLE,
  // iOS 앱에서만 제공한다 (Android는 카카오·구글만)
  APPLE;

  // 로그인 API 경로의 kakao·google·apple(소문자)를 읽는다. 그 밖의 값은 AUTH_007
  public static SocialProvider fromPath(String value) {
    return Arrays.stream(values())
        .filter(provider -> provider.name().toLowerCase().equals(value))
        .findFirst()
        .orElseThrow(() -> new BusinessException(AuthErrorCode.UNSUPPORTED_PROVIDER));
  }
}
