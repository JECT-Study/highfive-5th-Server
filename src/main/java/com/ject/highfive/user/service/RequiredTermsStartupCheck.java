package com.ject.highfive.user.service;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.stereotype.Component;

// Flyway로 스키마·약관 데이터를 관리하는 환경(dev·운영)에서 시행 중인 필수 약관이 하나도 없으면 서버를 띄우지 않는다.
// 필수 약관이 없으면 TermAgreementChecker가 약관 단계를 건너뛰므로, 법적 필수 동의 없이 가입되는 일을 막는다.
// 시작이 실패하면 배포 헬스 체크(deploy/scripts/health.sh)에 걸려 배포도 실패로 드러난다
@Component
@ConditionalOnBooleanProperty("spring.flyway.enabled")
public class RequiredTermsStartupCheck implements ApplicationRunner {

  private final CurrentTerms currentTerms;

  public RequiredTermsStartupCheck(CurrentTerms currentTerms) {
    this.currentTerms = currentTerms;
  }

  @Override
  public void run(ApplicationArguments args) {
    if (currentTerms.requiredIds().isEmpty()) {
      throw new IllegalStateException(
          "시행 중인 필수 약관이 없습니다. terms 데이터(Flyway 마이그레이션)와 effective_at을 확인하세요.");
    }
  }
}
