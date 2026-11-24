package com.ject.highfive.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import com.ject.highfive.global.config.JpaAuditingConfig;
import com.ject.highfive.user.entity.User;
import com.ject.highfive.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

// 실제 DB(H2)가 내는 닉네임 UNIQUE 위반을 서비스가 닉네임 충돌로 알아보는지 확인한다 (MySQL은 UserProfileRepositoryTest)
// 기본 내장 H2는 MySQL 호환 모드가 아니라 collate 구문을 못 읽으므로, 테스트 설정(application.yml)의 H2를 그대로 쓴다
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaAuditingConfig.class)
class NicknameUniqueConstraintH2Test {

  @Autowired UserRepository userRepository;

  private User userWithNickname(String nickname) {
    User user = User.signUp();
    user.updateProfile(nickname, null);
    return user;
  }

  @Test
  @DisplayName("같은 닉네임을 저장하면 나는 오류를 닉네임 충돌로 판단한다")
  void duplicateNicknameIsRecognized() {
    userRepository.saveAndFlush(userWithNickname("두현"));

    DataIntegrityViolationException e =
        catchThrowableOfType(
            DataIntegrityViolationException.class,
            () -> userRepository.saveAndFlush(userWithNickname("두현")));

    assertThat(UserProfileService.isNicknameConflict(e)).isTrue();
  }
}
