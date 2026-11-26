package com.ject.highfive.user.service;

import com.ject.highfive.user.entity.User;
import com.ject.highfive.user.repository.UserTermAgreementRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// 지금 시행 중인 필수 약관에 모두 동의했는지. nextStep(TERMS)과 프로필 저장(USER_003)이 쓴다.
// 필수 약관이 개정되면(새 버전 시행) 새 버전에는 동의 기록이 없으니 기존 회원도 다시 TERMS가 된다 (결정 #37)
@Component
@RequiredArgsConstructor
public class TermAgreementChecker {

  private final CurrentTerms currentTerms;
  private final UserTermAgreementRepository userTermAgreementRepository;

  public boolean hasAgreedRequiredTerms(User user) {
    List<Long> requiredIds = currentTerms.requiredIds();
    if (requiredIds.isEmpty()) {
      return true;
    }
    long agreed =
        userTermAgreementRepository.countByUserIdAndTermIdInAndAgreedTrue(
            user.getId(), requiredIds);
    return agreed == requiredIds.size();
  }
}
