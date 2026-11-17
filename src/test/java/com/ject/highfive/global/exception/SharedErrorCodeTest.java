package com.ject.highfive.global.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.ject.highfive.concert.exception.ConcertErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class SharedErrorCodeTest {

  @Test
  @DisplayName("공연 없음은 404 CONCERT_001")
  void concertNotFound() {
    assertThat(ConcertErrorCode.CONCERT_NOT_FOUND.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(ConcertErrorCode.CONCERT_NOT_FOUND.getCode()).isEqualTo("CONCERT_001");
  }

  @Test
  @DisplayName("권한 없음은 403 AUTH_005")
  void forbidden() {
    assertThat(AuthErrorCode.FORBIDDEN.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(AuthErrorCode.FORBIDDEN.getCode()).isEqualTo("AUTH_005");
  }
}
