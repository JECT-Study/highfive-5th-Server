package com.ject.highfive.user.service;

import com.ject.highfive.user.entity.User;
import com.ject.highfive.user.repository.UserTermAgreementRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

// 지금 시행 중인 필수 약관에 모두 동의했는지. nextStep(TERMS)과 프로필 저장(USER_003)이 쓴다.
// 필수 약관이 개정되면(새 버전 시행) 새 버전에는 동의 기록이 없으니 기존 회원도 다시 TERMS가 된다 (결정 #37)
@Slf4j
@Component
@RequiredArgsConstructor
public class TermAgreementChecker {

  private final CurrentTerms currentTerms;
  private final UserTermAgreementRepository userTermAgreementRepository;

  public boolean hasAgreedRequiredTerms(User user) {
    List<Long> requiredIds = currentTerms.requiredIds();
    if (requiredIds.isEmpty()) {
      // 약관 데이터가 없는 로컬(Flyway 끔)·테스트 DB에서만 생긴다. dev·운영은 시작할 때
      // RequiredTermsStartupCheck가 막으므로, 여기서는 약관 단계를 건너뛰되 드러나게 남긴다
      log.warn("시행 중인 필수 약관이 없어 약관 동의 단계를 건너뜁니다 (userId={})", user.getId());
      return true;
    }
    long agreed =
        userTermAgreementRepository.countByUserIdAndTermIdInAndAgreedTrue(
            user.getId(), requiredIds);
    return agreed == requiredIds.size();
  }
}
