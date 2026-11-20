package com.ject.highfive.recruit.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ject.highfive.concert.entity.Concert;
import com.ject.highfive.concert.entity.ConcertTeam;
import com.ject.highfive.concert.entity.Genre;
import com.ject.highfive.concert.repository.ConcertRepository;
import com.ject.highfive.concert.repository.ConcertTeamRepository;
import com.ject.highfive.global.common.response.PageResponse;
import com.ject.highfive.recruit.dto.RecruitPostSearchRequest;
import com.ject.highfive.recruit.dto.RecruitPostSummaryResponse;
import com.ject.highfive.recruit.entity.RecruitPost;
import com.ject.highfive.recruit.entity.RecruitPostFavorite;
import com.ject.highfive.recruit.entity.TeamType;
import com.ject.highfive.recruit.entity.WantedCareerLevel;
import com.ject.highfive.recruit.repository.RecruitPostFavoriteRepository;
import com.ject.highfive.recruit.repository.RecruitPostRepository;
import com.ject.highfive.support.MySqlContainerConfig;
import jakarta.persistence.EntityManagerFactory;
import java.time.LocalDate;
import java.util.List;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

// 공고 수가 늘어도 쿼리 수가 늘지 않는지(N+1 없음) 실제 MySQL에서 센다 (스펙 4.5)
@SpringBootTest(
    properties = {
      "spring.jpa.properties.hibernate.generate_statistics=true",
      "spring.jpa.show-sql=true"
    })
@Import(MySqlContainerConfig.class)
class RecruitPostQueryServiceQueryCountTest {

  private static final Long USER_ID = 7L;
  private static final RecruitPostSearchRequest NO_CONDITION =
      new RecruitPostSearchRequest(null, null, null, null);

  @Autowired RecruitPostQueryService queryService;
  @Autowired RecruitPostRepository recruitPostRepository;
  @Autowired RecruitPostFavoriteRepository favoriteRepository;
  @Autowired ConcertRepository concertRepository;
  @Autowired ConcertTeamRepository concertTeamRepository;
  @Autowired EntityManagerFactory entityManagerFactory;

  @AfterEach
  void cleanUp() {
    favoriteRepository.deleteAll();
    recruitPostRepository.deleteAll();
    concertTeamRepository.deleteAll();
    concertRepository.deleteAll();
  }

  private void savePosts(int count) {
    for (int i = 0; i < count; i++) {
      Concert concert =
          concertRepository.saveAndFlush(
              Concert.builder()
                  .hostBandId(3L)
                  .createdBy(1L)
                  .title("공연" + i)
                  .recruitTeamCount(3)
                  .performDate(LocalDate.of(2999, 12, 20))
                  .region("SEOUL")
                  .genres(List.of(Genre.ROCK, Genre.INDIE))
                  .build());
      concertTeamRepository.saveAndFlush(ConcertTeam.builder().concert(concert).bandId(3L).build());
      concertTeamRepository.saveAndFlush(
          ConcertTeam.builder().concert(concert).bandId(100L + i).build());
      RecruitPost post =
          recruitPostRepository.saveAndFlush(
              RecruitPost.builder()
                  .concert(concert)
                  .authorUserId(10L)
                  .wantedCareerLevel(WantedCareerLevel.ANY)
                  .teamTypes(List.of(TeamType.ANYONE))
                  .deadline(LocalDate.of(2999, 12, 1))
                  .description("소개")
                  .build());
      favoriteRepository.saveAndFlush(
          RecruitPostFavorite.builder().userId(USER_ID).recruitPost(post).build());
    }
  }

  private long statementsFor(int size) {
    Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
    statistics.clear();
    PageResponse<RecruitPostSummaryResponse> result =
        queryService.search(NO_CONDITION, 1, size, USER_ID);
    assertThat(result.content()).hasSize(size);
    assertThat(result.content())
        .allSatisfy(
            summary -> {
              assertThat(summary.genres()).containsExactly(Genre.ROCK, Genre.INDIE);
              assertThat(summary.joinedTeamCount()).isEqualTo(1);
              assertThat(summary.isFavorite()).isTrue();
            });
    return statistics.getPrepareStatementCount();
  }

  @Test
  @DisplayName("공고 2개를 읽을 때와 8개를 읽을 때 쿼리 수가 같고 5번이다")
  void statementCountDoesNotGrowWithPageSize() {
    savePosts(10);

    long small = statementsFor(2);
    long large = statementsFor(8);

    // 공고+공연, 개수, 장르, 합류 팀 수, 찜 (결과가 size만큼 차서 개수 쿼리도 나간다)
    assertThat(small).isEqualTo(5);
    assertThat(large).isEqualTo(small);
  }
}
