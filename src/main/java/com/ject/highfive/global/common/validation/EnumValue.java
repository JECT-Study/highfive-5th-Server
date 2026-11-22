package com.ject.highfive.global.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// 문자열이 enum 상수 이름 중 하나인지 검사한다 (null은 통과, 필수 여부는 @NotBlank로).
// enum 타입으로 바로 받으면 없는 값이 JSON 파싱 오류(COMMON_001)가 되므로,
// 문자열로 받고 이 검사로 COMMON_004 + errors(필드별 사유)를 돌려줄 때 쓴다
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = EnumValueValidator.class)
public @interface EnumValue {

  Class<? extends Enum<?>> enumClass();

  String message() default "허용하지 않는 값입니다.";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
