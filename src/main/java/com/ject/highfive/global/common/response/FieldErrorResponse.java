package com.ject.highfive.global.common.response;

// 입력값 검증 실패 시 errors 배열의 항목
public record FieldErrorResponse(String field, String reason) {}
