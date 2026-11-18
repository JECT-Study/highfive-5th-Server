package com.ject.highfive.concert.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.ject.highfive.concert.entity.Concert;
import com.ject.highfive.concert.entity.Genre;
import com.ject.highfive.support.MySqlRepositoryTest;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

@MySqlRepositoryTest
class ConcertRepositoryTest {

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

  // 공연 삭제 메서드가 아직 없어 컬럼을 직접 채운다
  private void softDelete(Long concertId) {
    em.getEntityManager()
        .createNativeQuery("update concerts set deleted_at = now() where id = ?")
        .setParameter(1, concertId)
        .executeUpdate();
    em.clear();
  }

  @Test
  @DisplayName("삭제하지 않은 공연만 findByIdAndDeletedAtIsNull로 찾는다")
  void findByIdAndDeletedAtIsNull() {
    Concert live = savedConcert();
    Concert deleted = savedConcert();
    softDelete(deleted.getId());

    assertThat(concertRepository.findByIdAndDeletedAtIsNull(live.getId())).isPresent();
    assertThat(concertRepository.findByIdAndDeletedAtIsNull(deleted.getId())).isEmpty();
    assertThat(concertRepository.findById(deleted.getId())).isPresent();
  }
}
