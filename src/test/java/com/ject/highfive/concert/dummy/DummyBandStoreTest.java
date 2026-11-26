package com.ject.highfive.concert.dummy;

import static org.assertj.core.api.Assertions.assertThat;

import com.ject.highfive.concert.entity.Genre;
import com.ject.highfive.recruit.entity.WantedCareerLevel;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

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

  private static final PageRequest FIRST_PAGE = PageRequest.of(0, 10);

  private List<Long> ids(Page<DummyBand> page) {
    return page.map(DummyBand::id).getContent();
  }

  private List<Long> search(
      String keyword, String region, List<Genre> genres, List<WantedCareerLevel> careerLevels) {
    return ids(store.search(keyword, region, genres, careerLevels, FIRST_PAGE));
  }

  @Test
  @DisplayName("고정 목록에 지역·연주 기간·생성 시각이 들어 있다")
  void bandSearchValues() {
    DummyBand band = store.findAllByIds(List.of(3L)).get(3L);

    assertThat(band.region()).isEqualTo("GYEONGGI");
    assertThat(band.careerLevel()).isEqualTo(WantedCareerLevel.OVER_3Y);
    assertThat(band.createdAt()).isEqualTo(LocalDateTime.of(2026, 11, 3, 10, 0));
  }

  @Test
  @DisplayName("키워드가 없거나 공백뿐이면 조건 없이 전부 최신순")
  void noKeywordReturnsAll() {
    assertThat(search(null, null, null, null)).containsExactly(5L, 4L, 3L, 2L, 1L);
    assertThat(search("  ", null, null, null)).containsExactly(5L, 4L, 3L, 2L, 1L);
  }

  @Test
  @DisplayName("키워드는 밴드 이름의 일부만 맞아도 찾고 앞뒤 공백은 무시한다")
  void keywordPartialMatchAndStrip() {
    assertThat(search("  세시 ", null, null, null)).containsExactly(1L);
    assertThat(search("없는밴드", null, null, null)).isEmpty();
  }

  @Test
  @DisplayName("키워드는 대소문자를 구분하지 않는다")
  void keywordIgnoresCase() {
    DummyBandStore latinStore =
        new DummyBandStore(
            List.of(
                new DummyBand(
                    1L,
                    "The Blue",
                    null,
                    List.of(Genre.JAZZ),
                    3,
                    "SEOUL",
                    WantedCareerLevel.Y1_TO_3,
                    LocalDateTime.of(2026, 11, 1, 10, 0))));

    assertThat(ids(latinStore.search("BLUE", null, null, null, FIRST_PAGE))).containsExactly(1L);
    assertThat(ids(latinStore.search("the b", null, null, null, FIRST_PAGE))).containsExactly(1L);
  }

  @Test
  @DisplayName("지역은 같은 밴드만, 공백 지역은 조건 없음")
  void regionFilter() {
    assertThat(search("", "SEOUL", null, null)).containsExactly(4L, 2L, 1L);
    assertThat(search("", " ", null, null)).containsExactly(5L, 4L, 3L, 2L, 1L);
  }

  @Test
  @DisplayName("장르는 밴드 장르 중 하나라도 맞으면 포함한다")
  void genreAnyMatch() {
    assertThat(search("", null, List.of(Genre.INDIE, Genre.PUNK), null)).containsExactly(5L, 1L);
    assertThat(search("", null, List.of(), null)).hasSize(5);
  }

  @Test
  @DisplayName("연주 기간은 목록에 있는 밴드만 포함한다")
  void careerLevelFilter() {
    assertThat(search("", null, null, List.of(WantedCareerLevel.UNDER_1Y))).containsExactly(5L, 2L);
  }

  @Test
  @DisplayName("연주 기간에 ANY가 하나라도 있으면 조건 없음")
  void careerLevelWithAnyIgnoresCondition() {
    assertThat(search("", null, null, List.of(WantedCareerLevel.ANY, WantedCareerLevel.OVER_3Y)))
        .containsExactly(5L, 4L, 3L, 2L, 1L);
  }

  @Test
  @DisplayName("조건은 모두 AND로 묶인다")
  void conditionsAreAnded() {
    assertThat(search("새벽", "SEOUL", List.of(Genre.ROCK), List.of(WantedCareerLevel.Y1_TO_3)))
        .containsExactly(1L);
    assertThat(search("새벽", "BUSAN", null, null)).isEmpty();
  }

  @Test
  @DisplayName("페이지로 자르고 전체 개수는 거른 결과 기준이다")
  void pagination() {
    Page<DummyBand> second = store.search("", null, null, null, PageRequest.of(1, 2));

    assertThat(ids(second)).containsExactly(3L, 2L);
    assertThat(second.getTotalElements()).isEqualTo(5);
    assertThat(second.getTotalPages()).isEqualTo(3);
    assertThat(second.hasNext()).isTrue();

    Page<DummyBand> last = store.search("", null, null, null, PageRequest.of(2, 2));
    assertThat(ids(last)).containsExactly(1L);
    assertThat(last.hasNext()).isFalse();
  }

  @Test
  @DisplayName("범위 밖 페이지는 빈 목록이고 전체 개수는 그대로다")
  void pageBeyondRangeIsEmpty() {
    Page<DummyBand> page = store.search("", "SEOUL", null, null, PageRequest.of(4, 10));

    assertThat(page.getContent()).isEmpty();
    assertThat(page.getTotalElements()).isEqualTo(3);
  }
}
