package com.ject.highfive.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;

import com.ject.highfive.global.exception.BusinessException;
import com.ject.highfive.user.dto.ProfileUpdateRequest;
import com.ject.highfive.user.entity.User;
import com.ject.highfive.user.exception.UserErrorCode;
import com.ject.highfive.user.repository.UserGenreRepository;
import com.ject.highfive.user.repository.UserPositionRepository;
import com.ject.highfive.user.repository.UserRepository;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class UserProfileServiceTest {

  private final UserRepository userRepository = mock(UserRepository.class);
  private final TermAgreementChecker termAgreementChecker = mock(TermAgreementChecker.class);
  private final UserProfileService service =
      new UserProfileService(
          userRepository,
          mock(UserGenreRepository.class),
          mock(UserPositionRepository.class),
          new NextStepResolver(termAgreementChecker, new ActiveBandChecker()),
          new ActiveBandChecker(),
          termAgreementChecker);

  // 따로 정하지 않으면 필수 약관에 모두 동의한 회원으로 본다
  @BeforeEach
  void agreedToRequiredTerms() {
    given(termAgreementChecker.hasAgreedRequiredTerms(any())).willReturn(true);
  }

  @Test
  @DisplayName("필수 약관에 동의하지 않은 회원은 프로필을 저장할 수 없다 (USER_003)")
  void requiredTermsNotAgreedIsUser003() {
    given(userRepository.findByIdForUpdate(1L)).willReturn(Optional.of(User.signUp()));
    given(termAgreementChecker.hasAgreedRequiredTerms(any())).willReturn(false);

    assertThatThrownBy(() -> service.updateProfile(1L, request()))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(UserErrorCode.REQUIRED_TERMS_NOT_AGREED);
  }

  private static DataIntegrityViolationException violation(String constraintName) {
    return new DataIntegrityViolationException(
        "could not execute statement",
        new ConstraintViolationException(
            "constraint violation", new SQLException("duplicate"), constraintName));
  }

  private static ProfileUpdateRequest request() {
    return new ProfileUpdateRequest("두현", List.of("ROCK"), List.of("VOCAL"), null, null, null);
  }

  @Test
  @DisplayName("저장 순간 닉네임 UNIQUE에 걸리면(다른 회원이 먼저 저장) USER_001")
  void nicknameConflictAtFlushIsUser001() {
    given(userRepository.findByIdForUpdate(1L)).willReturn(Optional.of(User.signUp()));
    willThrow(violation("uk_users_nickname")).given(userRepository).flush();

    assertThatThrownBy(() -> service.updateProfile(1L, request()))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(UserErrorCode.NICKNAME_DUPLICATED);
  }

  @Test
  @DisplayName("닉네임이 아닌 무결성 오류는 USER_001로 숨기지 않고 그대로 던진다(500)")
  void otherIntegrityErrorIsNotNicknameConflict() {
    given(userRepository.findByIdForUpdate(1L)).willReturn(Optional.of(User.signUp()));
    willThrow(violation("some_other_constraint")).given(userRepository).flush();

    assertThatThrownBy(() -> service.updateProfile(1L, request()))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  @DisplayName("닉네임 충돌 판단은 제약 이름의 대소문자·스키마 접두사와 상관없다 (H2: PUBLIC.UK_USERS_NICKNAME_...)")
  void nicknameConflictIgnoresCaseAndPrefix() {
    assertThat(UserProfileService.isNicknameConflict(violation("PUBLIC.UK_USERS_NICKNAME_INDEX_4")))
        .isTrue();
    assertThat(UserProfileService.isNicknameConflict(violation(null))).isFalse();
  }

  @Test
  @DisplayName("프로필 저장도 회원이 없으면 USER_005")
  void unknownUserIsNotFound() {
    given(userRepository.findByIdForUpdate(anyLong())).willReturn(Optional.empty());

    assertThatThrownBy(() -> service.updateProfile(99L, request()))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(UserErrorCode.USER_NOT_FOUND);
  }

  @Test
  @DisplayName("이미 다른 회원이 쓰는 닉네임이면 저장 전에 USER_001")
  void duplicateNicknameBeforeSave() {
    given(userRepository.findByIdForUpdate(1L)).willReturn(Optional.of(User.signUp()));
    given(userRepository.existsByNicknameAndIdNot(any(), any())).willReturn(true);

    assertThatThrownBy(() -> service.updateProfile(1L, request()))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(UserErrorCode.NICKNAME_DUPLICATED);
  }
}
