package com.ject.highfive.global.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// 현재 시각의 기준. 서버 시간대(Asia/Seoul)를 실행 환경(CI·컨테이너의 UTC 등)과 상관없이 고정하고,
// 테스트에서는 고정 시각으로 바꿔 끼울 수 있게 한다
@Configuration
public class ClockConfig {

  public static final ZoneId SERVER_ZONE = ZoneId.of("Asia/Seoul");

  @Bean
  public Clock clock() {
    return Clock.system(SERVER_ZONE);
  }
}
