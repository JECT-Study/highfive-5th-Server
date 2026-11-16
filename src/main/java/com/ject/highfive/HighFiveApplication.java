package com.ject.highfive;

import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class HighFiveApplication {

  public static void main(String[] args) {
    // 서버 시간대는 컨벤션대로 Asia/Seoul 고정 (실행 환경의 시간대 설정과 무관하게)
    TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
    SpringApplication.run(HighFiveApplication.class, args);
  }
}
