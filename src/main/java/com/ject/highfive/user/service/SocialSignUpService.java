package com.ject.highfive.user.service;

import com.ject.highfive.user.entity.SocialProvider;
import com.ject.highfive.user.entity.User;
import com.ject.highfive.user.entity.UserSocialAccount;
import com.ject.highfive.user.repository.UserRepository;
import com.ject.highfive.user.repository.UserSocialAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SocialSignUpService {

  private final UserRepository userRepository;
  private final UserSocialAccountRepository userSocialAccountRepository;

  // 회원(PENDING)과 로그인 수단을 한 트랜잭션으로 만든다.
  // 같은 소셜 계정이 이미 있으면 UNIQUE 위반(DataIntegrityViolationException)으로 전체가 롤백된다
  @Transactional
  public User signUp(SocialProvider provider, String providerUserId, String email) {
    User user = userRepository.save(User.signUp());
    userSocialAccountRepository.saveAndFlush(
        UserSocialAccount.builder()
            .user(user)
            .provider(provider)
            .providerUserId(providerUserId)
            .email(email)
            .build());
    return user;
  }
}
