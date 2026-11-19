package com.ject.highfive.recruit.entity;

import com.ject.highfive.global.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 해제하면 행을 지운다(의사결정 D14). created_at이 찜 목록(#18)의 "최신 찜 순" 기준
@Getter
@Entity
@Table(
    name = "recruitment_favorites",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_recruitment_favorites_user_post",
            columnNames = {"user_id", "recruitment_post_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecruitPostFavorite extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  // 컬럼 이름은 ERD 그대로(recruitment_post_id)
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "recruitment_post_id", nullable = false)
  private RecruitPost recruitPost;

  @Builder
  private RecruitPostFavorite(Long userId, RecruitPost recruitPost) {
    this.userId = userId;
    this.recruitPost = recruitPost;
  }
}
