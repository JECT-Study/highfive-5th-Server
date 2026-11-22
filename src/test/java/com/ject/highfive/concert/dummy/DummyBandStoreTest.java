package com.ject.highfive.concert.dummy;

import static org.assertj.core.api.Assertions.assertThat;

import com.ject.highfive.concert.entity.Genre;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DummyBandStoreTest {

  private final DummyBandStore store = new DummyBandStore();

  @Test
  @DisplayName("목록에 있는 id의 밴드만 돌려주고 없는 id는 빠진다")
  void findAllByIdsSkipsUnknown() {
    assertThat(store.findAllByIds(List.of(1L, 3L, 99L))).containsOnlyKeys(1L, 3L);
  }

  @Test
  @DisplayName("같은 id가 여러 번 와도 한 번만 담긴다")
  void duplicateIdsCollapse() {
    assertThat(store.findAllByIds(List.of(3L, 3L))).containsOnlyKeys(3L);
  }

  @Test
  @DisplayName("빈 목록이면 빈 Map")
  void emptyIds() {
    assertThat(store.findAllByIds(List.of())).isEmpty();
  }

  @Test
  @DisplayName("밴드 값은 이름·장르(선언 순서)·인원 수를 갖는다")
  void bandValues() {
    DummyBand band = store.findAllByIds(List.of(1L)).get(1L);

    assertThat(band.name()).isEqualTo("새벽세시");
    assertThat(band.genres()).containsExactly(Genre.ROCK, Genre.INDIE);
    assertThat(band.memberCount()).isEqualTo(4);
  }
}
