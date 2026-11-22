package com.ject.highfive.user.service;

import com.ject.highfive.user.entity.User;
import com.ject.highfive.user.entity.UserStatus;
import org.springframework.stereotype.Component;

// 최신 필수 약관에 모두 동의했는지. nextStep(TERMS)과 프로필 저장(USER_003)이 쓴다
@Component
public class TermAgreementChecker {

  // TODO: 약관 동의 기록(user_term_agreements)으로 판단한다 (JECT-213, 약관 정책 PM 확정 후).
  //  지금은 약관 데이터가 없어 가입 중(PENDING)이면 아직 동의하지 않은 것으로 본다
  public boolean hasAgreedRequiredTerms(User user) {
    return user.getStatus() != UserStatus.PENDING;
  }
}
