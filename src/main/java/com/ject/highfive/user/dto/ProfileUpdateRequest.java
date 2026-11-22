package com.ject.highfive.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

// TODO: 코드 존재 여부, ETC·CUSTOM과 직접 입력 값의 짝, 직접 입력 값 형식(앞뒤 공백 제거 후 1~10자,
//  한글·영문·숫자)은 서비스에서 검증한다 (JECT-214). 없는 코드도 COMMON_004로 응답하려고 코드는 문자열로 받는다
public record ProfileUpdateRequest(
    @Schema(description = "2~10자, 한글·영문·숫자만. 영문 대소문자 구분", example = "두현")
        @NotBlank(message = "닉네임은 필수입니다.")
        @Size(min = 2, max = 10, message = "닉네임은 2~10자여야 합니다.")
        @Pattern(regexp = "^[가-힣a-zA-Z0-9]+$", message = "닉네임은 한글·영문·숫자만 쓸 수 있습니다.")
        String nickname,
    @Schema(
            description = "코드 목록 조회 API의 GENRE 코드 (여러 개, 개수 제한 없음)",
            example = "[\"ROCK\", \"ETC\"]")
        @NotEmpty(message = "선호 장르를 1개 이상 골라 주세요.")
        List<@NotBlank(message = "장르 코드가 비어 있습니다.") String> genreCodes,
    @Schema(
            description = "코드 목록 조회 API의 POSITION 코드 (여러 개, 개수 제한 없음)",
            example = "[\"VOCAL\", \"CUSTOM\"]")
        @NotEmpty(message = "연주 포지션을 1개 이상 골라 주세요.")
        List<@NotBlank(message = "포지션 코드가 비어 있습니다.") String> positionCodes,
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
    @Schema(description = "업로드 URL 발급 API로 올린 이미지의 fileUrl. 사진을 건너뛰면 null", nullable = true)
        String profileImageUrl) {}
