package com.ject.highfive.auth.oidc;

import com.ject.highfive.user.entity.SocialProvider;
import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

// 소셜별 ID 토큰 검증 설정. 검증 방식은 하나이고 소셜마다 다른 것은 이 값들뿐이다.
// audience(카카오 앱 키·구글 서버 클라이언트 ID·애플 번들 ID)는 환경변수로 받는다
@ConfigurationProperties(prefix = "oidc")
public record OidcProperties(Map<SocialProvider, Provider> providers) {

  public OidcProperties {
    providers = providers == null ? Map.of() : Map.copyOf(providers);
  }

  // issuers: 구글은 accounts.google.com과 https://accounts.google.com 둘 다 쓴다
  public record Provider(String jwkSetUri, List<String> issuers, String audience) {

    // audience가 없으면 그 소셜은 아직 연동 전(콘솔 미등록)이다
    public boolean isConfigured() {
      return jwkSetUri != null
          && !jwkSetUri.isBlank()
          && issuers != null
          && !issuers.isEmpty()
          && audience != null
          && !audience.isBlank();
    }
  }
}
