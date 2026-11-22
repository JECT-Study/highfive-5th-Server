package com.ject.highfive.auth.dto;

import com.ject.highfive.user.dto.NextStep;
import com.ject.highfive.user.entity.User;
import com.ject.highfive.user.entity.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;

public record LoginResponse(
    @Schema(description = "서비스 액세스 토큰 (JWT, 30분)") String accessToken,
    @Schema(description = "서비스 리프레시 토큰 (JWT, 14일)") String refreshToken,
    LoginUser user,
    @Schema(description = "앱이 이동할 화면") NextStep nextStep) {

  public record LoginUser(
      Long id,
      UserStatus status,
      @Schema(description = "프로필 생성 전에는 null", nullable = true) String nickname) {

    public static LoginUser from(User user) {
      return new LoginUser(user.getId(), user.getStatus(), user.getNickname());
    }
  }
}
