package com.ject.highfive.user.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.ject.highfive.support.MySqlRepositoryTest;
import com.ject.highfive.user.entity.Term;
import com.ject.highfive.user.entity.User;
import com.ject.highfive.user.entity.UserTermAgreement;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

// V2 마이그레이션이 넣은 약관 데이터와 동의 기록 제약을 실제 MySQL에서 확인한다
@MySqlRepositoryTest
class TermRepositoryTest {

  @Autowired TermRepository termRepository;
  @Autowired UserTermAgreementRepository userTermAgreementRepository;
  @Autowired UserRepository userRepository;

  @Test
  @DisplayName("V2는 지금 시행 중인 필수 약관 3개(이용약관·개인정보·만 14세)를 넣는다")
  void seedsRequiredTerms() {
    List<Term> current = termRepository.findAllByEffectiveAtLessThanEqual(LocalDateTime.now());

    assertThat(current)
        .extracting(Term::getCode, Term::getVersion, Term::isRequired)
        .containsExactlyInAnyOrder(
            tuple("SERVICE", "1.0", true),
            tuple("PRIVACY", "1.0", true),
            tuple("AGE_OVER_14", "1.0", true));
  }

  @Test
  @DisplayName("같은 회원이 같은 약관 버전에 두 번 기록되지 않는다 (UNIQUE)")
  void agreementIsUniquePerUserAndTerm() {
    User user = userRepository.saveAndFlush(User.signUp());
    Term service =
        termRepository.findAllByEffectiveAtLessThanEqual(LocalDateTime.now()).stream()
            .filter(term -> term.getCode().equals("SERVICE"))
            .findFirst()
            .orElseThrow();
    userTermAgreementRepository.saveAndFlush(
        new UserTermAgreement(user, service, true, LocalDateTime.now()));

    assertThatThrownBy(
            () ->
                userTermAgreementRepository.saveAndFlush(
                    new UserTermAgreement(user, service, false, LocalDateTime.now())))
        .isInstanceOf(DataIntegrityViolationException.class);
  }
}
