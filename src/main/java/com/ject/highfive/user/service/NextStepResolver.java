package com.ject.highfive.user.service;

import com.ject.highfive.user.dto.NextStep;
import com.ject.highfive.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// 로그인·내 정보 조회·약관 동의·프로필 저장 응답의 nextStep. 규칙(위에서부터 먼저 맞는 것):
// 필수 약관 미동의 → TERMS / 닉네임 없음 → PROFILE / ACTIVE 밴드 없음 → BAND_START / 그 외 → HOME
@Component
@RequiredArgsConstructor
public class NextStepResolver {

  private final TermAgreementChecker termAgreementChecker;
  private final ActiveBandChecker activeBandChecker;

  public NextStep resolve(User user) {
    if (!termAgreementChecker.hasAgreedRequiredTerms(user)) {
      return NextStep.TERMS;
    }
    if (user.getNickname() == null) {
      return NextStep.PROFILE;
    }
    if (!activeBandChecker.hasActiveBand(user)) {
      return NextStep.BAND_START;
    }
    return NextStep.HOME;
  }
}
