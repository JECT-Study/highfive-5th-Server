package com.ject.highfive.user.repository;

import com.ject.highfive.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {}
