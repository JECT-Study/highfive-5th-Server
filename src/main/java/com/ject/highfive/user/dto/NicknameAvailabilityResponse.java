package com.ject.highfive.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record NicknameAvailabilityResponse(
    boolean available,
    @Schema(
            description = "사용할 수 없는 이유. 사용 가능하면 null",
            nullable = true,
            allowableValues = {INVALID_LENGTH, INVALID_CHARACTER, DUPLICATED})
        String reason) {

  public static final String INVALID_LENGTH = "INVALID_LENGTH";
  public static final String INVALID_CHARACTER = "INVALID_CHARACTER";
  public static final String DUPLICATED = "DUPLICATED";

  public static NicknameAvailabilityResponse usable() {
    return new NicknameAvailabilityResponse(true, null);
  }

  public static NicknameAvailabilityResponse unavailable(String reason) {
    return new NicknameAvailabilityResponse(false, reason);
  }
}
