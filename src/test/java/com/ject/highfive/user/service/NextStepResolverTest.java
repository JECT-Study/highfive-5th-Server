package com.ject.highfive.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.ject.highfive.user.dto.NextStep;
import com.ject.highfive.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NextStepResolverTest {

  private final TermAgreementChecker termAgreementChecker = mock(TermAgreementChecker.class);
  private final ActiveBandChecker activeBandChecker = mock(ActiveBandChecker.class);
  private final NextStepResolver resolver =
      new NextStepResolver(termAgreementChecker, activeBandChecker);

  @Test
  @DisplayName("약관 미동의 → TERMS, 닉네임 없음 → PROFILE, 밴드 없음 → BAND_START, 그 외 → HOME 순으로 판단한다")
  void resolvesInOrder() {
    User user = User.signUp();
    given(termAgreementChecker.hasAgreedRequiredTerms(user)).willReturn(false);
    assertThat(resolver.resolve(user)).isEqualTo(NextStep.TERMS);

    given(termAgreementChecker.hasAgreedRequiredTerms(user)).willReturn(true);
    assertThat(resolver.resolve(user)).isEqualTo(NextStep.PROFILE);

    user.updateProfile("두현", null);
    given(activeBandChecker.hasActiveBand(user)).willReturn(false);
    assertThat(resolver.resolve(user)).isEqualTo(NextStep.BAND_START);

    given(activeBandChecker.hasActiveBand(user)).willReturn(true);
    assertThat(resolver.resolve(user)).isEqualTo(NextStep.HOME);
  }

  @Test
  @DisplayName("프로필까지 마친 회원도 필수 약관이 개정돼 미동의 상태면 다시 TERMS (결정 #37)")
  void activeUserGoesBackToTermsAfterRevision() {
    User user = User.signUp();
    user.updateProfile("두현", null);
    given(termAgreementChecker.hasAgreedRequiredTerms(user)).willReturn(false);

    assertThat(resolver.resolve(user)).isEqualTo(NextStep.TERMS);
  }
}
