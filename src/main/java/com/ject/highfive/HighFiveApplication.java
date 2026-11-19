package com.ject.highfive;

import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

// 로그인은 JWT로만 하므로 Boot가 만드는 기본 사용자(generated security password)는 쓰지 않는다
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class HighFiveApplication {

  public static void main(String[] args) {
    // 서버 시간대는 컨벤션대로 Asia/Seoul 고정 (실행 환경의 시간대 설정과 무관하게)
    TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
    SpringApplication.run(HighFiveApplication.class, args);
  }
}
