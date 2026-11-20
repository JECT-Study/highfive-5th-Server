package com.ject.highfive.recruit.controller;

import com.ject.highfive.concert.entity.Genre;
import com.ject.highfive.global.common.response.ApiResponse;
import com.ject.highfive.global.common.response.PageResponse;
import com.ject.highfive.recruit.dto.RecruitPostCreateResponse;
import com.ject.highfive.recruit.dto.RecruitPostRequest;
import com.ject.highfive.recruit.dto.RecruitPostSearchRequest;
import com.ject.highfive.recruit.dto.RecruitPostSummaryResponse;
import com.ject.highfive.recruit.entity.WantedCareerLevel;
import com.ject.highfive.recruit.service.RecruitPostQueryService;
import com.ject.highfive.recruit.service.RecruitPostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "모집 공고", description = "모집 공고 작성·수정·마감·삭제·목록")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class RecruitPostController {

  private static final int DEFAULT_PAGE_SIZE = 10;
  private static final int MAX_PAGE_SIZE = 100;

  private final RecruitPostService recruitPostService;
  private final RecruitPostQueryService recruitPostQueryService;

  @Operation(
      summary = "모집 공고 목록·검색",
      description =
          "공고를 최신 등록순으로 페이지 단위로 돌려준다. 삭제된 공고·공연은 빠지고 마감 공고는 포함된다.\n\n"
              + "- region: 하나. genre·careerLevel: 여러 개(같은 이름 반복). 장르는 하나라도 맞으면 포함\n"
              + "- careerLevel로 거르면 무관(ANY) 공고는 항상 포함\n"
              + "- keyword: 공연 제목·공고 소개에서 찾음, 최대 50자\n"
              + "- page는 1부터(1보다 작으면 1)·최대 10000, size 기본 10·최대 100(넘으면 100)\n\n"
              + "에러: COMMON_004(목록에 없는 genre·careerLevel 코드, 50자 넘는 키워드, "
              + "10000 넘는 page, 숫자가 아닌 page·size)\n\n"
              + "**임시: `X-User-Id` 헤더 필수 (로그인 연동 전). isMine은 아직 항상 false**")
  @GetMapping("/recruit-posts")
  public ApiResponse<PageResponse<RecruitPostSummaryResponse>> search(
      @Parameter(description = "공연 지역 코드", example = "SEOUL") @RequestParam(required = false)
          String region,
      @Parameter(description = "장르 코드, 여러 개 가능") @RequestParam(required = false) List<Genre> genre,
      @Parameter(description = "모집 팀 연주 기간 코드, 여러 개 가능") @RequestParam(required = false)
          List<WantedCareerLevel> careerLevel,
      @Parameter(description = "검색어(공연 제목·공고 소개)")
          @RequestParam(required = false)
          @Size(max = 50, message = "검색어는 50자 이하여야 합니다.")
          String keyword,
      // offset((page-1)*size)이 int를 넘으면 Spring Data가 예외를 던져 500이 나므로 상한을 둔다
      @Parameter(description = "페이지(1부터, 최대 10000)")
          @RequestParam(defaultValue = "1")
          @Max(value = 10000, message = "페이지는 10000 이하여야 합니다.")
          int page,
      @Parameter(description = "페이지 크기(최대 100)") @RequestParam(defaultValue = "10") int size,
      // TODO: JECT-249 로그인 연동(JECT-211) 후 로그인 사용자로 바꾼다
      @Parameter(description = "임시: 로그인 연동 전 사용자 ID", example = "1") @RequestHeader("X-User-Id")
          Long userId) {
    return ApiResponse.success(
        recruitPostQueryService.search(
            new RecruitPostSearchRequest(region, genre, careerLevel, keyword),
            Math.max(page, 1),
            pageSize(size),
            userId));
  }

  // 컨벤션의 페이징 규칙(size 기본 10, 최대 100)을 이 API에서 맞춘다. 전역 설정은 팀이 정할 때 (스펙 4.3)
  private static int pageSize(int size) {
    if (size < 1) {
      return DEFAULT_PAGE_SIZE;
    }
    return Math.min(size, MAX_PAGE_SIZE);
  }

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
