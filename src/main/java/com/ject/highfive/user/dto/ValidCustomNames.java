package com.ject.highfive.user.dto;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// 장르 기타(ETC)·포지션 직접입력(CUSTOM)과 직접 입력 값의 짝을 검사한다.
// 오류는 genreCustomName·positionCustomName 필드에 붙어 COMMON_004 errors로 나간다
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = CustomNamesValidator.class)
@interface ValidCustomNames {

  String message() default "직접 입력 값이 올바르지 않습니다.";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
