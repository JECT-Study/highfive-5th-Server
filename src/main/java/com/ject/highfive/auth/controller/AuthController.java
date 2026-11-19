package com.ject.highfive.auth.controller;

import com.ject.highfive.auth.dto.LoginRequest;
import com.ject.highfive.auth.dto.LoginResponse;
import com.ject.highfive.auth.dto.TokenRefreshRequest;
import com.ject.highfive.auth.dto.TokenResponse;
import com.ject.highfive.auth.service.AuthService;
import com.ject.highfive.global.common.response.ApiResponse;
import com.ject.highfive.user.dto.NextStep;
import com.ject.highfive.user.entity.UserStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "인증", description = "소셜 로그인·토큰 재발급·로그아웃")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

  // TODO: 소셜 ID 토큰 검증과 회원 가입을 붙이면 실제 사용자로 바꾼다 (JECT-212)
  private static final Long MOCK_LOGIN_USER_ID = 1L;

  private final AuthService authService;

  @Operation(
      summary = "소셜 로그인",
      description =
          "앱 SDK로 받은 소셜 ID 토큰을 검증하고 서비스 토큰을 발급한다. 처음이면 가입(PENDING)까지 처리한다.\n\n"
              + "에러: AUTH_006(소셜 토큰 검증 실패), AUTH_007(지원하지 않는 provider), COMMON_004\n\n"
              + "**임시: 소셜 토큰을 검증하지 않고 항상 1번 사용자로 실제 서비스 토큰을 발급한다 (JECT-212에서 교체)**")
  @SecurityRequirements
  @PostMapping("/login/{provider}")
  public ApiResponse<LoginResponse> login(
      @Parameter(
              description = "로그인한 소셜 서비스 (애플은 iOS만)",
              schema = @Schema(allowableValues = {"kakao", "google", "apple"}))
          @PathVariable
          String provider,
      @RequestBody @Valid LoginRequest request) {
    TokenResponse tokens = authService.issueTokens(MOCK_LOGIN_USER_ID);
    LoginResponse response =
        new LoginResponse(
            tokens.accessToken(),
            tokens.refreshToken(),
            new LoginResponse.LoginUser(MOCK_LOGIN_USER_ID, UserStatus.PENDING, null),
            NextStep.TERMS);
    return ApiResponse.success(response);
  }

  @Operation(
      summary = "토큰 재발급",
      description =
          "리프레시 토큰으로 액세스·리프레시 토큰을 둘 다 새로 발급한다. 앱은 AUTH_002를 받았을 때만 호출한다.\n\n"
              + "에러: AUTH_004(리프레시 토큰 만료·불일치, 다른 기기 로그인, 이미 쓴 토큰), COMMON_004")
  @SecurityRequirements
  @PostMapping("/refresh")
  public ApiResponse<TokenResponse> refresh(@RequestBody @Valid TokenRefreshRequest request) {
    return ApiResponse.success(authService.refresh(request.refreshToken()));
  }

  @Operation(
      summary = "로그아웃",
      description =
          "서버에 저장된 리프레시 토큰을 삭제한다. 앱은 응답과 상관없이 저장된 토큰을 지운다.\n\n"
              + "에러: AUTH_001, AUTH_002, AUTH_003")
  @PostMapping("/logout")
  public ApiResponse<Map<String, Object>> logout(@AuthenticationPrincipal Long userId) {
    authService.logout(userId);
    return ApiResponse.success();
  }
}
