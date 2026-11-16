package com.ject.highfive.code.controller;

import com.ject.highfive.code.dto.CodeResponse;
import com.ject.highfive.code.dto.CodeType;
import com.ject.highfive.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// TODO: 장르·포지션 목록이 팀 공통으로 확정되면 서버 enum 기반으로 교체 (JECT-214)
@Tag(name = "공통", description = "코드 목록·이미지 업로드")
@RestController
@RequestMapping("/api/v1/codes")
public class CodeController {

  private static final Map<CodeType, List<CodeResponse>> MOCK_CODES =
      Map.of(
          CodeType.GENRE,
          List.of(
              new CodeResponse("ROCK", "록"),
              new CodeResponse("POP", "팝"),
              new CodeResponse("JAZZ", "재즈")),
          CodeType.POSITION,
          List.of(
              new CodeResponse("VOCAL", "보컬"),
              new CodeResponse("GUITAR", "기타"),
              new CodeResponse("BASS", "베이스"),
              new CodeResponse("DRUM", "드럼"),
              new CodeResponse("KEYBOARD", "키보드")));

  @Operation(
      summary = "코드 목록 조회",
      description =
          "선호 장르·연주 포지션 선택지의 코드와 표시 이름을 돌려준다. 화면에는 name, 저장 API에는 code를 쓴다.\n\n"
              + "에러: COMMON_004(types 누락·알 수 없는 타입)\n\n"
              + "**현재는 가짜 응답 (목록은 임시값)**")
  @GetMapping
  public ApiResponse<Map<CodeType, List<CodeResponse>>> getCodes(
      @Parameter(description = "가져올 코드 종류 (쉼표 구분)", example = "GENRE,POSITION") @RequestParam
          List<CodeType> types) {
    Map<CodeType, List<CodeResponse>> codes = new EnumMap<>(CodeType.class);
    types.forEach(type -> codes.put(type, MOCK_CODES.get(type)));
    return ApiResponse.success(codes);
  }
}
