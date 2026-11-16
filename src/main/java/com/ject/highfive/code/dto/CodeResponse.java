package com.ject.highfive.code.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record CodeResponse(
    @Schema(description = "저장·전송에 쓰는 값") String code,
    @Schema(description = "화면 표시 이름") String name) {}
