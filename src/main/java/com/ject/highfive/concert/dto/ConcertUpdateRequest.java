package com.ject.highfive.concert.dto;

import com.ject.highfive.concert.entity.Concert;
import com.ject.highfive.concert.entity.Genre;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

// API 명세 1.3. ConcertCreateRequest에서 bandId를 빼고 version을 더한 필드·검증이다.
// 두 DTO가 어긋나지 않는지는 ConcertControllerTest가 같은 잘못된 값으로 생성·수정을 함께 확인한다
public record ConcertUpdateRequest(
    @Schema(description = "공연 제목", example = "겨울 합동공연")
        @NotBlank(message = "공연 제목은 필수입니다.")
        @Size(max = 30, message = "공연 제목은 30자 이하여야 합니다.")
        String title,
    @Schema(description = "연주 장르 코드 (중복은 하나로 합침)", example = "[\"ROCK\", \"INDIE\"]")
        @NotEmpty(message = "연주 장르를 하나 이상 골라 주세요.")
        List<@NotNull(message = "연주 장르가 비어 있습니다.") Genre> genres,
    @Schema(description = "모집할 팀 수 (주최 팀 제외)", example = "3")
        @NotNull(message = "모집 팀 수는 필수입니다.")
        @Min(value = 1, message = "모집 팀 수는 1 이상이어야 합니다.")
        Integer recruitTeamCount,
    @Schema(
            description = "공연 날짜. null이면 미정. 기존 날짜와 같으면 지난 날짜여도 그대로 둘 수 있음",
            example = "2026-12-20",
            nullable = true)
        LocalDate performDate,
    @Schema(description = "시작 시간. endTime과 함께 보내거나 함께 null", example = "19:00:00", nullable = true)
        LocalTime startTime,
    @Schema(description = "종료 시간. 자정을 넘길 수 있음", example = "21:00:00", nullable = true)
        LocalTime endTime,
    @Schema(description = "장소명", example = "홍대 롤링홀", nullable = true)
        @Size(max = 100, message = "장소명은 100자 이하여야 합니다.")
        String place,
    @Schema(description = "장소 주소", example = "서울 마포구 어울마당로 35", nullable = true)
        @Size(max = 200, message = "장소 주소는 200자 이하여야 합니다.")
        String address,
    @Schema(description = "지역 코드", example = "SEOUL", nullable = true)
        @Size(max = 50, message = "지역 코드는 50자 이하여야 합니다.")
        String region,
    @Schema(description = "전체 대관비(원)", example = "500000", nullable = true)
        @PositiveOrZero(message = "대관비는 0원 이상이어야 합니다.")
        Integer rentalFee,
    @Schema(description = "업로드 URL 발급 API로 올린 이미지의 fileUrl", nullable = true)
        @Size(max = 500, message = "포스터 주소는 500자 이하여야 합니다.")
        String posterImageUrl,
    @Schema(description = "수정 화면을 열 때 공연 조회(1.2)로 받은 version", example = "3")
        @NotNull(message = "version은 필수입니다.")
        Long version) {

  public void applyTo(Concert concert, Long userId) {
    concert.update(
        title,
        genres,
        recruitTeamCount,
        performDate,
        startTime,
        endTime,
        place,
        address,
        region,
        rentalFee,
        posterImageUrl,
        userId);
  }
}
