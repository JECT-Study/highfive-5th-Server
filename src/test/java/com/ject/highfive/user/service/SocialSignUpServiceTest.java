package com.ject.highfive.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ject.highfive.support.MySqlRepositoryTest;
import com.ject.highfive.user.entity.SocialProvider;
import com.ject.highfive.user.entity.User;
import com.ject.highfive.user.repository.UserRepository;
import com.ject.highfive.user.repository.UserSocialAccountRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

// 가입 트랜잭션이 실제로 커밋·롤백되는지 보려고 테스트 자체 트랜잭션은 끈다
@MySqlRepositoryTest
@Import(SocialSignUpService.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class SocialSignUpServiceTest {

  @Autowired SocialSignUpService socialSignUpService;
  @Autowired UserRepository userRepository;
  @Autowired UserSocialAccountRepository userSocialAccountRepository;

  @AfterEach
  void cleanUp() {
    userSocialAccountRepository.deleteAllInBatch();
    userRepository.deleteAllInBatch();
  }

  @Test
  @DisplayName("회원과 소셜 계정을 함께 만든다")
  void signUpCreatesUserAndAccount() {
    User user = socialSignUpService.signUp(SocialProvider.KAKAO, "kakao-1", "a@b.com");

    assertThat(userRepository.count()).isEqualTo(1);
    assertThat(
            userSocialAccountRepository.findByProviderAndProviderUserId(
                SocialProvider.KAKAO, "kakao-1"))
        .get()
        .satisfies(account -> assertThat(account.getUser().getId()).isEqualTo(user.getId()));
  }

  @Test
  @DisplayName("이미 가입한 소셜 계정으로 다시 가입하면 실패하고, 먼저 만든 회원 행도 남지 않는다")
  void duplicateSignUpRollsBackUser() {
    socialSignUpService.signUp(SocialProvider.KAKAO, "kakao-1", null);

    assertThatThrownBy(() -> socialSignUpService.signUp(SocialProvider.KAKAO, "kakao-1", null))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThat(userRepository.count()).isEqualTo(1);
    assertThat(userSocialAccountRepository.count()).isEqualTo(1);
  }
}
