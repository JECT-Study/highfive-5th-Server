package com.ject.highfive.code.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.ject.highfive.concert.entity.Genre;
import java.util.Arrays;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GenreCodeTest {

  @Test
  @DisplayName("개인 프로필 장르 코드는 공연 장르(Genre)와 같은 값·순서다")
  void sameValuesAsConcertGenre() {
    assertThat(Arrays.stream(GenreCode.values()).map(Enum::name))
        .containsExactlyElementsOf(Arrays.stream(Genre.values()).map(Enum::name).toList());
  }

  @Test
  @DisplayName("직접 입력 값이 필요한 것은 장르 ETC, 포지션 CUSTOM뿐이다")
  void onlyEtcAndCustomRequireCustomName() {
    assertThat(Arrays.stream(GenreCode.values()).filter(GenreCode::requiresCustomName))
        .containsExactly(GenreCode.ETC);
    assertThat(Arrays.stream(PositionCode.values()).filter(PositionCode::requiresCustomName))
        .containsExactly(PositionCode.CUSTOM);
  }
}
