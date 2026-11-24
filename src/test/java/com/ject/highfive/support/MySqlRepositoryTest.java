package com.ject.highfive.support;

import com.ject.highfive.global.config.JpaAuditingConfig;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

// @DataJpaTest는 @Configuration을 읽지 않으므로 Auditing 설정을 함께 가져온다
// dev 서버와 같은 방식으로 Flyway 마이그레이션이 테이블을 만들고, Hibernate는 엔티티와 맞는지만 확인한다.
// 엔티티를 바꾸고 마이그레이션을 빠뜨리면 이 어노테이션을 쓰는 테스트가 모두 시작부터 실패한다
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({MySqlContainerConfig.class, JpaAuditingConfig.class})
@TestPropertySource(
    properties = {"spring.flyway.enabled=true", "spring.jpa.hibernate.ddl-auto=validate"})
public @interface MySqlRepositoryTest {}
