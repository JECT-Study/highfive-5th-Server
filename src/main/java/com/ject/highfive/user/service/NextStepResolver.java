package com.ject.highfive.user.service;

import com.ject.highfive.user.dto.NextStep;
import com.ject.highfive.user.entity.User;
import com.ject.highfive.user.entity.UserStatus;
import org.springframework.stereotype.Component;

// 로그인·내 정보 조회 응답의 nextStep. 규칙(위에서부터 먼저 맞는 것):
// 필수 약관 미동의 → TERMS / 닉네임 없음 → PROFILE / ACTIVE 밴드 없음 → BAND_START / 그 외 → HOME
@Component
public class NextStepResolver {

  // TODO: 약관 동의(JECT-213)·닉네임(JECT-214)·소속 밴드(JECT-71) 확인을 붙인다.
  //  지금은 그 데이터가 없어 가입 중(PENDING)이면 약관부터, 가입을 끝냈으면 밴드 시작으로 보낸다
  public NextStep resolve(User user) {
    return user.getStatus() == UserStatus.PENDING ? NextStep.TERMS : NextStep.BAND_START;
  }
}
