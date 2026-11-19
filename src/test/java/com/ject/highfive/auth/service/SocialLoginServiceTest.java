package com.ject.highfive.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.ject.highfive.auth.dto.LoginResponse;
import com.ject.highfive.auth.dto.TokenResponse;
import com.ject.highfive.auth.oidc.OidcTokenVerifier;
import com.ject.highfive.auth.oidc.SocialUserInfo;
import com.ject.highfive.global.exception.AuthErrorCode;
import com.ject.highfive.global.exception.BusinessException;
import com.ject.highfive.user.dto.NextStep;
import com.ject.highfive.user.entity.SocialProvider;
import com.ject.highfive.user.entity.User;
import com.ject.highfive.user.entity.UserSocialAccount;
import com.ject.highfive.user.entity.UserStatus;
import com.ject.highfive.user.repository.UserSocialAccountRepository;
import com.ject.highfive.user.service.NextStepResolver;
import com.ject.highfive.user.service.SocialSignUpService;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

class SocialLoginServiceTest {

  private static final String ID_TOKEN = "kakao-id-token";
  private static final SocialUserInfo KAKAO_USER = new SocialUserInfo("kakao-1", "a@b.com");

  private final OidcTokenVerifier oidcTokenVerifier = mock(OidcTokenVerifier.class);
  private final UserSocialAccountRepository accountRepository =
      mock(UserSocialAccountRepository.class);
  private final SocialSignUpService signUpService = mock(SocialSignUpService.class);
  private final AuthService authService = mock(AuthService.class);
  private final SocialLoginService socialLoginService =
      new SocialLoginService(
          oidcTokenVerifier, accountRepository, signUpService, authService, new NextStepResolver());

  private static User userWithId(long id) {
    User user = User.signUp();
    ReflectionTestUtils.setField(user, "id", id);
    return user;
  }

  private static Optional<UserSocialAccount> accountOf(User user) {
    return Optional.of(
        UserSocialAccount.builder()
            .user(user)
            .provider(SocialProvider.KAKAO)
            .providerUserId(KAKAO_USER.providerUserId())
            .build());
  }

  private void givenVerifiedAndTokens(long userId) {
    given(oidcTokenVerifier.verify(SocialProvider.KAKAO, ID_TOKEN)).willReturn(KAKAO_USER);
    given(authService.issueTokens(userId)).willReturn(new TokenResponse("access", "refresh"));
  }

  @Test
  @DisplayName("처음 로그인하면 PENDING 회원으로 가입시키고 약관 화면으로 보낸다")
  void firstLoginSignsUp() {
    givenVerifiedAndTokens(10L);
    given(accountRepository.findByProviderAndProviderUserId(SocialProvider.KAKAO, "kakao-1"))
        .willReturn(Optional.empty());
    given(signUpService.signUp(SocialProvider.KAKAO, "kakao-1", "a@b.com"))
        .willReturn(userWithId(10L));

    LoginResponse response = socialLoginService.login("kakao", ID_TOKEN);

    assertThat(response.accessToken()).isEqualTo("access");
    assertThat(response.refreshToken()).isEqualTo("refresh");
    assertThat(response.user().id()).isEqualTo(10L);
    assertThat(response.user().status()).isEqualTo(UserStatus.PENDING);
    assertThat(response.user().nickname()).isNull();
    assertThat(response.nextStep()).isEqualTo(NextStep.TERMS);
  }

  @Test
  @DisplayName("이미 가입한 소셜 계정이면 가입하지 않고 그 회원으로 로그인한다")
  void existingAccountLogsIn() {
    givenVerifiedAndTokens(10L);
    given(accountRepository.findByProviderAndProviderUserId(SocialProvider.KAKAO, "kakao-1"))
        .willReturn(accountOf(userWithId(10L)));

    LoginResponse response = socialLoginService.login("kakao", ID_TOKEN);

    assertThat(response.user().id()).isEqualTo(10L);
    verify(signUpService, never()).signUp(any(), any(), any());
  }

  @Test
  @DisplayName("동시에 처음 로그인해 가입이 겹치면 먼저 가입된 회원으로 로그인한다")
  void concurrentSignUpFallsBackToExistingAccount() {
    givenVerifiedAndTokens(10L);
    given(accountRepository.findByProviderAndProviderUserId(SocialProvider.KAKAO, "kakao-1"))
        .willReturn(Optional.empty())
        .willReturn(accountOf(userWithId(10L)));
    willThrow(new DataIntegrityViolationException("duplicate"))
        .given(signUpService)
        .signUp(any(), any(), any());

    LoginResponse response = socialLoginService.login("kakao", ID_TOKEN);

    assertThat(response.user().id()).isEqualTo(10L);
  }

  @Test
  @DisplayName("kakao·google·apple이 아닌 provider는 AUTH_007이고 토큰을 검증하지 않는다")
  void unsupportedProviderIsRejected() {
    for (String provider : new String[] {"naver", "KAKAO", ""}) {
      assertThatThrownBy(() -> socialLoginService.login(provider, ID_TOKEN))
          .isInstanceOf(BusinessException.class)
          .extracting(e -> ((BusinessException) e).getErrorCode())
          .isEqualTo(AuthErrorCode.UNSUPPORTED_PROVIDER);
    }
    verifyNoInteractions(oidcTokenVerifier);
  }

  @Test
  @DisplayName("ID 토큰 검증에 실패하면 가입·토큰 발급을 하지 않는다")
  void invalidIdTokenStopsLogin() {
    given(oidcTokenVerifier.verify(SocialProvider.KAKAO, ID_TOKEN))
        .willThrow(new BusinessException(AuthErrorCode.SOCIAL_TOKEN_INVALID));

    assertThatThrownBy(() -> socialLoginService.login("kakao", ID_TOKEN))
        .isInstanceOf(BusinessException.class);
    verifyNoInteractions(signUpService, authService);
  }
}
