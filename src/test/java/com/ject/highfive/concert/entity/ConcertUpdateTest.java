package com.ject.highfive.concert.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ConcertUpdateTest {

  private Concert concert() {
    return Concert.builder()
        .hostBandId(3L)
        .createdBy(1L)
        .title("겨울 합동공연")
        .recruitTeamCount(3)
        .performDate(LocalDate.of(2999, 12, 20))
        .startTime(LocalTime.of(19, 0))
        .endTime(LocalTime.of(21, 0))
        .place("홍대 롤링홀")
        .address("서울 마포구 어울마당로 35")
        .region("SEOUL")
        .rentalFee(500000)
        .posterImageUrl("https://cdn.example.com/posters/1.jpg")
        .genres(List.of(Genre.ROCK, Genre.INDIE))
        .build();
  }

  @Test
  @DisplayName("요청한 값으로 모든 필드를 바꾸고 수정자를 기록한다")
  void updateReplacesAllFields() {
    Concert concert = concert();

    concert.update(
        "봄 합동공연",
        List.of(Genre.JAZZ, Genre.POP),
        5,
        LocalDate.of(2999, 3, 1),
        LocalTime.of(18, 0),
        LocalTime.of(20, 30),
        "신촌 클럽",
        "서울 서대문구 신촌로 1",
        "GYEONGGI",
        300000,
        "https://cdn.example.com/posters/2.jpg",
        20L);

    assertThat(concert.getTitle()).isEqualTo("봄 합동공연");
    assertThat(concert.getGenres()).containsExactly(Genre.POP, Genre.JAZZ);
    assertThat(concert.getRecruitTeamCount()).isEqualTo(5);
    assertThat(concert.getPerformDate()).isEqualTo(LocalDate.of(2999, 3, 1));
    assertThat(concert.getStartTime()).isEqualTo(LocalTime.of(18, 0));
    assertThat(concert.getEndTime()).isEqualTo(LocalTime.of(20, 30));
    assertThat(concert.getPlace()).isEqualTo("신촌 클럽");
    assertThat(concert.getAddress()).isEqualTo("서울 서대문구 신촌로 1");
    assertThat(concert.getRegion()).isEqualTo("GYEONGGI");
    assertThat(concert.getRentalFee()).isEqualTo(300000);
    assertThat(concert.getPosterImageUrl()).isEqualTo("https://cdn.example.com/posters/2.jpg");
    assertThat(concert.getUpdatedBy()).isEqualTo(20L);
  }

  @Test
  @DisplayName("선택 값을 null로 보내면 기존 값을 지우고 미정으로 바꾼다 (전체 교체)")
  void nullOptionalValuesClearExisting() {
    Concert concert = concert();

    concert.update(
        "겨울 합동공연", List.of(Genre.ROCK), 3, null, null, null, null, null, null, null, null, 20L);

    assertThat(concert.getPerformDate()).isNull();
    assertThat(concert.getStartTime()).isNull();
    assertThat(concert.getEndTime()).isNull();
    assertThat(concert.getPlace()).isNull();
    assertThat(concert.getAddress()).isNull();
    assertThat(concert.getRegion()).isNull();
    assertThat(concert.getRentalFee()).isNull();
    assertThat(concert.getPosterImageUrl()).isNull();
  }

  @Test
  @DisplayName("주최 밴드, 만든 사람, 상태, 신청 링크, 삭제 시각은 바꾸지 않는다")
  void updateKeepsFixedFields() {
    Concert concert = concert();

    concert.update(
        "봄 합동공연", List.of(Genre.JAZZ), 5, null, null, null, null, null, null, null, null, 20L);

    assertThat(concert.getHostBandId()).isEqualTo(3L);
    assertThat(concert.getCreatedBy()).isEqualTo(1L);
    assertThat(concert.getStatus()).isEqualTo(ConcertStatus.PREPARING);
    assertThat(concert.getApplyUrl()).isNull();
    assertThat(concert.getDeletedAt()).isNull();
  }

  @Test
  @DisplayName("장르가 비어 있으면 거부하고 다른 필드도 바꾸지 않는다")
  void invalidGenresLeaveConcertUnchanged() {
    Concert concert = concert();

    assertThatThrownBy(
            () ->
                concert.update(
                    "봄 합동공연", List.of(), 5, null, null, null, null, null, null, null, null, 20L))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(concert.getTitle()).isEqualTo("겨울 합동공연");
    assertThat(concert.getRecruitTeamCount()).isEqualTo(3);
    assertThat(concert.getUpdatedBy()).isEqualTo(1L);
    assertThat(concert.getGenres()).containsExactly(Genre.ROCK, Genre.INDIE);
  }
}
