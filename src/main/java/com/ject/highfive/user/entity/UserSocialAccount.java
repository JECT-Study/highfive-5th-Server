package com.ject.highfive.user.entity;

import com.ject.highfive.global.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

// 로그인 수단. 탈퇴하면 이 행을 지워 같은 소셜 계정으로 다시 로그인하면 새 계정이 된다 (11/13 회의 B6)
@Getter
@Entity
@Table(
    name = "user_social_accounts",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_user_social_accounts_provider_user",
            columnNames = {"provider", "provider_user_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserSocialAccount extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10)
  private SocialProvider provider;

  // 소셜 ID 토큰의 sub
  @Column(name = "provider_user_id", nullable = false)
  private String providerUserId;

  // 애플은 이메일을 숨길 수 있고, 카카오는 동의하지 않으면 주지 않는다
  private String email;

  @Builder
  private UserSocialAccount(
      User user, SocialProvider provider, String providerUserId, String email) {
    this.user = user;
    this.provider = provider;
    this.providerUserId = providerUserId;
    this.email = email;
  }
}
