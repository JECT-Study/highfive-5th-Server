package com.ject.highfive.user.dto;

import com.ject.highfive.code.entity.GenreCode;
import com.ject.highfive.code.entity.PositionCode;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.List;
import java.util.regex.Pattern;

// 직접 입력 값 규칙 (PM 11/19): 앞뒤 공백을 뺀 1~10자, 한글·영문·숫자만 (닉네임과 같은 문자 규칙).
// 해당 코드를 골랐으면 필수, 고르지 않았으면 보내지 않는다 (null 또는 빈 값)
class CustomNamesValidator implements ConstraintValidator<ValidCustomNames, ProfileUpdateRequest> {

  static final int MAX_LENGTH = 10;
  private static final Pattern ALLOWED = Pattern.compile("^[가-힣a-zA-Z0-9]+$");

  @Override
  public boolean isValid(ProfileUpdateRequest request, ConstraintValidatorContext context) {
    context.disableDefaultConstraintViolation();
    boolean genreValid =
        check(
            context,
            "genreCustomName",
            "장르 기타(ETC)",
            contains(request.genreCodes(), GenreCode.ETC.name()),
            request.genreCustomName());
    boolean positionValid =
        check(
            context,
            "positionCustomName",
            "포지션 직접입력(CUSTOM)",
            contains(request.positionCodes(), PositionCode.CUSTOM.name()),
            request.positionCustomName());
    return genreValid && positionValid;
  }

  private boolean check(
      ConstraintValidatorContext context,
      String field,
      String option,
      boolean selected,
      String value) {
    String trimmed = value == null ? "" : value.trim();
    if (!selected) {
      return trimmed.isEmpty() || reject(context, field, option + "을 고르지 않았으면 보내지 않습니다.");
    }
    if (trimmed.isEmpty()) {
      return reject(context, field, option + "을 고르면 직접 입력 값이 필요합니다.");
    }
    if (trimmed.length() > MAX_LENGTH || !ALLOWED.matcher(trimmed).matches()) {
      return reject(context, field, "직접 입력 값은 1~10자의 한글·영문·숫자여야 합니다.");
    }
    return true;
  }

  private static boolean contains(List<String> codes, String code) {
    return codes != null && codes.contains(code);
  }

  private static boolean reject(ConstraintValidatorContext context, String field, String message) {
    context
        .buildConstraintViolationWithTemplate(message)
        .addPropertyNode(field)
        .addConstraintViolation();
    return false;
  }
}
