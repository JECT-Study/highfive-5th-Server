package com.ject.highfive.auth.repository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Repository;

// 리프레시 토큰은 Redis refresh:{userId}에 사용자당 1개만 둔다 (동시 로그인 불가).
// Redis·AOF 파일이 유출돼도 토큰을 그대로 쓸 수 없도록 원문 대신 SHA-256 해시를 저장·비교한다
@Repository
@RequiredArgsConstructor
public class RefreshTokenRepository {

  private static final String KEY_PREFIX = "refresh:";

  // 저장된 값이 기존 토큰과 같을 때만 새 토큰으로 바꾼다. 비교와 교체를 한 번에 해서
  // 같은 리프레시 토큰으로 동시에 재발급해도 한 요청만 성공한다
  private static final RedisScript<Long> ROTATE_SCRIPT =
      RedisScript.of(
          """
          if redis.call('GET', KEYS[1]) == ARGV[1] then
            redis.call('SET', KEYS[1], ARGV[2], 'PX', ARGV[3])
            return 1
          end
          return 0
          """,
          Long.class);

  private final StringRedisTemplate redisTemplate;

  // 로그인할 때마다 덮어쓰므로 다른 기기의 리프레시 토큰은 다음 재발급 때 거부된다
  public void save(Long userId, String refreshToken, Duration ttl) {
    redisTemplate.opsForValue().set(key(userId), hash(refreshToken), ttl);
  }

  public boolean rotate(Long userId, String oldToken, String newToken, Duration ttl) {
    Long result =
        redisTemplate.execute(
            ROTATE_SCRIPT,
            List.of(key(userId)),
            hash(oldToken),
            hash(newToken),
            String.valueOf(ttl.toMillis()));
    return Long.valueOf(1L).equals(result);
  }

  public void deleteByUserId(Long userId) {
    redisTemplate.delete(key(userId));
  }

  private String key(Long userId) {
    return KEY_PREFIX + userId;
  }

  static String hash(String token) {
    try {
      byte[] digest =
          MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException e) {
      // SHA-256은 모든 JVM이 반드시 제공한다
      throw new IllegalStateException(e);
    }
  }
}
