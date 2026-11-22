package com.ject.highfive.user.repository;

import com.ject.highfive.user.entity.User;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

  // 닉네임 중복 확인. 대소문자는 컬럼 정렬 규칙(utf8mb4_bin)대로 구분한다
  boolean existsByNicknameAndIdNot(String nickname, Long id);

  // 프로필 저장용. 회원 행에 쓰기 락을 걸어 같은 회원의 동시 저장(장르·포지션 삭제 후 다시 저장)을 차례로 처리한다.
  // 다른 회원과는 경합하지 않는다
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select u from User u where u.id = :id")
  Optional<User> findByIdForUpdate(@Param("id") Long id);
}
