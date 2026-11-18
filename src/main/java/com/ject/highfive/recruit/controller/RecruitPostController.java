package com.ject.highfive.recruit.controller;

import com.ject.highfive.global.common.response.ApiResponse;
import com.ject.highfive.recruit.dto.RecruitPostCreateResponse;
import com.ject.highfive.recruit.dto.RecruitPostRequest;
import com.ject.highfive.recruit.service.RecruitPostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "모집 공고", description = "모집 공고 작성·수정·마감·삭제")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class RecruitPostController {

  private final RecruitPostService recruitPostService;

  @Operation(
      summary = "모집 공고 작성",
      description =
          "공연의 모집 공고를 작성한다. 공연당 하나.\n\n"
              + "에러: COMMON_004(필수값·500자), COMMON_001(목록에 없는 코드), "
              + "RECRUIT_003(마감일이 오늘 이전), CONCERT_001(공연 없음), RECRUIT_006(이미 지난 공연), "
              + "RECRUIT_005(마감일이 공연 날짜보다 늦음), "
              + "RECRUIT_002(이미 공고가 있음, 삭제한 공고 포함)\n\n"
              + "**임시: `X-User-Id` 헤더 필수 (로그인 연동 전). 주최 대표자 확인은 아직 하지 않는다**")
  @PostMapping("/concerts/{concertId}/recruit-posts")
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<RecruitPostCreateResponse> create(
      @Parameter(description = "공연 ID") @PathVariable Long concertId,
      // TODO: JECT-249 로그인 연동(JECT-211) 후 로그인 사용자로 바꾼다
      @Parameter(description = "임시: 로그인 연동 전 사용자 ID", example = "1") @RequestHeader("X-User-Id")
          Long userId,
      @RequestBody @Valid RecruitPostRequest request) {
    // 본문 status는 ApiResponseStatusAdvice가 201로 맞춘다
    return ApiResponse.success(recruitPostService.create(concertId, userId, request));
  }

  @Operation(
      summary = "모집 공고 수정",
      description =
          "공고 내용을 전체 교체(PUT)한다. 본문은 작성과 같다.\n\n"
              + "에러: COMMON_004(필수값·500자, 숫자가 아닌 공고 ID), COMMON_001(목록에 없는 코드), "
              + "RECRUIT_003(마감일이 오늘 이전), RECRUIT_001(공고 없음, 삭제 포함), "
              + "RECRUIT_004(이미 마감된 공고: 직접 마감, 마감일 지남 모두), "
              + "RECRUIT_005(마감일이 공연 날짜보다 늦음)\n\n"
              + "**임시: `X-User-Id` 헤더 필수 (로그인 연동 전). 주최 대표자 확인은 아직 하지 않는다**")
  @PutMapping("/recruit-posts/{recruitPostId}")
  public ApiResponse<Map<String, Object>> update(
      @Parameter(description = "모집 공고 ID") @PathVariable Long recruitPostId,
      // TODO: JECT-249 로그인 연동(JECT-211) 후 로그인 사용자로 바꾼다
      @Parameter(description = "임시: 로그인 연동 전 사용자 ID", example = "1") @RequestHeader("X-User-Id")
          Long userId,
      @RequestBody @Valid RecruitPostRequest request) {
    recruitPostService.update(recruitPostId, userId, request);
    return ApiResponse.success();
  }

  @Operation(
      summary = "모집 마감",
      description =
          "공고를 직접 마감한다. 다시 여는 기능은 없다.\n\n"
              + "에러: RECRUIT_001(공고 없음, 삭제 포함), RECRUIT_004(이미 마감된 공고, 마감일 지남 포함)\n\n"
              + "**임시: `X-User-Id` 헤더 필수 (로그인 연동 전). 주최 대표자 확인은 아직 하지 않는다**")
  @PostMapping("/recruit-posts/{recruitPostId}/close")
  public ApiResponse<Map<String, Object>> close(
      @Parameter(description = "모집 공고 ID") @PathVariable Long recruitPostId,
      // TODO: JECT-249 로그인 연동(JECT-211) 후 로그인 사용자로 바꾼다
      @Parameter(description = "임시: 로그인 연동 전 사용자 ID", example = "1") @RequestHeader("X-User-Id")
          Long userId) {
    recruitPostService.close(recruitPostId, userId);
    return ApiResponse.success();
  }

  @Operation(
      summary = "모집 공고 삭제",
      description =
          "공고를 삭제한다. 마감된 공고도 지울 수 있다. 공고를 지운 공연에는 다시 작성할 수 없다.\n\n"
              + "에러: RECRUIT_001(공고 없음, 이미 삭제 포함)\n\n"
              + "**임시: `X-User-Id` 헤더 필수 (로그인 연동 전). 주최 대표자 확인은 아직 하지 않는다**")
  @DeleteMapping("/recruit-posts/{recruitPostId}")
  public ApiResponse<Map<String, Object>> delete(
      @Parameter(description = "모집 공고 ID") @PathVariable Long recruitPostId,
      // TODO: JECT-249 로그인 연동(JECT-211) 후 로그인 사용자로 바꾼다
      @Parameter(description = "임시: 로그인 연동 전 사용자 ID", example = "1") @RequestHeader("X-User-Id")
          Long userId) {
    recruitPostService.delete(recruitPostId, userId);
    return ApiResponse.success();
  }
}
