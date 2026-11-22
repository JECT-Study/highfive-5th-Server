package com.ject.highfive.concert.dummy;

import com.ject.highfive.concert.entity.Genre;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

// 밴드 엔티티(#4, 밴드 담당)가 생기기 전까지 쓰는 임시 밴드 목록. DB 테이블은 없다.
// 공연을 만들 때 bandId를 1~5로 보내면 상세·라인업에 밴드가 붙는다. JECT-253에서 패키지째 지운다
@Component
public class DummyBandStore {

  private static final Map<Long, DummyBand> BANDS =
      Stream.of(
              new DummyBand(1L, "새벽세시", null, List.of(Genre.ROCK, Genre.INDIE), 4),
              new DummyBand(2L, "노을", null, List.of(Genre.POP), 3),
              new DummyBand(3L, "소음공장", null, List.of(Genre.ROCK), 4),
              new DummyBand(4L, "블루노트", null, List.of(Genre.JAZZ), 5),
              new DummyBand(5L, "철가방", null, List.of(Genre.METAL, Genre.PUNK), 4))
          .collect(Collectors.toUnmodifiableMap(DummyBand::id, Function.identity()));

  public Map<Long, DummyBand> findAllByIds(Collection<Long> ids) {
    return ids.stream()
        .distinct()
        .filter(BANDS::containsKey)
        .collect(Collectors.toMap(Function.identity(), BANDS::get));
  }
}
