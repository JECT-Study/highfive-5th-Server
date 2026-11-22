package com.ject.highfive.auth.oidc;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(OidcProperties.class)
public class OidcConfig {}
