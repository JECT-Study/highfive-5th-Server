package com.ject.highfive.user.dto;

import com.ject.highfive.user.entity.User;
import com.ject.highfive.user.entity.UserGenre;
import com.ject.highfive.user.entity.UserPosition;
import com.ject.highfive.user.entity.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Objects;

public record MyInfoResponse(
    Long id,
    UserStatus status,
    @Schema(description = "프로필 생성 전에는 null", nullable = true) String nickname,
    @Schema(description = "없으면 null (앱이 기본 이미지 표시)", nullable = true) String profileImageUrl,
    @Schema(description = "선호 장르 코드 목록 (여러 개 선택). 프로필 생성 전에는 빈 배열") List<String> genreCodes,
    @Schema(description = "연주 포지션 코드 목록 (여러 개 선택). 프로필 생성 전에는 빈 배열") List<String> positionCodes,
    @Schema(description = "장르 \"기타\"(ETC) 직접 입력 값. ETC를 고르지 않았으면 null", nullable = true)
        String genreCustomName,
    @Schema(description = "포지션 \"직접입력\"(CUSTOM) 값. CUSTOM을 고르지 않았으면 null", nullable = true)
        String positionCustomName,
    @Schema(description = "ACTIVE 상태로 속한 밴드가 있는지") boolean hasBand,
    NextStep nextStep) {

  public static MyInfoResponse of(
      User user,
      List<UserGenre> genres,
      List<UserPosition> positions,
      boolean hasBand,
      NextStep nextStep) {
    return new MyInfoResponse(
        user.getId(),
        user.getStatus(),
        user.getNickname(),
        user.getProfileImageUrl(),
        genres.stream().map(genre -> genre.getGenreCode().name()).toList(),
        positions.stream().map(position -> position.getPositionCode().name()).toList(),
        customNameOf(genres.stream().map(UserGenre::getCustomName).toList()),
        customNameOf(positions.stream().map(UserPosition::getCustomName).toList()),
        hasBand,
        nextStep);
  }

  // 직접 입력 값은 ETC·CUSTOM 행 하나에만 있다
  private static String customNameOf(List<String> customNames) {
    return customNames.stream().filter(Objects::nonNull).findFirst().orElse(null);
  }
}
