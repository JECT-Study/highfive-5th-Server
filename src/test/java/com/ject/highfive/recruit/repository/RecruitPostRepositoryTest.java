package com.ject.highfive.recruit.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ject.highfive.concert.entity.Concert;
import com.ject.highfive.concert.entity.Genre;
import com.ject.highfive.concert.repository.ConcertRepository;
import com.ject.highfive.recruit.entity.RecruitPost;
import com.ject.highfive.recruit.entity.TeamType;
import com.ject.highfive.recruit.entity.WantedCareerLevel;
import com.ject.highfive.support.MySqlRepositoryTest;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

@MySqlRepositoryTest
class RecruitPostRepositoryTest {

  @Autowired RecruitPostRepository recruitPostRepository;
  @Autowired ConcertRepository concertRepository;
  @Autowired TestEntityManager em;

  private Concert savedConcert() {
    return concertRepository.saveAndFlush(
        Concert.builder()
            .hostBandId(3L)
            .createdBy(1L)
            .title("겨울 합동공연")
            .recruitTeamCount(3)
            .performDate(LocalDate.of(2999, 12, 20))
            .genres(List.of(Genre.ROCK))
            .build());
  }

  private RecruitPost newPost(Concert concert, TeamType... teamTypes) {
    return RecruitPost.builder()
        .concert(concert)
        .authorUserId(10L)
        .wantedCareerLevel(WantedCareerLevel.Y1_TO_3)
        .teamTypes(List.of(teamTypes))
        .deadline(LocalDate.of(2999, 12, 1))
        .description("12월 홍대에서 함께할 팀을 찾습니다.")
        .build();
  }

  @SuppressWarnings("unchecked")
  private List<String> teamTypeRows(Long postId) {
    return em.getEntityManager()
        .createNativeQuery(
            "select team_type from recruit_post_team_types"
                + " where post_id = ? order by team_type")
        .setParameter(1, postId)
        .getResultList();
  }

  @Test
  @DisplayName("공고를 저장하면 생성·수정 시각과 팀 유형 행이 생긴다")
  void savePersistsPostAndTeamTypes() {
    RecruitPost saved =
        recruitPostRepository.saveAndFlush(
            newPost(savedConcert(), TeamType.FIRST_PERFORMANCE, TeamType.ORIGINAL_SONGS));
    em.clear();

    RecruitPost found = recruitPostRepository.findById(saved.getId()).orElseThrow();
    assertThat(found.getCreatedAt()).isNotNull();
    assertThat(found.getUpdatedAt()).isNotNull();
    assertThat(found.getDeletedAt()).isNull();
    assertThat(teamTypeRows(saved.getId())).containsExactly("FIRST_PERFORMANCE", "ORIGINAL_SONGS");
  }

  @Test
  @DisplayName("DB에서 다시 읽어도 팀 유형은 선언 순서로 나온다")
  void teamTypesReloadInDeclarationOrder() {
    RecruitPost saved =
        recruitPostRepository.saveAndFlush(
            newPost(savedConcert(), TeamType.ANYONE, TeamType.ACTIVE, TeamType.FIRST_PERFORMANCE));
    em.clear();

    assertThat(recruitPostRepository.findById(saved.getId()).orElseThrow().getTeamTypes())
        .containsExactly(TeamType.FIRST_PERFORMANCE, TeamType.ACTIVE, TeamType.ANYONE);
  }

  @Test
  @DisplayName("같은 공연에 공고를 두 번 저장하면 UNIQUE 제약 위반")
  void secondPostForSameConcertViolatesUnique() {
    Concert concert = savedConcert();
    recruitPostRepository.saveAndFlush(newPost(concert, TeamType.ANYONE));

    assertThatThrownBy(() -> recruitPostRepository.saveAndFlush(newPost(concert, TeamType.HOBBY)))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  @DisplayName("공고가 있는 공연만 existsByConcertId가 true다")
  void existsByConcertId() {
    Concert withPost = savedConcert();
    Concert withoutPost = savedConcert();
    recruitPostRepository.saveAndFlush(newPost(withPost, TeamType.ANYONE));

    assertThat(recruitPostRepository.existsByConcertId(withPost.getId())).isTrue();
    assertThat(recruitPostRepository.existsByConcertId(withoutPost.getId())).isFalse();
  }

  @Test
  @DisplayName("팀 유형을 바꾸면 빠진 유형의 행은 지워지고 새 유형의 행이 생긴다")
  void updateReplacesTeamTypeRows() {
    RecruitPost post =
        recruitPostRepository.saveAndFlush(
            newPost(savedConcert(), TeamType.FIRST_PERFORMANCE, TeamType.HOBBY));

    post.update(
        WantedCareerLevel.ANY,
        List.of(TeamType.HOBBY, TeamType.ANYONE),
        LocalDate.of(2999, 12, 5),
        "소개를 고쳤습니다.");
    em.flush();
    em.clear();

    assertThat(teamTypeRows(post.getId())).containsExactly("ANYONE", "HOBBY");
  }

  @Test
  @DisplayName("삭제한 공고는 findByIdAndDeletedAtIsNull로 찾을 수 없지만 existsByConcertId는 찾는다")
  void deletedPostHiddenButStillBlocksNewPost() {
    Concert concert = savedConcert();
    RecruitPost deleted = recruitPostRepository.saveAndFlush(newPost(concert, TeamType.ANYONE));

    deleted.delete();
    em.flush();
    em.clear();
    RecruitPost live = recruitPostRepository.saveAndFlush(newPost(savedConcert(), TeamType.ANYONE));

    assertThat(recruitPostRepository.findByIdAndDeletedAtIsNull(deleted.getId())).isEmpty();
    assertThat(recruitPostRepository.findByIdAndDeletedAtIsNull(live.getId())).isPresent();
    assertThat(recruitPostRepository.existsByConcertId(concert.getId())).isTrue();
  }
}
