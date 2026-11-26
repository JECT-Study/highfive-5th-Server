package com.ject.highfive.user.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RequiredTermsStartupCheckTest {

  private final CurrentTerms currentTerms = mock(CurrentTerms.class);
  private final RequiredTermsStartupCheck check = new RequiredTermsStartupCheck(currentTerms);

  @Test
  @DisplayName("시행 중인 필수 약관이 없으면 서버 시작을 막는다 (필수 동의 없이 가입되는 것 방지)")
  void failsWithoutRequiredTerms() {
    given(currentTerms.requiredIds()).willReturn(List.of());

    assertThatThrownBy(() -> check.run(null)).isInstanceOf(IllegalStateException.class);
  }

  @Test
  @DisplayName("필수 약관이 있으면 그대로 시작한다")
  void passesWithRequiredTerms() {
    given(currentTerms.requiredIds()).willReturn(List.of(1L, 2L, 3L));

    assertThatCode(() -> check.run(null)).doesNotThrowAnyException();
  }
}
