package com.ject.highfive.user.controller;

import com.ject.highfive.global.common.response.ApiResponse;
import com.ject.highfive.user.dto.MyInfoResponse;
import com.ject.highfive.user.dto.NicknameAvailabilityResponse;
import com.ject.highfive.user.dto.ProfileUpdateRequest;
import com.ject.highfive.user.dto.ProfileUpdateResponse;
import com.ject.highfive.user.dto.TermAgreementRequest;
import com.ject.highfive.user.dto.TermAgreementResponse;
import com.ject.highfive.user.service.TermService;
import com.ject.highfive.user.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "사용자", description = "내 정보·약관 동의·개인 프로필")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {

  private final UserProfileService userProfileService;
  private final TermService termService;

  @Operation(
      summary = "내 정보 조회",
      description =
          "앱 실행 시 회원 상태·프로필과 이동할 화면(nextStep)을 돌려준다. PENDING 회원도 호출할 수 있다.\n\n"
              + "에러: AUTH_001, AUTH_002, AUTH_003, USER_005(사용자 없음)\n\n"
              + "필수 약관이 개정되면 ACTIVE 회원도 다시 TERMS가 된다. "
              + "밴드 기능 전까지 hasBand는 항상 false라 프로필 저장 후에는 BAND_START")
  @GetMapping("/me")
  public ApiResponse<MyInfoResponse> getMyInfo(@AuthenticationPrincipal Long userId) {
    return ApiResponse.success(userProfileService.getMyInfo(userId));
  }

  @Operation(
      summary = "약관 동의",
      description =
          "약관 목록 조회에서 받은 약관 각각의 동의·거부를 저장하고 다음 화면을 돌려준다. "
              + "약관 개정 후 재동의에도 쓴다. 회원 상태는 바꾸지 않는다(ACTIVE는 프로필 저장 때).\n\n"
              + "에러: USER_003(필수 약관 누락·거부), USER_005(사용자 없음), "
              + "COMMON_004(필드 누락, 없는 약관·지난 버전·중복 termId)")
  @PostMapping("/me/term-agreements")
  public ApiResponse<TermAgreementResponse> agreeTerms(
      @AuthenticationPrincipal Long userId, @RequestBody @Valid TermAgreementRequest request) {
    return ApiResponse.success(termService.agree(userId, request));
  }

  @Operation(
      summary = "닉네임 사용 가능 여부",
      description =
          "닉네임 형식(2~10자, 한글·영문·숫자)과 중복을 확인한다. 영문 대소문자는 구분하고, 내 현재 닉네임은 사용 가능. "
              + "중복·형식 오류도 에러가 아니라 200 + available: false와 사유(reason)로 돌려준다.\n\n"
              + "에러: COMMON_004(nickname 파라미터 없음)")
  @GetMapping("/nickname-availability")
  public ApiResponse<NicknameAvailabilityResponse> checkNicknameAvailability(
      @AuthenticationPrincipal Long userId,
      @Parameter(description = "확인할 닉네임 (URL 인코딩)") @RequestParam String nickname) {
    return ApiResponse.success(userProfileService.checkNicknameAvailability(userId, nickname));
  }

  @Operation(
      summary = "개인 프로필 저장",
      description =
          "닉네임·선호 장르·연주 포지션·사진을 저장한다. 온보딩 중(PENDING)이면 ACTIVE로 바꾼다. 마이페이지 수정에도 쓴다.\n\n"
              + "장르·포지션은 여러 개를 고른다. 장르 기타(ETC)·포지션 직접입력(CUSTOM)을 고르면 "
              + "genreCustomName·positionCustomName을 함께 보낸다.\n\n"
              + "에러: USER_001(닉네임 중복), USER_003(필수 약관 미동의), USER_005(사용자 없음), "
              + "COMMON_004(필드 누락·형식, 없는 코드, 직접 입력 값 누락·형식)")
  @PutMapping("/me/profile")
  public ApiResponse<ProfileUpdateResponse> updateProfile(
      @AuthenticationPrincipal Long userId, @RequestBody @Valid ProfileUpdateRequest request) {
    return ApiResponse.success(userProfileService.updateProfile(userId, request));
  }
}
