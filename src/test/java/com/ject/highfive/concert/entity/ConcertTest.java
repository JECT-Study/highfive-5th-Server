package com.ject.highfive.concert.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ject.highfive.concert.repository.ConcertRepository;
import com.ject.highfive.support.RepositoryTest;
import jakarta.persistence.PersistenceException;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

@RepositoryTest
class ConcertTest {

  @Autowired ConcertRepository concertRepository;
  @Autowired TestEntityManager em;

  private Concert newConcert(Genre... genres) {
    return Concert.builder()
        .hostBandId(3L)
        .createdBy(1L)
        .title("겨울 합동공연")
        .recruitTeamCount(3)
        .performDate(LocalDate.of(2026, 12, 20))
        .region("SEOUL")
        .place("홍대 롤링홀")
        .address("서울 마포구 어울마당로 35")
        .rentalFee(500000)
        .genres(List.of(genres))
        .build();
  }

  @SuppressWarnings("unchecked")
  private List<String> genreRows(Long concertId) {
    return em.getEntityManager()
        .createNativeQuery(
            "select genre_code from concert_genres" + " where concert_id = ? order by genre_code")
        .setParameter(1, concertId)
        .getResultList();
  }

  private void flushAndClear() {
    em.flush();
    em.clear();
  }

  @Test
  @DisplayName("공연을 저장하면 PREPARING 상태, 생성·수정 시각, 장르 행이 생긴다")
  void saveConcert() {
    Concert saved = concertRepository.saveAndFlush(newConcert(Genre.ROCK, Genre.INDIE));
    em.clear();

    Concert found = concertRepository.findById(saved.getId()).orElseThrow();
    assertThat(found.getStatus()).isEqualTo(ConcertStatus.PREPARING);
    assertThat(found.getCreatedAt()).isNotNull();
    assertThat(found.getUpdatedAt()).isNotNull();
    assertThat(found.getUpdatedBy()).isEqualTo(1L);
    assertThat(found.getDeletedAt()).isNull();
    assertThat(found.getGenres()).containsExactlyInAnyOrder(Genre.ROCK, Genre.INDIE);
    assertThat(genreRows(saved.getId())).containsExactly("INDIE", "ROCK");
  }

  @Test
  @DisplayName("장르 교체: 빠진 장르 행은 지워지고, 새 장르는 추가되고, 유지한 장르는 남는다")
  void changeGenres() {
    Concert concert = concertRepository.saveAndFlush(newConcert(Genre.ROCK, Genre.INDIE));
    Long concertId = concert.getId();

    concert.changeGenres(List.of(Genre.ROCK, Genre.METAL));
    flushAndClear();

    assertThat(genreRows(concertId)).containsExactly("METAL", "ROCK");
    assertThat(concertRepository.findById(concertId).orElseThrow().getGenres())
        .containsExactlyInAnyOrder(Genre.ROCK, Genre.METAL);
  }

  @Test
  @DisplayName("요청 장르에 중복이 있으면 행은 하나만 생긴다")
  void duplicateGenresInInputCreateOneRow() {
    Concert saved = concertRepository.saveAndFlush(newConcert(Genre.ROCK, Genre.ROCK));
    em.clear();

    assertThat(genreRows(saved.getId())).containsExactly("ROCK");
  }

  @Test
  @DisplayName("장르가 비어 있으면 생성·교체 모두 거부한다")
  void emptyGenresRejected() {
    assertThatThrownBy(() -> newConcert()).isInstanceOf(IllegalArgumentException.class);

    Concert concert = newConcert(Genre.ROCK);
    assertThatThrownBy(() -> concert.changeGenres(List.of()))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("getGenres()로 받은 Set은 고칠 수 없다")
  void genresViewIsReadOnly() {
    Concert concert = newConcert(Genre.ROCK);

    assertThatThrownBy(() -> concert.getGenres().add(Genre.JAZZ))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  @DisplayName("같은 공연에 같은 장르 행을 직접 두 번 넣으면 PK 위반")
  void duplicateGenreRowViolatesPrimaryKey() {
    Concert concert = concertRepository.saveAndFlush(newConcert(Genre.ROCK));

    assertThatThrownBy(
            () ->
                em.getEntityManager()
                    .createNativeQuery(
                        "insert into concert_genres (concert_id, genre_code) values (?, 'ROCK')")
                    .setParameter(1, concert.getId())
                    .executeUpdate())
        .isInstanceOf(PersistenceException.class);
  }

  @Test
  @DisplayName("update는 값을 전부 교체하고 미정(null)도 반영하며 created_by는 그대로 둔다")
  void updateReplacesAllValues() {
    Concert concert = concertRepository.saveAndFlush(newConcert(Genre.ROCK));
    Long concertId = concert.getId();

    concert.update(
        2L, "새 제목", 5, null, null, null, null, null, null, null, null, List.of(Genre.JAZZ));
    flushAndClear();

    Concert found = concertRepository.findById(concertId).orElseThrow();
    assertThat(found.getTitle()).isEqualTo("새 제목");
    assertThat(found.getRecruitTeamCount()).isEqualTo(5);
    assertThat(found.getPerformDate()).isNull();
    assertThat(found.getRegion()).isNull();
    assertThat(found.getPlace()).isNull();
    assertThat(found.getAddress()).isNull();
    assertThat(found.getRentalFee()).isNull();
    assertThat(found.getUpdatedBy()).isEqualTo(2L);
    assertThat(found.getCreatedBy()).isEqualTo(1L);
    assertThat(found.getGenres()).containsExactly(Genre.JAZZ);
  }
}
