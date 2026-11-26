package com.ject.highfive.recruit.repository;

import com.ject.highfive.concert.entity.Concert;
import com.ject.highfive.concert.entity.Genre;
import com.ject.highfive.recruit.entity.RecruitPost;
import com.ject.highfive.recruit.entity.RecruitPostStatus;
import com.ject.highfive.recruit.entity.WantedCareerLevel;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Stream;
import org.springframework.data.jpa.domain.Specification;

// 공고 목록(2.5) 조건. 값이 없으면 null을 돌려 조건을 건너뛴다
public final class RecruitPostSpecifications {

  // MySQL 문자열에서 \는 그 자체가 이스케이프라 쓰지 않는다
  private static final char LIKE_ESCAPE = '!';

  private RecruitPostSpecifications() {}

  public static Specification<RecruitPost> search(
      String region,
      Collection<Genre> genres,
      Collection<WantedCareerLevel> careerLevels,
      String keyword,
      boolean hideClosed,
      LocalDate today) {
    return Stream.of(
            notDeleted(),
            regionEq(region),
            genreIn(genres),
            careerLevelIn(careerLevels),
            keywordContains(keyword),
            openOnly(hideClosed, today))
        .filter(Objects::nonNull)
        .reduce(Specification::and)
        .orElseThrow();
  }

  static Specification<RecruitPost> notDeleted() {
    return (root, query, cb) ->
        cb.and(cb.isNull(root.get("deletedAt")), cb.isNull(root.get("concert").get("deletedAt")));
  }

  static Specification<RecruitPost> regionEq(String region) {
    if (region == null || region.isBlank()) {
      return null;
    }
    return (root, query, cb) -> cb.equal(root.get("concert").get("region"), region);
  }

  // join하면 공고가 장르 수만큼 중복되어 페이징이 깨지므로 EXISTS로 거른다 (스펙 4.1)
  static Specification<RecruitPost> genreIn(Collection<Genre> genres) {
    if (genres == null || genres.isEmpty()) {
      return null;
    }
    return (root, query, cb) -> {
      Subquery<Long> sub = query.subquery(Long.class);
      Root<Concert> concert = sub.from(Concert.class);
      Join<Concert, Genre> genre = concert.join("genres");
      sub.select(concert.get("id"))
          .where(cb.equal(concert.get("id"), root.get("concert").get("id")), genre.in(genres));
      return cb.exists(sub);
    };
  }

  // 무관(ANY) 공고는 어느 연주 기간 팀이든 지원할 수 있어 항상 포함한다 (스펙 4.1)
  static Specification<RecruitPost> careerLevelIn(Collection<WantedCareerLevel> careerLevels) {
    if (careerLevels == null || careerLevels.isEmpty()) {
      return null;
    }
    return (root, query, cb) ->
        cb.or(
            root.get("wantedCareerLevel").in(careerLevels),
            cb.equal(root.get("wantedCareerLevel"), WantedCareerLevel.ANY));
  }

  // 지역 코드(SEOUL)로는 "마포" 같은 검색어가 잡히지 않아 주소에서 찾는다 (225 스펙 4.5).
  // 주소가 null이면 LIKE가 참이 아니어서 제목으로만 걸린다
  static Specification<RecruitPost> keywordContains(String keyword) {
    if (keyword == null || keyword.isBlank()) {
      return null;
    }
    String pattern = "%" + escapeLike(keyword.strip()) + "%";
    return (root, query, cb) ->
        cb.or(
            cb.like(root.get("concert").get("title"), pattern, LIKE_ESCAPE),
            cb.like(root.get("concert").get("address"), pattern, LIKE_ESCAPE));
  }

  // 응답 status(RecruitPost.isClosed)와 같은 기준. 마감일이 오늘인 공고는 아직 모집 중이다 (D10, 225 스펙 4.6)
  static Specification<RecruitPost> openOnly(boolean hideClosed, LocalDate today) {
    if (!hideClosed) {
      return null;
    }
    return (root, query, cb) ->
        cb.and(
            cb.equal(root.get("status"), RecruitPostStatus.RECRUITING),
            cb.greaterThanOrEqualTo(root.get("deadline"), today));
  }

  private static String escapeLike(String value) {
    return value
        .replace(String.valueOf(LIKE_ESCAPE), "" + LIKE_ESCAPE + LIKE_ESCAPE)
        .replace("%", LIKE_ESCAPE + "%")
        .replace("_", LIKE_ESCAPE + "_");
  }
}
