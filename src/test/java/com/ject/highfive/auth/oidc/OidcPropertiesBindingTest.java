package com.ject.highfive.auth.oidc;

import static org.assertj.core.api.Assertions.assertThat;

import com.ject.highfive.user.entity.SocialProvider;
import java.io.IOException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.FileSystemResource;

// 테스트용 application.yml에는 oidc 설정이 없으므로, main의 application.yml이 의도대로 읽히는지 따로 확인한다
class OidcPropertiesBindingTest {

  private static OidcProperties bindMainApplicationYml() throws IOException {
    StandardEnvironment environment = new StandardEnvironment();
    new YamlPropertySourceLoader()
        .load("main", new FileSystemResource("src/main/resources/application.yml"))
        .forEach(source -> environment.getPropertySources().addLast(source));
    return Binder.get(environment).bind("oidc", OidcProperties.class).get();
  }

  @Test
  @DisplayName("세 소셜의 JWKS·발급자를 읽고, 구글은 발급자 두 개를 허용한다")
  void bindsAllProviders() throws IOException {
    OidcProperties properties = bindMainApplicationYml();

    assertThat(properties.providers())
        .containsOnlyKeys(SocialProvider.KAKAO, SocialProvider.GOOGLE, SocialProvider.APPLE);
    assertThat(properties.providers().get(SocialProvider.KAKAO).jwkSetUri())
        .isEqualTo("https://kauth.kakao.com/.well-known/jwks.json");
    assertThat(properties.providers().get(SocialProvider.GOOGLE).issuers())
        .containsExactly("accounts.google.com", "https://accounts.google.com");
    assertThat(properties.providers().get(SocialProvider.APPLE).issuers())
        .containsExactly("https://appleid.apple.com");
  }
}
