package com.ject.highfive.auth.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.ject.highfive.support.RedisContainerConfig;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.redis.test.autoconfigure.DataRedisTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;

@DataRedisTest
@Import({RedisContainerConfig.class, RefreshTokenRepository.class})
class RefreshTokenRepositoryTest {

  private static final Duration TTL = Duration.ofDays(14);
  private static final String KEY = "refresh:7";

  @Autowired RefreshTokenRepository refreshTokenRepository;
  @Autowired StringRedisTemplate redisTemplate;

  @BeforeEach
  void clear() {
    redisTemplate.delete(KEY);
  }

  @Test
  @DisplayName("refresh:{userId}에 만료 시간과 함께 저장하고, 다시 저장하면 덮어쓴다")
  void saveOverwritesWithTtl() {
    refreshTokenRepository.save(7L, "first", TTL);
    refreshTokenRepository.save(7L, "second", TTL);

    assertThat(redisTemplate.opsForValue().get(KEY))
        .isEqualTo(RefreshTokenRepository.hash("second"));
    assertThat(redisTemplate.getExpire(KEY)).isPositive().isLessThanOrEqualTo(TTL.toSeconds());
  }

  @Test
  @DisplayName("저장된 값과 같은 토큰이면 새 토큰으로 바꾸고 만료 시간을 다시 잡는다")
  void rotateReplacesMatchingToken() {
    refreshTokenRepository.save(7L, "old", Duration.ofMinutes(1));

    boolean rotated = refreshTokenRepository.rotate(7L, "old", "new", TTL);

    assertThat(rotated).isTrue();
    assertThat(redisTemplate.opsForValue().get(KEY)).isEqualTo(RefreshTokenRepository.hash("new"));
    assertThat(redisTemplate.getExpire(KEY)).isGreaterThan(Duration.ofMinutes(1).toSeconds());
  }

  @Test
  @DisplayName("저장된 값과 다르거나 없으면 바꾸지 않는다")
  void rotateRejectsMismatchedToken() {
    assertThat(refreshTokenRepository.rotate(7L, "old", "new", TTL)).isFalse();
    assertThat(redisTemplate.hasKey(KEY)).isFalse();

    refreshTokenRepository.save(7L, "current", TTL);
    assertThat(refreshTokenRepository.rotate(7L, "old", "new", TTL)).isFalse();
    assertThat(redisTemplate.opsForValue().get(KEY))
        .isEqualTo(RefreshTokenRepository.hash("current"));
  }

  @Test
  @DisplayName("같은 토큰으로 두 번 바꾸면 두 번째는 실패한다")
  void rotateSucceedsOnlyOnce() {
    refreshTokenRepository.save(7L, "old", TTL);

    assertThat(refreshTokenRepository.rotate(7L, "old", "new1", TTL)).isTrue();
    assertThat(refreshTokenRepository.rotate(7L, "old", "new2", TTL)).isFalse();
    assertThat(redisTemplate.opsForValue().get(KEY)).isEqualTo(RefreshTokenRepository.hash("new1"));
  }

  @Test
  @DisplayName("로그아웃하면 지운다")
  void deleteRemovesToken() {
    refreshTokenRepository.save(7L, "token", TTL);

    refreshTokenRepository.deleteByUserId(7L);

    assertThat(redisTemplate.hasKey(KEY)).isFalse();
  }

  @Test
  @DisplayName("원문이 아니라 SHA-256 해시를 저장한다")
  void storesHashNotRawToken() {
    refreshTokenRepository.save(7L, "raw-refresh-token", TTL);

    String stored = redisTemplate.opsForValue().get(KEY);
    assertThat(stored).isNotEqualTo("raw-refresh-token").hasSize(64).matches("[0-9a-f]+");
  }
}
