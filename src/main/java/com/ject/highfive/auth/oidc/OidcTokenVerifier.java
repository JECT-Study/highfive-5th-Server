package com.ject.highfive.auth.oidc;

import com.ject.highfive.global.exception.AuthErrorCode;
import com.ject.highfive.global.exception.BusinessException;
import com.ject.highfive.user.entity.SocialProvider;
import java.time.Duration;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

// 카카오·구글·애플 OIDC ID 토큰을 같은 방식으로 검증한다:
// 소셜 공개키(JWKS)로 서명 확인 + iss·aud·exp 확인. 실패하면 AUTH_006
@Slf4j
@Component
public class OidcTokenVerifier {

  private static final String EMAIL_CLAIM = "email";

  // 소셜 JWKS가 느리거나 응답하지 않을 때 요청 스레드가 오래 묶이지 않도록 제한한다
  private static final Duration JWKS_CONNECT_TIMEOUT = Duration.ofSeconds(3);
  private static final Duration JWKS_READ_TIMEOUT = Duration.ofSeconds(5);
  private static final RestTemplate JWKS_REST_TEMPLATE = createJwksRestTemplate();

  private final Map<SocialProvider, JwtDecoder> decoders;

  @Autowired
  public OidcTokenVerifier(OidcProperties properties) {
    this(createDecoders(properties));
  }

  // 테스트에서 공개키를 직접 넣은 디코더를 쓰기 위한 생성자
  OidcTokenVerifier(Map<SocialProvider, JwtDecoder> decoders) {
    this.decoders = decoders.isEmpty() ? Map.of() : new EnumMap<>(decoders);
  }

  public SocialUserInfo verify(SocialProvider provider, String idToken) {
    JwtDecoder decoder = decoders.get(provider);
    if (decoder == null) {
      // 콘솔 등록 전이라 audiences가 없는 소셜. 앱에는 검증 실패로 알리고 원인은 로그로 남긴다
      log.warn("{} 로그인 설정(oidc.providers)이 없어 ID 토큰을 검증할 수 없습니다.", provider);
      throw new BusinessException(AuthErrorCode.SOCIAL_TOKEN_INVALID);
    }
    if (idToken == null || idToken.isBlank()) {
      throw new BusinessException(AuthErrorCode.SOCIAL_TOKEN_INVALID);
    }
    Jwt jwt;
    try {
      jwt = decoder.decode(idToken);
    } catch (BadJwtException e) {
      // 잘못된 토큰(서명·형식·iss·aud·만료)만 AUTH_006. 토큰 원문은 로그에 남기지 않는다.
      // JWKS를 받아 오지 못한 경우(소셜 서버 장애·타임아웃)는 일반 JwtException이라 잡지 않고
      // 전역 처리기의 500으로 보내 서버 장애로 드러나게 한다
      log.info("{} ID 토큰 검증 실패: {}", provider, e.getMessage());
      throw new BusinessException(AuthErrorCode.SOCIAL_TOKEN_INVALID);
    }
    String subject = jwt.getSubject();
    if (subject == null || subject.isBlank()) {
      throw new BusinessException(AuthErrorCode.SOCIAL_TOKEN_INVALID);
    }
    return new SocialUserInfo(subject, emptyToNull(jwt.getClaimAsString(EMAIL_CLAIM)));
  }

  private static String emptyToNull(String value) {
    return value == null || value.isBlank() ? null : value;
  }

  // 서명 외에 확인할 것: 만료(exp·nbf), 발급자(iss), 우리 앱용 토큰인지(aud)
  static OAuth2TokenValidator<Jwt> validator(OidcProperties.Provider provider) {
    List<String> issuers = provider.issuers();
    List<String> audiences = provider.audiences();
    // 기본값은 exp가 없으면 통과시키므로 막는다 (OIDC에서 exp는 필수 클레임)
    JwtTimestampValidator timestampValidator = new JwtTimestampValidator();
    timestampValidator.setAllowEmptyExpiryClaim(false);
    return new DelegatingOAuth2TokenValidator<>(
        timestampValidator,
        new JwtClaimValidator<Object>(
            JwtClaimNames.ISS, iss -> iss != null && issuers.contains(iss.toString())),
        new JwtClaimValidator<List<String>>(
            JwtClaimNames.AUD, aud -> aud != null && aud.stream().anyMatch(audiences::contains)));
  }

  private static Map<SocialProvider, JwtDecoder> createDecoders(OidcProperties properties) {
    Map<SocialProvider, JwtDecoder> decoders = new EnumMap<>(SocialProvider.class);
    properties
        .providers()
        .forEach(
            (provider, config) -> {
              if (!config.isConfigured()) {
                log.warn("{} 로그인은 oidc.providers 설정(audiences 등)이 없어 비활성화합니다.", provider);
                return;
              }
              // JWKS는 처음 검증할 때 받아 오고 캐시한다. 세 소셜 모두 RS256으로 서명한다
              NimbusJwtDecoder decoder =
                  NimbusJwtDecoder.withJwkSetUri(config.jwkSetUri())
                      .jwsAlgorithm(SignatureAlgorithm.RS256)
                      .restOperations(JWKS_REST_TEMPLATE)
                      .build();
              decoder.setJwtValidator(validator(config));
              decoders.put(provider, decoder);
            });
    return decoders;
  }

  private static RestTemplate createJwksRestTemplate() {
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(JWKS_CONNECT_TIMEOUT);
    requestFactory.setReadTimeout(JWKS_READ_TIMEOUT);
    return new RestTemplate(requestFactory);
  }
}
