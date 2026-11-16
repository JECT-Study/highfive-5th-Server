package com.ject.highfive.user.controller;

import com.ject.highfive.global.common.response.ApiResponse;
import com.ject.highfive.user.dto.MyInfoResponse;
import com.ject.highfive.user.dto.NextStep;
import com.ject.highfive.user.dto.NicknameAvailabilityResponse;
import com.ject.highfive.user.dto.ProfileUpdateRequest;
import com.ject.highfive.user.dto.ProfileUpdateResponse;
import com.ject.highfive.user.dto.TermAgreementRequest;
import com.ject.highfive.user.dto.TermAgreementResponse;
import com.ject.highfive.user.entity.UserStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// TODO: 가짜 응답을 실제 구현으로 교체 (JECT-213, JECT-214)
@Tag(name = "사용자", description = "내 정보·약관 동의·개인 프로필")
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

  @Operation(
      summary = "내 정보 조회",
      description =
          "앱 실행 시 회원 상태·프로필과 이동할 화면(nextStep)을 돌려준다. PENDING 회원도 호출할 수 있다.\n\n"
              + "에러: AUTH_001, AUTH_002, AUTH_003, USER_005(사용자 없음)\n\n"
              + "**현재는 가짜 응답**")
  @GetMapping("/me")
  public ApiResponse<MyInfoResponse> getMyInfo() {
    MyInfoResponse response =
        new MyInfoResponse(
            1L, UserStatus.ACTIVE, "두현", null, "ROCK", "GUITAR", false, NextStep.BAND_START);
    return ApiResponse.success(response);
  }

  @Operation(
      summary = "약관 동의",
      description =
          "최신 약관 각각의 동의·거부를 저장하고 다음 화면을 돌려준다. 약관 개정 후 재동의에도 쓴다.\n\n"
              + "에러: USER_003(필수 약관 미동의), COMMON_004(필드 누락, 최신이 아닌 termId)\n\n"
              + "**현재는 가짜 응답**")
  @PostMapping("/me/term-agreements")
  public ApiResponse<TermAgreementResponse> agreeTerms(
      @RequestBody @Valid TermAgreementRequest request) {
    return ApiResponse.success(new TermAgreementResponse(NextStep.PROFILE));
  }

  @Operation(
      summary = "닉네임 사용 가능 여부",
      description =
          "닉네임 형식과 중복을 확인한다. 중복·형식 오류도 에러가 아니라 200 + available: false로 돌려준다.\n\n"
              + "에러: COMMON_004(nickname 파라미터 없음)\n\n"
              + "**현재는 가짜 응답 (항상 사용 가능)**")
  @GetMapping("/nickname-availability")
  public ApiResponse<NicknameAvailabilityResponse> checkNicknameAvailability(
      @Parameter(description = "확인할 닉네임 (URL 인코딩)") @RequestParam String nickname) {
    return ApiResponse.success(new NicknameAvailabilityResponse(true, null));
  }

  @Operation(
      summary = "개인 프로필 저장",
      description =
          "닉네임·선호 장르·연주 포지션·사진을 저장한다. 온보딩 중(PENDING)이면 ACTIVE로 바꾼다. 마이페이지 수정에도 쓴다.\n\n"
              + "에러: USER_001(닉네임 중복), USER_002(닉네임 형식), USER_003(필수 약관 미동의), COMMON_004\n\n"
              + "**현재는 가짜 응답**")
  @PutMapping("/me/profile")
  public ApiResponse<ProfileUpdateResponse> updateProfile(
      @RequestBody @Valid ProfileUpdateRequest request) {
    return ApiResponse.success(new ProfileUpdateResponse(UserStatus.ACTIVE, NextStep.BAND_START));
  }
}
