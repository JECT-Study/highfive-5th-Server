package com.ject.highfive.user.dto;

import com.ject.highfive.code.entity.GenreCode;
import com.ject.highfive.code.entity.PositionCode;
import com.ject.highfive.global.common.validation.EnumValue;
import com.ject.highfive.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.LinkedHashSet;
import java.util.List;

// 코드는 문자열로 받는다: 없는 코드도 JSON 파싱 오류(COMMON_001)가 아니라 COMMON_004 + errors로 알려 주려고
@ValidCustomNames
public record ProfileUpdateRequest(
    @Schema(description = "2~10자, 한글·영문·숫자만. 영문 대소문자 구분", example = "두현")
        @NotBlank(message = "닉네임은 필수입니다.")
        @Size(
            min = User.NICKNAME_MIN_LENGTH,
            max = User.NICKNAME_MAX_LENGTH,
            message = "닉네임은 2~10자여야 합니다.")
        @Pattern(regexp = User.NICKNAME_REGEX, message = "닉네임은 한글·영문·숫자만 쓸 수 있습니다.")
        String nickname,
    @Schema(
            description = "코드 목록 조회 API의 GENRE 코드 (여러 개, 개수 제한 없음)",
            example = "[\"ROCK\", \"ETC\"]")
        @NotEmpty(message = "선호 장르를 1개 이상 골라 주세요.")
        List<
                @NotBlank(message = "장르 코드가 비어 있습니다.")
                @EnumValue(enumClass = GenreCode.class, message = "없는 장르 코드입니다.") String>
            genreCodes,
    @Schema(
            description = "코드 목록 조회 API의 POSITION 코드 (여러 개, 개수 제한 없음)",
            example = "[\"VOCAL\", \"CUSTOM\"]")
        @NotEmpty(message = "연주 포지션을 1개 이상 골라 주세요.")
        List<
                @NotBlank(message = "포지션 코드가 비어 있습니다.")
                @EnumValue(enumClass = PositionCode.class, message = "없는 포지션 코드입니다.") String>
            positionCodes,
    @Schema(
            description = "장르 \"기타\"(ETC) 직접 입력 값 (최대 10자). genreCodes에 ETC가 있을 때만 보내고 없으면 null",
            nullable = true,
            example = "시티팝")
        String genreCustomName,
    @Schema(
            description =
                "포지션 \"직접입력\"(CUSTOM) 값 (최대 10자). positionCodes에 CUSTOM이 있을 때만 보내고 없으면 null",
            nullable = true,
            example = "하모니카")
        String positionCustomName,
    @Schema(
            description = "업로드 URL 발급 API로 올린 이미지의 fileUrl (500자 이하). 사진을 건너뛰면 null",
            nullable = true)
        @Size(max = User.PROFILE_IMAGE_URL_MAX_LENGTH, message = "프로필 사진 주소는 500자 이하여야 합니다.")
        String profileImageUrl) {

  // 검증을 통과한 뒤에만 부른다. 중복은 처음 나온 순서대로 하나만 남긴다
  public List<GenreCode> genres() {
    return new LinkedHashSet<>(genreCodes).stream().map(GenreCode::valueOf).toList();
  }

  public List<PositionCode> positions() {
    return new LinkedHashSet<>(positionCodes).stream().map(PositionCode::valueOf).toList();
  }

  // 빈 문자열은 사진을 건너뛴 것(null)으로 본다
  public String profileImageUrlOrNull() {
    return profileImageUrl == null || profileImageUrl.isBlank() ? null : profileImageUrl;
  }

  // 직접 입력 값은 앞뒤 공백을 빼고, 해당 코드를 고르지 않았으면 null
  public String trimmedGenreCustomName() {
    return genres().contains(GenreCode.ETC) ? genreCustomName.trim() : null;
  }

  public String trimmedPositionCustomName() {
    return positions().contains(PositionCode.CUSTOM) ? positionCustomName.trim() : null;
  }
}
