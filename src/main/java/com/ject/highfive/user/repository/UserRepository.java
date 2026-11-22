package com.ject.highfive.user.repository;

import com.ject.highfive.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

  // 닉네임 중복 확인. 대소문자는 컬럼 정렬 규칙(utf8mb4_bin)대로 구분한다
  boolean existsByNicknameAndIdNot(String nickname, Long id);
}
