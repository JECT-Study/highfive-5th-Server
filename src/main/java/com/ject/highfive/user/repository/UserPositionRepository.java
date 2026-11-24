package com.ject.highfive.user.repository;

import com.ject.highfive.user.entity.UserPosition;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserPositionRepository extends JpaRepository<UserPosition, Long> {

  List<UserPosition> findAllByUserIdOrderByIdAsc(Long userId);

  // 바로 DELETE 문을 실행한다. 파생 삭제(deleteBy...)는 INSERT보다 늦게 실행돼
  // 같은 포지션을 다시 저장할 때 UNIQUE(user_id, position_code)에 걸린다
  // clearAutomatically는 켜지 않는다: 영속성 컨텍스트를 비우면 같은 트랜잭션의 User가 준영속이 되어
  // 바로 뒤에 저장하는 행이 회원을 참조할 수 없다
  @Modifying(flushAutomatically = true)
  @Query("delete from UserPosition p where p.user.id = :userId")
  void deleteAllByUserId(@Param("userId") Long userId);
}
