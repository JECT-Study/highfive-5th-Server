package com.ject.highfive.global.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class EnumValueValidator implements ConstraintValidator<EnumValue, String> {

  private Set<String> allowedNames;

  @Override
  public void initialize(EnumValue annotation) {
    allowedNames =
        Arrays.stream(annotation.enumClass().getEnumConstants())
            .map(Enum::name)
            .collect(Collectors.toUnmodifiableSet());
  }

  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    // 빈 값은 @NotBlank가 따로 알려 주므로 여기서는 통과시켜 같은 필드에 오류가 두 번 붙지 않게 한다
    return value == null || value.isBlank() || allowedNames.contains(value);
  }
}
