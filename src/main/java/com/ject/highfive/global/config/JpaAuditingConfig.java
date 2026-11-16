package com.ject.highfive.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

// 메인 클래스에 붙이면 @WebMvcTest 같은 슬라이스 테스트가 JPA 없이 뜨지 못해 따로 둔다
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {}
