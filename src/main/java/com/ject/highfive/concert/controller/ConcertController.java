package com.ject.highfive.concert.controller;

import com.ject.highfive.concert.dto.ConcertCreateRequest;
import com.ject.highfive.concert.dto.ConcertCreateResponse;
import com.ject.highfive.concert.dto.ConcertUpdateRequest;
import com.ject.highfive.concert.service.ConcertService;
import com.ject.highfive.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "합동공연", description = "합동공연 생성·수정")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/concerts")
public class ConcertController {

  private final ConcertService concertService;

  @Operation(
      summary = "합동공연 생성",
      description =
          "밴드 대표자가 합동공연을 만든다. 요청한 밴드가 주최 팀(첫 참여 팀)이 된다.\n\n"
              + "에러: COMMON_004(필수값·길이·장르 null 원소), "
              + "COMMON_001(JSON을 읽지 못함: 날짜·시간 형식, 숫자 범위, 목록에 없는 코드), "
              + "CONCERT_010(시작·종료 시간 중 하나만), CONCERT_002(공연 날짜가 오늘 이전)\n\n"
              + "**임시: `X-User-Id` 헤더 필수 (로그인 연동 전). 밴드 대표자 확인은 아직 하지 않는다**")
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<ConcertCreateResponse> create(
      // TODO: JECT-249 로그인 연동(JECT-211) 후 로그인 사용자로 바꾼다
      @Parameter(description = "임시: 로그인 연동 전 사용자 ID", example = "1") @RequestHeader("X-User-Id")
          Long userId,
      @RequestBody @Valid ConcertCreateRequest request) {
    // 본문 status는 ApiResponseStatusAdvice가 201로 맞춘다
    return ApiResponse.success(concertService.create(userId, request));
  }

  @Operation(
      summary = "공연 기본 정보 수정",
      description =
          "참여 팀 멤버가 공연 정보를 전체 교체(PUT)한다. 보내지 않은 선택 값은 미정(null)이 된다.\n\n"
              + "에러: COMMON_004(필수값·길이·장르 null 원소, 숫자가 아닌 공연 ID), "
              + "COMMON_001(JSON을 읽지 못함: 날짜·시간 형식, 숫자 범위, 목록에 없는 코드), "
              + "CONCERT_010(시작·종료 시간 중 하나만), CONCERT_001(공연 없음), "
              + "CONCERT_002(공연 날짜를 오늘 이전으로 바꿈), "
              + "CONCERT_011(공연 날짜가 진행 중인 모집 공고 마감일보다 빠름), "
              + "CONCERT_003(모집 팀 수가 주최 팀을 뺀 합류 팀 수보다 적음)\n\n"
              + "**임시: `X-User-Id` 헤더 필수 (로그인 연동 전). 참여 팀 멤버 확인은 아직 하지 않는다**")
  @PutMapping("/{concertId}")
  public ApiResponse<Map<String, Object>> update(
      @Parameter(description = "공연 ID") @PathVariable Long concertId,
      // TODO: JECT-249 로그인 연동(JECT-211) 후 로그인 사용자로 바꾼다
      @Parameter(description = "임시: 로그인 연동 전 사용자 ID", example = "1") @RequestHeader("X-User-Id")
          Long userId,
      @RequestBody @Valid ConcertUpdateRequest request) {
    concertService.update(concertId, userId, request);
    return ApiResponse.success();
  }
}
