package com.ject.highfive.auth.oidc;

import com.ject.highfive.user.entity.SocialProvider;
import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

// 소셜별 ID 토큰 검증 설정. 검증 방식은 하나이고 소셜마다 다른 것은 이 값들뿐이다.
// audiences(카카오 앱 키·구글 클라이언트 ID·애플 번들 ID)는 환경변수로 받는다
@ConfigurationProperties(prefix = "oidc")
public record OidcProperties(Map<SocialProvider, Provider> providers) {

  public OidcProperties {
    providers = providers == null ? Map.of() : Map.copyOf(providers);
  }

  // issuers: 구글은 accounts.google.com과 https://accounts.google.com 둘 다 쓴다.
  // audiences: 토큰의 aud가 이 중 하나와 같으면 우리 앱용으로 본다. dev·운영 빌드가 다른 앱 키를 쓰거나
  // (카카오 네이티브 앱 키 Default·Dev), 플랫폼마다 aud가 다를 수 있어서 여러 개를 받는다
  public record Provider(String jwkSetUri, List<String> issuers, List<String> audiences) {

    public Provider {
      issuers = nonBlank(issuers);
      audiences = nonBlank(audiences);
    }

    // audiences가 없으면 그 소셜은 아직 연동 전(콘솔 미등록)이다
    public boolean isConfigured() {
      return jwkSetUri != null
          && !jwkSetUri.isBlank()
          && !issuers.isEmpty()
          && !audiences.isEmpty();
    }

    // 환경변수가 비어 있거나 "a, b"처럼 공백이 섞여 와도 빈 값 없이 정리한다
    private static List<String> nonBlank(List<String> values) {
      if (values == null) {
        return List.of();
      }
      return values.stream().map(String::trim).filter(value -> !value.isEmpty()).toList();
    }
  }
}
