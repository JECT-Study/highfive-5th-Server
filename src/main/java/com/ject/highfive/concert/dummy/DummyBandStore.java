package com.ject.highfive.concert.dummy;

import com.ject.highfive.concert.entity.Genre;
import com.ject.highfive.recruit.entity.WantedCareerLevel;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

// 밴드 엔티티(#4, 밴드 담당)가 생기기 전까지 쓰는 임시 밴드 목록. DB 테이블은 없다.
// 공연을 만들 때 bandId를 1~5로 보내면 상세·라인업에 밴드가 붙고, 같은 번호의 사용자(1~5)가 그 밴드의 멤버다. JECT-253에서 패키지째 지운다
@Component
public class DummyBandStore {

  private static final List<DummyBand> FIXED_BANDS =
      List.of(
          new DummyBand(
              1L,
              "새벽세시",
              null,
              List.of(Genre.ROCK, Genre.INDIE),
              4,
              "SEOUL",
              WantedCareerLevel.Y1_TO_3,
              LocalDateTime.of(2026, 11, 1, 10, 0),
              Set.of(1L)),
          new DummyBand(
              2L,
              "노을",
              null,
              List.of(Genre.POP),
              3,
              "SEOUL",
              WantedCareerLevel.UNDER_1Y,
              LocalDateTime.of(2026, 11, 2, 10, 0),
              Set.of(2L)),
          new DummyBand(
              3L,
              "소음공장",
              null,
              List.of(Genre.ROCK),
              4,
              "GYEONGGI",
              WantedCareerLevel.OVER_3Y,
              LocalDateTime.of(2026, 11, 3, 10, 0),
              Set.of(3L)),
          new DummyBand(
              4L,
              "블루노트",
              null,
              List.of(Genre.JAZZ),
              5,
              "SEOUL",
              WantedCareerLevel.Y1_TO_3,
              LocalDateTime.of(2026, 11, 4, 10, 0),
              Set.of(4L)),
          new DummyBand(
              5L,
              "철가방",
              null,
              List.of(Genre.METAL, Genre.PUNK),
              4,
              "BUSAN",
              WantedCareerLevel.UNDER_1Y,
              LocalDateTime.of(2026, 11, 5, 10, 0),
              Set.of(5L)));

  // 2.5 공고 목록과 같은 최신순. 같은 시각이면 id로 고정한다
  private static final Comparator<DummyBand> LATEST =
      Comparator.comparing(DummyBand::createdAt).thenComparing(DummyBand::id).reversed();

  private final Map<Long, DummyBand> bands;

  public DummyBandStore() {
    this(FIXED_BANDS);
  }

  // 고정 목록은 이름이 모두 한글이라 대소문자 무시를 확인하려고 테스트에서 목록을 바꿔 넣는다
  DummyBandStore(List<DummyBand> bands) {
    this.bands =
        bands.stream().collect(Collectors.toUnmodifiableMap(DummyBand::id, Function.identity()));
  }

  public Map<Long, DummyBand> findAllByIds(Collection<Long> ids) {
    return ids.stream()
        .distinct()
        .filter(bands::containsKey)
        .collect(Collectors.toMap(Function.identity(), bands::get));
  }

  // 참여 팀 멤버 확인(JECT-226)의 임시 멤버십. 밴드 1~5의 멤버가 각각 사용자 1~5다
  public Set<Long> findBandIdsByUserId(Long userId) {
    return bands.values().stream()
        .filter(band -> band.memberUserIds().contains(userId))
        .map(DummyBand::id)
        .collect(Collectors.toUnmodifiableSet());
  }

  // 팀 검색(2.6). 규칙은 225 스펙 4.2. 대소문자 무시는 MySQL 기본 콜레이션(ai_ci)과 맞춰 253 뒤에도 결과가 같게 한다
  public Page<DummyBand> search(
      String keyword,
      String region,
      Collection<Genre> genres,
      Collection<WantedCareerLevel> careerLevels,
      Pageable pageable) {
    // 키워드가 없으면 빈 문자열로 두어 모든 이름이 걸리게 한다
    String loweredKeyword = keyword == null ? "" : keyword.strip().toLowerCase(Locale.ROOT);
    List<DummyBand> matched =
        bands.values().stream()
            .filter(band -> band.name().toLowerCase(Locale.ROOT).contains(loweredKeyword))
            .filter(band -> region == null || region.isBlank() || band.region().equals(region))
            .filter(
                band ->
                    genres == null
                        || genres.isEmpty()
                        || band.genres().stream().anyMatch(genres::contains))
            .filter(
                band ->
                    careerLevels == null
                        || careerLevels.isEmpty()
                        || careerLevels.contains(WantedCareerLevel.ANY)
                        || careerLevels.contains(band.careerLevel()))
            .sorted(LATEST)
            .toList();
    int from = (int) Math.min(pageable.getOffset(), matched.size());
    int to = Math.min(from + pageable.getPageSize(), matched.size());
    return new PageImpl<>(matched.subList(from, to), pageable, matched.size());
  }
}
