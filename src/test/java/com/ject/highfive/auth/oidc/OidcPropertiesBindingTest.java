package com.ject.highfive.auth.oidc;

import static org.assertj.core.api.Assertions.assertThat;

import com.ject.highfive.user.entity.SocialProvider;
import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MapPropertySource;
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

  @Test
  @DisplayName("환경변수에 쉼표로 넣은 aud 여러 개를 공백 없이 목록으로 읽는다")
  void bindsCommaSeparatedAudiences() throws IOException {
    StandardEnvironment environment = new StandardEnvironment();
    environment
        .getPropertySources()
        .addFirst(
            new MapPropertySource(
                "env",
                Map.of("OIDC_KAKAO_AUDIENCE", "default-key, dev-key", "OIDC_APPLE_AUDIENCE", "")));
    new YamlPropertySourceLoader()
        .load("main", new FileSystemResource("src/main/resources/application.yml"))
        .forEach(source -> environment.getPropertySources().addLast(source));

    OidcProperties properties = Binder.get(environment).bind("oidc", OidcProperties.class).get();

    assertThat(properties.providers().get(SocialProvider.KAKAO).audiences())
        .containsExactly("default-key", "dev-key");
    assertThat(properties.providers().get(SocialProvider.APPLE).audiences()).isEmpty();
    assertThat(properties.providers().get(SocialProvider.APPLE).isConfigured()).isFalse();
  }
}
