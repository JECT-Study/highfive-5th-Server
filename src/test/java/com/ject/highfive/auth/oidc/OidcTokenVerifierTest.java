package com.ject.highfive.auth.oidc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ject.highfive.global.exception.AuthErrorCode;
import com.ject.highfive.global.exception.BusinessException;
import com.ject.highfive.user.entity.SocialProvider;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

// 소셜 JWKS 대신 테스트에서 만든 RSA 키로 서명·검증해 iss·aud·exp 검사를 확인한다
class OidcTokenVerifierTest {

  private static final String KAKAO_ISSUER = "https://kauth.kakao.com";
  private static final String KAKAO_APP_KEY = "kakao-app-key";
  // dev 빌드는 다른 네이티브 앱 키를 쓴다
  private static final String KAKAO_DEV_APP_KEY = "kakao-dev-app-key";
  private static final OidcProperties.Provider KAKAO =
      new OidcProperties.Provider(
          "https://unused", List.of(KAKAO_ISSUER), List.of(KAKAO_APP_KEY, KAKAO_DEV_APP_KEY));
  private static final OidcProperties.Provider GOOGLE =
      new OidcProperties.Provider(
          "https://unused",
          List.of("accounts.google.com", "https://accounts.google.com"),
          List.of("google-client-id"));

  private final KeyPair socialKey = generateRsaKey();
  private final KeyPair otherKey = generateRsaKey();

  private final OidcTokenVerifier verifier =
      new OidcTokenVerifier(
          Map.of(
              SocialProvider.KAKAO, decoderFor(KAKAO),
              SocialProvider.GOOGLE, decoderFor(GOOGLE)));

  private static KeyPair generateRsaKey() {
    try {
      KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
      generator.initialize(2048);
      return generator.generateKeyPair();
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  private NimbusJwtDecoder decoderFor(OidcProperties.Provider provider) {
    NimbusJwtDecoder decoder =
        NimbusJwtDecoder.withPublicKey((RSAPublicKey) socialKey.getPublic()).build();
    decoder.setJwtValidator(OidcTokenVerifier.validator(provider));
    return decoder;
  }

  // 정상 카카오 ID 토큰에서 일부 클레임만 바꾼다
  private String kakaoToken(KeyPair key, Consumer<JwtClaimsSet.Builder> customizer) {
    Instant now = Instant.now();
    JwtClaimsSet.Builder claims =
        JwtClaimsSet.builder()
            .issuer(KAKAO_ISSUER)
            .audience(List.of(KAKAO_APP_KEY))
            .subject("kakao-user-1")
            .issuedAt(now)
            .expiresAt(now.plus(Duration.ofHours(1)))
            .claim("email", "user@example.com");
    customizer.accept(claims);
    return NimbusJwtEncoder.withKeyPair(
            (RSAPublicKey) key.getPublic(), (RSAPrivateKey) key.getPrivate())
        .build()
        .encode(
            JwtEncoderParameters.from(
                JwsHeader.with(SignatureAlgorithm.RS256).build(), claims.build()))
        .getTokenValue();
  }

  private void assertRejected(SocialProvider provider, String token) {
    assertThatThrownBy(() -> verifier.verify(provider, token))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(AuthErrorCode.SOCIAL_TOKEN_INVALID);
  }

  @Test
  @DisplayName("올바른 ID 토큰이면 소셜 고유 ID(sub)와 이메일을 꺼낸다")
  void validTokenReturnsSubjectAndEmail() {
    SocialUserInfo info = verifier.verify(SocialProvider.KAKAO, kakaoToken(socialKey, c -> {}));

    assertThat(info.providerUserId()).isEqualTo("kakao-user-1");
    assertThat(info.email()).isEqualTo("user@example.com");
  }

  @Test
  @DisplayName("이메일이 없는 토큰도 통과하고 email은 null")
  void emailIsOptional() {
    String token = kakaoToken(socialKey, c -> c.claims(claims -> claims.remove("email")));

    assertThat(verifier.verify(SocialProvider.KAKAO, token).email()).isNull();
  }

  @Test
  @DisplayName("다른 키로 서명·다른 발급자·다른 앱용·만료된 토큰은 AUTH_006")
  void invalidTokensAreRejected() {
    assertRejected(SocialProvider.KAKAO, kakaoToken(otherKey, c -> {}));
    assertRejected(SocialProvider.KAKAO, kakaoToken(socialKey, c -> c.issuer("https://evil.com")));
    assertRejected(
        SocialProvider.KAKAO, kakaoToken(socialKey, c -> c.audience(List.of("other-app"))));
    assertRejected(
        SocialProvider.KAKAO,
        kakaoToken(
            socialKey,
            c ->
                c.issuedAt(Instant.now().minus(Duration.ofHours(3)))
                    .expiresAt(Instant.now().minus(Duration.ofHours(2)))));
    assertRejected(SocialProvider.KAKAO, kakaoToken(socialKey, c -> c.subject(" ")));
    assertRejected(SocialProvider.KAKAO, "not-a-jwt");
  }

  @Test
  @DisplayName("구글은 scheme 없는 발급자(accounts.google.com)도 허용한다")
  void googleAcceptsBothIssuers() {
    for (String issuer : GOOGLE.issuers()) {
      String token =
          kakaoToken(socialKey, c -> c.issuer(issuer).audience(List.of("google-client-id")));

      assertThat(verifier.verify(SocialProvider.GOOGLE, token).providerUserId())
          .isEqualTo("kakao-user-1");
    }
  }

  @Test
  @DisplayName("다른 소셜용 토큰(카카오 토큰을 구글로)은 AUTH_006")
  void tokenOfAnotherProviderIsRejected() {
    assertRejected(SocialProvider.GOOGLE, kakaoToken(socialKey, c -> {}));
  }

  @Test
  @DisplayName("설정이 없는 소셜(콘솔 등록 전)은 AUTH_006")
  void unconfiguredProviderIsRejected() {
    assertRejected(SocialProvider.APPLE, kakaoToken(socialKey, c -> {}));
  }

  @Test
  @DisplayName("audience가 비어 있는 소셜은 검증기를 만들지 않는다")
  void providerWithoutAudienceIsDisabled() {
    OidcTokenVerifier fromProperties =
        new OidcTokenVerifier(
            new OidcProperties(
                Map.of(
                    SocialProvider.KAKAO,
                    new OidcProperties.Provider(
                        "https://kauth.kakao.com/.well-known/jwks.json",
                        List.of(KAKAO_ISSUER),
                        List.of(" ", "")))));

    assertThatThrownBy(() -> fromProperties.verify(SocialProvider.KAKAO, "any"))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(AuthErrorCode.SOCIAL_TOKEN_INVALID);
  }

  @Test
  @DisplayName("exp가 없거나 nbf가 아직 오지 않은 토큰은 AUTH_006")
  void missingExpiryAndFutureNotBeforeAreRejected() {
    assertRejected(
        SocialProvider.KAKAO,
        kakaoToken(socialKey, c -> c.claims(claims -> claims.remove(JwtClaimNames.EXP))));
    assertRejected(
        SocialProvider.KAKAO,
        kakaoToken(socialKey, c -> c.notBefore(Instant.now().plus(Duration.ofMinutes(10)))));
  }

  @Test
  @DisplayName("서명 없는 토큰(alg: none)·null·빈 토큰은 AUTH_006")
  void unsignedAndBlankTokensAreRejected() {
    Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
    String payload =
        encoder.encodeToString(
            ("{\"iss\":\""
                    + KAKAO_ISSUER
                    + "\",\"aud\":\""
                    + KAKAO_APP_KEY
                    + "\",\"sub\":\"kakao-user-1\",\"exp\":"
                    + Instant.now().plus(Duration.ofHours(1)).getEpochSecond()
                    + "}")
                .getBytes());
    String unsigned = encoder.encodeToString("{\"alg\":\"none\"}".getBytes()) + "." + payload + ".";

    assertRejected(SocialProvider.KAKAO, unsigned);
    assertRejected(SocialProvider.KAKAO, null);
    assertRejected(SocialProvider.KAKAO, " ");
  }

  @Test
  @DisplayName("이메일이 빈 문자열이면 null로 바꾼다")
  void blankEmailBecomesNull() {
    String token = kakaoToken(socialKey, c -> c.claim("email", ""));

    assertThat(verifier.verify(SocialProvider.KAKAO, token).email()).isNull();
  }

  @Test
  @DisplayName("JWKS를 받아 오지 못한 장애는 AUTH_006으로 숨기지 않고 그대로 던진다(전역 처리기의 500)")
  void jwksOutageIsNotHiddenAsInvalidToken() {
    OidcTokenVerifier outage =
        new OidcTokenVerifier(
            Map.of(
                SocialProvider.KAKAO,
                token -> {
                  throw new JwtException("JWKS 서버에 연결할 수 없음");
                }));

    assertThatThrownBy(() -> outage.verify(SocialProvider.KAKAO, "any-token"))
        .isInstanceOf(JwtException.class)
        .isNotInstanceOf(BusinessException.class);
  }

  @Test
  @DisplayName("aud가 설정한 값 중 하나와 같으면 통과한다 (카카오 Default·Dev 앱 키)")
  void acceptsAnyConfiguredAudience() {
    String devBuildToken = kakaoToken(socialKey, c -> c.audience(List.of(KAKAO_DEV_APP_KEY)));

    assertThat(verifier.verify(SocialProvider.KAKAO, devBuildToken).providerUserId())
        .isEqualTo("kakao-user-1");
  }
}
