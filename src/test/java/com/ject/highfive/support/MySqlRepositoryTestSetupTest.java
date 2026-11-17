package com.ject.highfive.support;

import static org.assertj.core.api.Assertions.assertThat;

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
}
