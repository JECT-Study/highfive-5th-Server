package com.ject.highfive.user.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ject.highfive.support.MySqlRepositoryTest;
import com.ject.highfive.user.entity.SocialProvider;
import com.ject.highfive.user.entity.User;
import com.ject.highfive.user.entity.UserSocialAccount;
import com.ject.highfive.user.entity.UserStatus;
import jakarta.persistence.EntityManager;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

@MySqlRepositoryTest
class UserSocialAccountRepositoryTest {

  @Autowired UserRepository userRepository;
  @Autowired UserSocialAccountRepository userSocialAccountRepository;
  @Autowired EntityManager entityManager;

  private UserSocialAccount account(User user, SocialProvider provider, String providerUserId) {
    return UserSocialAccount.builder()
        .user(user)
        .provider(provider)
        .providerUserId(providerUserId)
        .email(null)
        .build();
  }

  @Test
  @DisplayName("가입한 회원은 PENDING이고 푸시 설정은 모두 켜져 있다")
  void signUpCreatesPendingUser() {
    User user = userRepository.saveAndFlush(User.signUp());
    entityManager.clear();

    User found = userRepository.findById(user.getId()).orElseThrow();
    assertThat(found.getStatus()).isEqualTo(UserStatus.PENDING);
    assertThat(found.getNickname()).isNull();
    assertThat(found.isPushEnabled()).isTrue();
    assertThat(found.isPushTicketEnabled()).isTrue();
    assertThat(found.getCreatedAt()).isNotNull();
  }

  @Test
  @DisplayName("소셜과 소셜 고유 ID로 찾고 회원도 함께 읽는다")
  void findByProviderAndProviderUserIdFetchesUser() {
    User user = userRepository.save(User.signUp());
    userSocialAccountRepository.saveAndFlush(account(user, SocialProvider.KAKAO, "kakao-1"));
    entityManager.clear();

    UserSocialAccount found =
        userSocialAccountRepository
            .findByProviderAndProviderUserId(SocialProvider.KAKAO, "kakao-1")
            .orElseThrow();

    assertThat(Hibernate.isInitialized(found.getUser())).isTrue();
    assertThat(found.getUser().getId()).isEqualTo(user.getId());
    assertThat(
            userSocialAccountRepository.findByProviderAndProviderUserId(
                SocialProvider.GOOGLE, "kakao-1"))
        .isEmpty();
  }

  @Test
  @DisplayName("같은 소셜 계정은 두 번 가입할 수 없다 (UNIQUE(provider, provider_user_id))")
  void sameSocialAccountIsUnique() {
    User first = userRepository.save(User.signUp());
    User second = userRepository.save(User.signUp());
    userSocialAccountRepository.saveAndFlush(account(first, SocialProvider.KAKAO, "kakao-1"));

    assertThatThrownBy(
            () ->
                userSocialAccountRepository.saveAndFlush(
                    account(second, SocialProvider.KAKAO, "kakao-1")))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  @DisplayName("소셜 고유 ID는 대소문자를 구분한다 (AbC와 abc는 다른 계정)")
  void providerUserIdIsCaseSensitive() {
    User upper = userRepository.save(User.signUp());
    User lower = userRepository.save(User.signUp());
    userSocialAccountRepository.saveAndFlush(account(upper, SocialProvider.APPLE, "AbC"));
    userSocialAccountRepository.saveAndFlush(account(lower, SocialProvider.APPLE, "abc"));
    entityManager.clear();

    assertThat(
            userSocialAccountRepository
                .findByProviderAndProviderUserId(SocialProvider.APPLE, "abc")
                .orElseThrow()
                .getUser()
                .getId())
        .isEqualTo(lower.getId());
    assertThat(
            userSocialAccountRepository.findByProviderAndProviderUserId(
                SocialProvider.APPLE, "ABC"))
        .isEmpty();
  }
}
