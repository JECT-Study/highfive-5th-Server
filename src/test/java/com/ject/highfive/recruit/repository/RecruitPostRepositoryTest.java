package com.ject.highfive.recruit.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ject.highfive.concert.entity.Concert;
import com.ject.highfive.concert.repository.ConcertRepository;
import com.ject.highfive.recruit.entity.RecruitPost;
import com.ject.highfive.recruit.entity.TeamType;
import com.ject.highfive.recruit.entity.WantedCareerLevel;
import com.ject.highfive.support.ConcertFixture;
import com.ject.highfive.support.MySqlRepositoryTest;
import java.time.LocalDate;
import java.util.List;
import org.hibernate.Hibernate;
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
    return concertRepository.saveAndFlush(ConcertFixture.concert());
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
  @DisplayName("같은 공연에 살아 있는 공고를 두 번 저장하면 UNIQUE 제약 위반")
  void secondLivePostForSameConcertViolatesUnique() {
    Concert concert = savedConcert();
    recruitPostRepository.saveAndFlush(newPost(concert, TeamType.ANYONE));

    assertThatThrownBy(() -> recruitPostRepository.saveAndFlush(newPost(concert, TeamType.HOBBY)))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  @DisplayName("공고를 두 번 지운 공연에도 새 공고를 저장할 수 있고, 살아 있는 공고만 active_concert_id를 갖는다")
  void rewriteAfterDeletingTwice() {
    Concert concert = savedConcert();
    RecruitPost first = saveAndDelete(concert);
    RecruitPost second = saveAndDelete(concert);

    RecruitPost third = recruitPostRepository.saveAndFlush(newPost(concert, TeamType.ANYONE));
    em.clear();

    assertThat(List.of(first.getId(), second.getId(), third.getId())).doesNotHaveDuplicates();
    assertThat(reload(first).getActiveConcertId()).isNull();
    assertThat(reload(second).getActiveConcertId()).isNull();
    assertThat(reload(third).getActiveConcertId()).isEqualTo(concert.getId());
    assertThat(
            recruitPostRepository
                .findByConcertIdAndDeletedAtIsNull(concert.getId())
                .map(RecruitPost::getId))
        .contains(third.getId());
  }

  private RecruitPost saveAndDelete(Concert concert) {
    RecruitPost post = recruitPostRepository.saveAndFlush(newPost(concert, TeamType.ANYONE));
    post.delete();
    em.flush();
    return post;
  }

  private RecruitPost reload(RecruitPost post) {
    return recruitPostRepository.findById(post.getId()).orElseThrow();
  }

  @Test
  @DisplayName("existsByConcertIdAndDeletedAtIsNull은 살아 있는 공고가 있는 공연만 true다")
  void existsByConcertIdAndDeletedAtIsNull() {
    Concert withPost = savedConcert();
    Concert withDeletedPost = savedConcert();
    final Concert withoutPost = savedConcert();
    recruitPostRepository.saveAndFlush(newPost(withPost, TeamType.ANYONE));
    saveAndDelete(withDeletedPost);

    assertThat(recruitPostRepository.existsByConcertIdAndDeletedAtIsNull(withPost.getId()))
        .isTrue();
    assertThat(recruitPostRepository.existsByConcertIdAndDeletedAtIsNull(withDeletedPost.getId()))
        .isFalse();
    assertThat(recruitPostRepository.existsByConcertIdAndDeletedAtIsNull(withoutPost.getId()))
        .isFalse();
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
  @DisplayName("삭제한 공고는 findByIdAndDeletedAtIsNull로 찾을 수 없다")
  void deletedPostHidden() {
    Concert concert = savedConcert();
    RecruitPost deleted = recruitPostRepository.saveAndFlush(newPost(concert, TeamType.ANYONE));

    deleted.delete();
    em.flush();
    em.clear();
    RecruitPost live = recruitPostRepository.saveAndFlush(newPost(savedConcert(), TeamType.ANYONE));

    assertThat(recruitPostRepository.findByIdAndDeletedAtIsNull(deleted.getId())).isEmpty();
    assertThat(recruitPostRepository.findByIdAndDeletedAtIsNull(live.getId())).isPresent();
  }

  @Test
  @DisplayName("삭제하지 않은 공고만 findByConcertIdAndDeletedAtIsNull로 찾는다")
  void findByConcertIdAndDeletedAtIsNull() {
    Concert withDeletedPost = savedConcert();
    recruitPostRepository.saveAndFlush(newPost(withDeletedPost, TeamType.ANYONE)).delete();
    em.flush();
    em.clear();
    Concert withLivePost = savedConcert();
    RecruitPost livePost =
        recruitPostRepository.saveAndFlush(newPost(withLivePost, TeamType.ANYONE));

    assertThat(
            recruitPostRepository
                .findByConcertIdAndDeletedAtIsNull(withLivePost.getId())
                .map(RecruitPost::getId))
        .contains(livePost.getId());
    Concert withoutPost = savedConcert();
    assertThat(recruitPostRepository.findByConcertIdAndDeletedAtIsNull(withDeletedPost.getId()))
        .isEmpty();
    assertThat(recruitPostRepository.findByConcertIdAndDeletedAtIsNull(withoutPost.getId()))
        .isEmpty();
  }

  @Test
  @DisplayName("existsByIdAndDeletedAtIsNull은 마감된 공고는 참, 삭제된 공고와 없는 공고는 거짓")
  void existsByIdAndDeletedAtIsNull() {
    RecruitPost closed =
        recruitPostRepository.saveAndFlush(newPost(savedConcert(), TeamType.ANYONE));
    closed.close();
    RecruitPost deleted =
        recruitPostRepository.saveAndFlush(newPost(savedConcert(), TeamType.ANYONE));
    deleted.delete();
    em.flush();
    em.clear();

    assertThat(recruitPostRepository.existsByIdAndDeletedAtIsNull(closed.getId())).isTrue();
    assertThat(recruitPostRepository.existsByIdAndDeletedAtIsNull(deleted.getId())).isFalse();
    assertThat(recruitPostRepository.existsByIdAndDeletedAtIsNull(Long.MAX_VALUE)).isFalse();
  }

  @Test
  @DisplayName("상세 조회는 살아 있는 공고를 공연과 함께 읽는다")
  void findDetailLoadsConcert() {
    RecruitPost saved =
        recruitPostRepository.saveAndFlush(newPost(savedConcert(), TeamType.ANYONE));
    em.clear();

    RecruitPost found =
        recruitPostRepository
            .findWithConcertByIdAndDeletedAtIsNullAndConcertDeletedAtIsNull(saved.getId())
            .orElseThrow();

    assertThat(Hibernate.isInitialized(found.getConcert())).isTrue();
  }

  @Test
  @DisplayName("상세 조회는 삭제한 공고를 찾지 않는다")
  void deletedPostNotFound() {
    RecruitPost post = recruitPostRepository.saveAndFlush(newPost(savedConcert(), TeamType.ANYONE));
    post.delete();
    em.flush();
    em.clear();

    assertThat(
            recruitPostRepository.findWithConcertByIdAndDeletedAtIsNullAndConcertDeletedAtIsNull(
                post.getId()))
        .isEmpty();
  }

  @Test
  @DisplayName("상세 조회는 공연이 삭제된 공고를 찾지 않는다")
  void concertDeletedPostNotFound() {
    Concert concert = savedConcert();
    RecruitPost post = recruitPostRepository.saveAndFlush(newPost(concert, TeamType.ANYONE));
    // Concert에는 삭제 메서드가 아직 없다(1.6 미구현)
    em.getEntityManager()
        .createNativeQuery("update concerts set deleted_at = now() where id = ?")
        .setParameter(1, concert.getId())
        .executeUpdate();
    em.clear();

    assertThat(
            recruitPostRepository.findWithConcertByIdAndDeletedAtIsNullAndConcertDeletedAtIsNull(
                post.getId()))
        .isEmpty();
  }
}
