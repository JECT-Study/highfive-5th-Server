package com.ject.highfive.code.controller;

import com.ject.highfive.code.dto.CodeResponse;
import com.ject.highfive.code.dto.CodeType;
import com.ject.highfive.code.entity.GenreCode;
import com.ject.highfive.code.entity.PositionCode;
import com.ject.highfive.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "공통", description = "코드 목록·이미지 업로드")
@RestController
@RequestMapping("/api/v1/codes")
public class CodeController {

  // 서버 enum이 원본이다. 앱은 화면에 name, 저장 API에 code를 쓴다
  private static final Map<CodeType, List<CodeResponse>> CODES =
      Map.of(
          CodeType.GENRE,
          Arrays.stream(GenreCode.values())
              .map(genre -> new CodeResponse(genre.name(), genre.getDisplayName()))
              .toList(),
          CodeType.POSITION,
          Arrays.stream(PositionCode.values())
              .map(position -> new CodeResponse(position.name(), position.getDisplayName()))
              .toList());

  @Operation(
      summary = "코드 목록 조회",
      description =
          "선호 장르·연주 포지션 선택지의 코드와 표시 이름을 돌려준다. 화면에는 name, 저장 API에는 code를 쓴다. "
              + "장르 기타(ETC)·포지션 직접입력(CUSTOM)을 고르면 프로필 저장 때 직접 입력 값을 함께 보낸다.\n\n"
              + "에러: COMMON_004(types 누락·빈 값·알 수 없는 타입)")
  @GetMapping
  public ApiResponse<Map<CodeType, List<CodeResponse>>> getCodes(
      // ?types= 는 빈 목록으로, ?types=GENRE, 는 null 원소로 바인딩되므로 둘 다 막는다
      @Parameter(description = "가져올 코드 종류 (쉼표 구분)", example = "GENRE,POSITION")
          @RequestParam
          @NotEmpty(message = "코드 종류는 필수입니다.")
          List<@NotNull(message = "코드 종류가 비어 있습니다.") CodeType> types) {
    Map<CodeType, List<CodeResponse>> codes = new EnumMap<>(CodeType.class);
    types.forEach(type -> codes.put(type, CODES.get(type)));
    return ApiResponse.success(codes);
  }
}
