package com.ject.highfive.recruit.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.ject.highfive.concert.entity.Concert;
import com.ject.highfive.concert.entity.Genre;
import com.ject.highfive.concert.repository.ConcertRepository;
import com.ject.highfive.recruit.entity.RecruitPost;
import com.ject.highfive.recruit.entity.TeamType;
import com.ject.highfive.recruit.entity.WantedCareerLevel;
import com.ject.highfive.support.MySqlRepositoryTest;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@MySqlRepositoryTest
class RecruitPostSpecificationsTest {

  private static final Sort LATEST = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

  @Autowired RecruitPostRepository recruitPostRepository;
  @Autowired ConcertRepository concertRepository;
  @Autowired TestEntityManager em;

  private Concert concert(String title, String region, Genre... genres) {
    return concertRepository.saveAndFlush(
        Concert.builder()
            .hostBandId(3L)
            .createdBy(1L)
            .title(title)
            .recruitTeamCount(3)
            .performDate(LocalDate.of(2999, 12, 20))
            .region(region)
            .genres(List.of(genres))
            .build());
  }

  private RecruitPost post(Concert concert, WantedCareerLevel level, String description) {
    return recruitPostRepository.saveAndFlush(
        RecruitPost.builder()
            .concert(concert)
            .authorUserId(10L)
            .wantedCareerLevel(level)
            .teamTypes(List.of(TeamType.ANYONE))
            .deadline(LocalDate.of(2999, 12, 1))
            .description(description)
            .build());
  }

  private RecruitPost post(Concert concert) {
    return post(concert, WantedCareerLevel.Y1_TO_3, "함께할 팀을 찾습니다.");
  }

  private Page<RecruitPost> search(
      String region,
      Collection<Genre> genres,
      Collection<WantedCareerLevel> levels,
      String keyword,
      int pageIndex,
      int size) {
    em.clear();
    return recruitPostRepository.findAll(
        RecruitPostSpecifications.search(region, genres, levels, keyword),
        PageRequest.of(pageIndex, size, LATEST));
  }

  private List<Long> ids(
      String region,
      Collection<Genre> genres,
      Collection<WantedCareerLevel> levels,
      String keyword) {
    return search(region, genres, levels, keyword, 0, 50).map(RecruitPost::getId).getContent();
  }

  @Test
  @DisplayName("조건이 없으면 삭제되지 않은 공고 전부를 최신 등록순으로 돌려준다")
  void noConditionReturnsAllLatestFirst() {
    RecruitPost first = post(concert("공연1", "SEOUL", Genre.ROCK));
    RecruitPost second = post(concert("공연2", "BUSAN", Genre.POP));
    RecruitPost third = post(concert("공연3", "SEOUL", Genre.JAZZ));

    assertThat(ids(null, null, null, null))
        .containsExactly(third.getId(), second.getId(), first.getId());
  }

  @Test
  @DisplayName("삭제된 공고와 삭제된 공연의 공고는 빠지고, 마감된 공고는 남는다")
  void deletedPostsAndConcertsExcluded() {
    final RecruitPost alive = post(concert("살아 있음", "SEOUL", Genre.ROCK));
    RecruitPost closed = post(concert("마감", "SEOUL", Genre.ROCK));
    closed.close();
    RecruitPost deletedPost = post(concert("공고 삭제", "SEOUL", Genre.ROCK));
    deletedPost.delete();
    Concert deletedConcert = concert("공연 삭제", "SEOUL", Genre.ROCK);
    post(deletedConcert);
    em.flush();
    // Concert에는 삭제 메서드가 아직 없다(1.6 미구현)
    em.getEntityManager()
        .createNativeQuery("update concerts set deleted_at = now() where id = ?")
        .setParameter(1, deletedConcert.getId())
        .executeUpdate();

    assertThat(ids(null, null, null, null)).containsExactly(closed.getId(), alive.getId());
  }

  @Test
  @DisplayName("지역은 같은 값만 남긴다")
  void regionFilter() {
    RecruitPost seoul = post(concert("공연1", "SEOUL", Genre.ROCK));
    post(concert("공연2", "BUSAN", Genre.ROCK));

    assertThat(ids("SEOUL", null, null, null)).containsExactly(seoul.getId());
  }

  @Test
  @DisplayName("장르는 하나라도 맞으면 포함하고, 여러 장르가 맞아도 공고는 한 번만 나온다")
  void genreFilterMatchesAnyWithoutDuplicates() {
    RecruitPost rockIndie = post(concert("공연1", "SEOUL", Genre.ROCK, Genre.INDIE));
    RecruitPost indie = post(concert("공연2", "SEOUL", Genre.INDIE));
    post(concert("공연3", "SEOUL", Genre.JAZZ));

    Page<RecruitPost> page = search(null, List.of(Genre.ROCK, Genre.INDIE), null, null, 0, 50);

    assertThat(page.map(RecruitPost::getId).getContent())
        .containsExactly(indie.getId(), rockIndie.getId());
    assertThat(page.getTotalElements()).isEqualTo(2);
  }

  @Test
  @DisplayName("연주 기간은 고른 값과 '무관(ANY)' 공고를 함께 돌려준다")
  void careerLevelFilterAlwaysIncludesAny() {
    RecruitPost under = post(concert("공연1", "SEOUL", Genre.ROCK), WantedCareerLevel.UNDER_1Y, "소개");
    post(concert("공연2", "SEOUL", Genre.ROCK), WantedCareerLevel.OVER_3Y, "소개");
    RecruitPost any = post(concert("공연3", "SEOUL", Genre.ROCK), WantedCareerLevel.ANY, "소개");

    assertThat(ids(null, null, List.of(WantedCareerLevel.UNDER_1Y), null))
        .containsExactly(any.getId(), under.getId());
  }

  @Test
  @DisplayName("키워드는 공연 제목이나 공고 소개에 들어 있으면 찾고, 앞뒤 공백은 무시한다")
  void keywordMatchesTitleOrDescription() {
    RecruitPost inTitle = post(concert("홍대 겨울 공연", "SEOUL", Genre.ROCK));
    RecruitPost inDescription =
        post(concert("겨울 공연", "SEOUL", Genre.ROCK), WantedCareerLevel.ANY, "홍대에서 만나요");
    post(concert("부산 공연", "BUSAN", Genre.ROCK));

    assertThat(ids(null, null, null, "  홍대 "))
        .containsExactly(inDescription.getId(), inTitle.getId());
  }

  @Test
  @DisplayName("키워드의 %와 _는 와일드카드가 아니라 글자 그대로 찾는다")
  void keywordTreatsWildcardsLiterally() {
    RecruitPost percent = post(concert("100% 라이브", "SEOUL", Genre.ROCK));
    post(concert("1000 라이브", "SEOUL", Genre.ROCK));
    RecruitPost underscore = post(concert("a_b 밴드", "SEOUL", Genre.ROCK));
    post(concert("axb 밴드", "SEOUL", Genre.ROCK));

    assertThat(ids(null, null, null, "0%")).containsExactly(percent.getId());
    assertThat(ids(null, null, null, "a_b")).containsExactly(underscore.getId());
  }

  @Test
  @DisplayName("공백뿐인 키워드는 조건으로 걸지 않는다")
  void blankKeywordIgnored() {
    post(concert("공연1", "SEOUL", Genre.ROCK));
    post(concert("공연2", "SEOUL", Genre.ROCK));

    assertThat(ids(null, null, null, "   ")).hasSize(2);
  }

  @Test
  @DisplayName("조건끼리는 AND로 묶인다")
  void conditionsAreAnded() {
    final RecruitPost match =
        post(concert("홍대 공연", "SEOUL", Genre.ROCK), WantedCareerLevel.UNDER_1Y, "소개");
    post(concert("홍대 공연", "BUSAN", Genre.ROCK), WantedCareerLevel.UNDER_1Y, "소개");
    post(concert("홍대 공연", "SEOUL", Genre.JAZZ), WantedCareerLevel.UNDER_1Y, "소개");
    post(concert("홍대 공연", "SEOUL", Genre.ROCK), WantedCareerLevel.OVER_3Y, "소개");
    post(concert("부산 공연", "SEOUL", Genre.ROCK), WantedCareerLevel.UNDER_1Y, "소개");

    assertThat(ids("SEOUL", List.of(Genre.ROCK), List.of(WantedCareerLevel.UNDER_1Y), "홍대"))
        .containsExactly(match.getId());
  }

  @Test
  @DisplayName("페이지 크기만큼 잘라 주고 전체 개수는 조건에 맞는 공고 수다")
  void pagingAndTotal() {
    post(concert("공연1", "SEOUL", Genre.ROCK));
    RecruitPost second = post(concert("공연2", "SEOUL", Genre.ROCK));
    RecruitPost third = post(concert("공연3", "SEOUL", Genre.ROCK));

    Page<RecruitPost> page = search(null, null, null, null, 0, 2);

    assertThat(page.map(RecruitPost::getId).getContent())
        .containsExactly(third.getId(), second.getId());
    assertThat(page.getTotalElements()).isEqualTo(3);
    assertThat(page.hasNext()).isTrue();
  }
}
