package com.ject.highfive.auth.service;

import com.ject.highfive.auth.dto.LoginResponse;
import com.ject.highfive.auth.dto.TokenResponse;
import com.ject.highfive.auth.oidc.OidcTokenVerifier;
import com.ject.highfive.auth.oidc.SocialUserInfo;
import com.ject.highfive.user.entity.SocialProvider;
import com.ject.highfive.user.entity.User;
import com.ject.highfive.user.entity.UserSocialAccount;
import com.ject.highfive.user.repository.UserSocialAccountRepository;
import com.ject.highfive.user.service.NextStepResolver;
import com.ject.highfive.user.service.SocialSignUpService;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

// 소셜 로그인: ID 토큰 검증 → 회원 조회(없으면 가입) → 서비스 토큰 발급 → 다음 화면 안내.
// 신규 가입과 기존 회원 로그인을 같은 흐름으로 처리한다
@Service
@RequiredArgsConstructor
public class SocialLoginService {

  private final OidcTokenVerifier oidcTokenVerifier;
  private final UserSocialAccountRepository userSocialAccountRepository;
  private final SocialSignUpService socialSignUpService;
  private final AuthService authService;
  private final NextStepResolver nextStepResolver;

  // 지원하지 않는 provider는 AUTH_007, ID 토큰 검증 실패는 AUTH_006
  public LoginResponse login(String providerPath, String idToken) {
    SocialProvider provider = SocialProvider.fromPath(providerPath);
    SocialUserInfo socialUser = oidcTokenVerifier.verify(provider, idToken);
    User user = findUser(provider, socialUser).orElseGet(() -> signUp(provider, socialUser));
    TokenResponse tokens = authService.issueTokens(user.getId());
    return new LoginResponse(
        tokens.accessToken(),
        tokens.refreshToken(),
        LoginResponse.LoginUser.from(user),
        nextStepResolver.resolve(user));
  }

  private Optional<User> findUser(SocialProvider provider, SocialUserInfo socialUser) {
    return userSocialAccountRepository
        .findByProviderAndProviderUserId(provider, socialUser.providerUserId())
        .map(UserSocialAccount::getUser);
  }

  private User signUp(SocialProvider provider, SocialUserInfo socialUser) {
    try {
      return socialSignUpService.signUp(provider, socialUser.providerUserId(), socialUser.email());
    } catch (DataIntegrityViolationException e) {
      // 같은 소셜 계정으로 동시에 처음 로그인하면 한 요청만 가입되고, 나머지는 그 계정으로 로그인한다
      return findUser(provider, socialUser).orElseThrow(() -> e);
    }
  }
}
