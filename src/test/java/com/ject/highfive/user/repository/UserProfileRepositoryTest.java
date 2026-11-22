package com.ject.highfive.user.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ject.highfive.code.entity.GenreCode;
import com.ject.highfive.support.MySqlRepositoryTest;
import com.ject.highfive.user.entity.User;
import com.ject.highfive.user.entity.UserGenre;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

// MySQL 기본 정렬 규칙은 대소문자를 구분하지 않으므로, 실제 MySQL에서 utf8mb4_bin이 적용되는지 확인한다
@MySqlRepositoryTest
class UserProfileRepositoryTest {

  @Autowired UserRepository userRepository;
  @Autowired UserGenreRepository userGenreRepository;
  @Autowired EntityManager entityManager;

  private User userWithNickname(String nickname) {
    User user = User.signUp();
    user.updateProfile(nickname, null);
    return userRepository.saveAndFlush(user);
  }

  @Test
  @DisplayName("닉네임은 대소문자를 구분한다: Rock과 rock은 둘 다 저장되고 중복 확인도 따로 한다")
  void nicknameIsCaseSensitive() {
    final User upper = userWithNickname("Rock");
    userWithNickname("rock");

    assertThat(userRepository.existsByNicknameAndIdNot("Rock", -1L)).isTrue();
    assertThat(userRepository.existsByNicknameAndIdNot("ROCK", -1L)).isFalse();
    assertThat(userRepository.existsByNicknameAndIdNot("Rock", upper.getId())).isFalse();
  }

  @Test
  @DisplayName("완전히 같은 닉네임은 UNIQUE로 막힌다")
  void sameNicknameIsUnique() {
    userWithNickname("두현");

    assertThatThrownBy(() -> userWithNickname("두현"))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  @DisplayName("장르를 지우고 같은 코드로 다시 저장해도 UNIQUE에 걸리지 않는다")
  void deleteThenReinsertSameGenre() {
    User user = userWithNickname("재저장");
    userGenreRepository.saveAllAndFlush(List.of(new UserGenre(user, GenreCode.ROCK, null)));

    userGenreRepository.deleteAllByUserId(user.getId());
    userGenreRepository.saveAllAndFlush(
        List.of(
            new UserGenre(user, GenreCode.ROCK, null), new UserGenre(user, GenreCode.ETC, "시티팝")));
    entityManager.clear();

    assertThat(userGenreRepository.findAllByUserIdOrderByIdAsc(user.getId()))
        .extracting(UserGenre::getGenreCode, UserGenre::getCustomName)
        .containsExactly(
            org.assertj.core.groups.Tuple.tuple(GenreCode.ROCK, null),
            org.assertj.core.groups.Tuple.tuple(GenreCode.ETC, "시티팝"));
  }
}
