package com.ject.highfive.user.repository;

import com.ject.highfive.user.entity.SocialProvider;
import com.ject.highfive.user.entity.UserSocialAccount;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSocialAccountRepository extends JpaRepository<UserSocialAccount, Long> {

  // 로그인 응답에 회원 정보가 필요하므로 회원도 함께 읽는다
  @EntityGraph(attributePaths = "user")
  Optional<UserSocialAccount> findByProviderAndProviderUserId(
      SocialProvider provider, String providerUserId);
}
