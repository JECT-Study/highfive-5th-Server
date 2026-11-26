package com.ject.highfive.recruit.controller;

import com.ject.highfive.global.common.response.ApiResponse;
import com.ject.highfive.recruit.service.RecruitPostFavoriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "모집 공고 찜", description = "공고 찜·찜 해제")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/recruit-posts/{recruitPostId}/favorite")
public class RecruitPostFavoriteController {

  private final RecruitPostFavoriteService favoriteService;

  @Operation(
      summary = "공고 찜",
      description =
          "공고를 찜한다. 이미 찜했어도 성공한다. 마감된 공고도 찜할 수 있다.\n\n"
              + "에러: COMMON_004(숫자가 아닌 공고 ID), RECRUIT_001(공고 없음, 삭제 포함)")
  @PutMapping
  public ApiResponse<Map<String, Object>> favorite(
      @Parameter(description = "모집 공고 ID") @PathVariable Long recruitPostId,
      @AuthenticationPrincipal Long userId) {
    favoriteService.favorite(recruitPostId, userId);
    return ApiResponse.success();
  }

  @Operation(
      summary = "공고 찜 해제",
      description = "공고 찜을 해제한다. 찜하지 않았거나 삭제된 공고여도 성공한다.\n\n" + "에러: COMMON_004(숫자가 아닌 공고 ID)")
  @DeleteMapping
  public ApiResponse<Map<String, Object>> unfavorite(
      @Parameter(description = "모집 공고 ID") @PathVariable Long recruitPostId,
      @AuthenticationPrincipal Long userId) {
    favoriteService.unfavorite(recruitPostId, userId);
    return ApiResponse.success();
  }
}
