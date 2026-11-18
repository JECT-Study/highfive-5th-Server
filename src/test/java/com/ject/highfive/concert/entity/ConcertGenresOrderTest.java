package com.ject.highfive.concert.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ConcertGenresOrderTest {

  private Concert concert(Genre... genres) {
    return Concert.builder()
        .hostBandId(3L)
        .createdBy(1L)
        .title("겨울 합동공연")
        .recruitTeamCount(3)
        .genres(List.of(genres))
        .build();
  }

  @Test
  @DisplayName("장르를 섞어 넣어도 선언 순서로 나온다")
  void genresFollowDeclarationOrder() {
    Concert concert = concert(Genre.ETC, Genre.ROCK, Genre.POP);

    assertThat(concert.getGenres()).containsExactly(Genre.ROCK, Genre.POP, Genre.ETC);
  }

  @Test
  @DisplayName("장르를 바꾼 직후에도 선언 순서로 나온다")
  void orderKeptAfterChange() {
    Concert concert = concert(Genre.ROCK);

    concert.changeGenres(List.of(Genre.JAZZ, Genre.INDIE, Genre.ROCK));

    assertThat(concert.getGenres()).containsExactly(Genre.ROCK, Genre.INDIE, Genre.JAZZ);
  }

  @Test
  @DisplayName("장르 선언 순서는 ERD 주석 순서와 같다 (화면 표시 순서)")
  void declarationOrderMatchesErdComment() {
    assertThat(Genre.values())
        .containsExactly(
            Genre.ROCK, Genre.INDIE, Genre.METAL, Genre.PUNK, Genre.POP, Genre.JAZZ, Genre.ETC);
  }
}
