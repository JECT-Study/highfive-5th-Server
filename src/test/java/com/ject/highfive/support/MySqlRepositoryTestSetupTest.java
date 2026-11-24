package com.ject.highfive.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

@MySqlRepositoryTest
class MySqlRepositoryTestSetupTest {

  @Autowired TestEntityManager em;

  @Test
  @DisplayName("Repository 테스트는 MySQL 8.4 컨테이너에서 돈다")
  void runsOnMySql() {
    Object version = em.getEntityManager().createNativeQuery("select version()").getSingleResult();
    assertThat(version.toString()).startsWith("8.4");
  }

  @Test
  @DisplayName("테이블은 Flyway 마이그레이션으로 만들어진다 (엔티티 검증은 컨텍스트가 뜰 때 validate가 한다)")
  void schemaIsCreatedByFlyway() {
    List<String> applied =
        em
            .getEntityManager()
            .createNativeQuery(
                "select version from flyway_schema_history"
                    + " where success = 1 order by installed_rank")
            .getResultList()
            .stream()
            .map(String::valueOf)
            .toList();
    assertThat(applied).contains("1");
  }
}
