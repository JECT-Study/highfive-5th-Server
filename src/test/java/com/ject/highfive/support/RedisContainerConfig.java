package com.ject.highfive.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;

// Redis를 쓰는 테스트용 컨테이너. 로컬 compose.yaml과 같은 메이저 버전을 쓴다 (Docker 필요)
@TestConfiguration(proxyBeanMethods = false)
public class RedisContainerConfig {

  private static final int REDIS_PORT = 6379;

  @Bean
  @ServiceConnection(name = "redis")
  GenericContainer<?> redisContainer() {
    return new GenericContainer<>("redis:7").withExposedPorts(REDIS_PORT);
  }
}
