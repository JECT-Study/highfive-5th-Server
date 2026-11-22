package com.ject.highfive.user.service;

import com.ject.highfive.global.exception.BusinessException;
import com.ject.highfive.user.dto.MyInfoResponse;
import com.ject.highfive.user.dto.NicknameAvailabilityResponse;
import com.ject.highfive.user.dto.ProfileUpdateRequest;
import com.ject.highfive.user.dto.ProfileUpdateResponse;
import com.ject.highfive.user.entity.User;
import com.ject.highfive.user.entity.UserGenre;
import com.ject.highfive.user.entity.UserPosition;
import com.ject.highfive.user.exception.UserErrorCode;
import com.ject.highfive.user.repository.UserGenreRepository;
import com.ject.highfive.user.repository.UserPositionRepository;
import com.ject.highfive.user.repository.UserRepository;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 내 정보 조회·닉네임 사용 가능 여부·개인 프로필 저장 (온보딩 #3, 마이페이지 수정)
@Service
@RequiredArgsConstructor
public class UserProfileService {

  private static final Pattern NICKNAME_PATTERN = Pattern.compile(User.NICKNAME_REGEX);

  private final UserRepository userRepository;
  private final UserGenreRepository userGenreRepository;
  private final UserPositionRepository userPositionRepository;
  private final NextStepResolver nextStepResolver;
  private final ActiveBandChecker activeBandChecker;

  @Transactional(readOnly = true)
  public MyInfoResponse getMyInfo(Long userId) {
    User user = findUser(userId);
    return MyInfoResponse.of(
        user,
        userGenreRepository.findAllByUserIdOrderByIdAsc(userId),
        userPositionRepository.findAllByUserIdOrderByIdAsc(userId),
        activeBandChecker.hasActiveBand(user),
        nextStepResolver.resolve(user));
  }

  // 형식·중복 오류도 에러가 아니라 available: false + 사유로 돌려준다. 내 현재 닉네임은 사용 가능
  @Transactional(readOnly = true)
  public NicknameAvailabilityResponse checkNicknameAvailability(Long userId, String nickname) {
    if (nickname.length() < User.NICKNAME_MIN_LENGTH
        || nickname.length() > User.NICKNAME_MAX_LENGTH) {
      return NicknameAvailabilityResponse.unavailable(NicknameAvailabilityResponse.INVALID_LENGTH);
    }
    if (!NICKNAME_PATTERN.matcher(nickname).matches()) {
      return NicknameAvailabilityResponse.unavailable(
          NicknameAvailabilityResponse.INVALID_CHARACTER);
    }
    if (userRepository.existsByNicknameAndIdNot(nickname, userId)) {
      return NicknameAvailabilityResponse.unavailable(NicknameAvailabilityResponse.DUPLICATED);
    }
    return NicknameAvailabilityResponse.usable();
  }

  // 닉네임·사진을 저장하고 장르·포지션은 지우고 다시 저장한다. 온보딩 중(PENDING)이면 ACTIVE로 바꾼다
  @Transactional
  public ProfileUpdateResponse updateProfile(Long userId, ProfileUpdateRequest request) {
    User user = findUser(userId);
    // TODO: 최신 필수 약관에 모두 동의하지 않았으면 USER_003 (JECT-213, 약관 정책 PM 확정 후)
    // TODO: profileImageUrl은 이미지 저장소(CloudFront) 주소만 허용한다 (JECT-247 저장소 구성 후)
    if (userRepository.existsByNicknameAndIdNot(request.nickname(), userId)) {
      throw new BusinessException(UserErrorCode.NICKNAME_DUPLICATED);
    }
    user.updateProfile(request.nickname(), request.profileImageUrl());
    try {
      // 위 확인과 저장 사이에 다른 회원이 같은 닉네임을 저장하면 UNIQUE 위반으로 여기서 걸린다
      userRepository.flush();
    } catch (DataIntegrityViolationException e) {
      throw new BusinessException(UserErrorCode.NICKNAME_DUPLICATED);
    }
    replaceGenresAndPositions(user, request);
    return new ProfileUpdateResponse(user.getStatus(), nextStepResolver.resolve(user));
  }

  private void replaceGenresAndPositions(User user, ProfileUpdateRequest request) {
    userGenreRepository.deleteAllByUserId(user.getId());
    userPositionRepository.deleteAllByUserId(user.getId());
    String genreCustomName = request.trimmedGenreCustomName();
    String positionCustomName = request.trimmedPositionCustomName();
    userGenreRepository.saveAll(
        request.genres().stream()
            .map(genre -> new UserGenre(user, genre, genreCustomName))
            .toList());
    userPositionRepository.saveAll(
        request.positions().stream()
            .map(position -> new UserPosition(user, position, positionCustomName))
            .toList());
  }

  private User findUser(Long userId) {
    return userRepository
        .findById(userId)
        .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));
  }
}
