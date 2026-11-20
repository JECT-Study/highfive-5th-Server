package com.ject.highfive.recruit.repository;

import com.ject.highfive.recruit.entity.RecruitPostFavorite;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RecruitPostFavoriteRepository extends JpaRepository<RecruitPostFavorite, Long> {

  // 이미 찜했으면 UNIQUE(user_id, recruitment_post_id)에 걸려 아무것도 바꾸지 않고 끝난다(멱등, 스펙 4.4).
  // IGNORE는 FK 위반 같은 다른 오류까지 삼켜 쓰지 않는다. 네이티브라 Auditing을 거치지 않아 시각을 넘겨받는다
  @Modifying
  @Query(
      nativeQuery = true,
      value =
          "insert into recruitment_favorites"
              + " (user_id, recruitment_post_id, created_at, updated_at)"
              + " values (:userId, :recruitPostId, :now, :now)"
              + " on duplicate key update id = id")
  void insertIfAbsent(
      @Param("userId") Long userId,
      @Param("recruitPostId") Long recruitPostId,
      @Param("now") LocalDateTime now);

  // 조회 후 삭제(파생 삭제)는 같은 해제가 겹치면 0행 삭제로 예외가 나서, 한 문장으로 지운다 (스펙 4.4)
  @Modifying
  @Query(
      "delete from RecruitPostFavorite f"
          + " where f.userId = :userId and f.recruitPost.id = :recruitPostId")
  void deleteByUserIdAndRecruitPostId(
      @Param("userId") Long userId, @Param("recruitPostId") Long recruitPostId);

  // 공고 목록(2.5)의 찜 여부. 넘긴 공고 중 사용자가 찜한 것만 돌려준다
  @Query(
      "select f.recruitPost.id from RecruitPostFavorite f"
          + " where f.userId = :userId and f.recruitPost.id in :recruitPostIds")
  Set<Long> findRecruitPostIdsByUserId(
      @Param("userId") Long userId, @Param("recruitPostIds") Collection<Long> recruitPostIds);
}
