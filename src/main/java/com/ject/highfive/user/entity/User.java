package com.ject.highfive.user.entity;

import com.ject.highfive.global.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 서비스 안의 사람 1명 + 개인 프로필. 로그인 수단은 UserSocialAccount로 분리한다
@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  // 프로필을 만들기 전과 탈퇴 후에는 null
  // TODO: 닉네임 저장 시 대소문자를 구분하는 정렬 규칙(utf8mb4_bin)과 UNIQUE를 맞춘다 (JECT-214)
  @Column(length = 10, unique = true)
  private String nickname;

  @Column(name = "profile_image_url", length = 500)
  private String profileImageUrl;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private UserStatus status;

  @Column(name = "push_enabled", nullable = false)
  private boolean pushEnabled;

  @Column(name = "push_chat_enabled", nullable = false)
  private boolean pushChatEnabled;

  @Column(name = "push_invite_enabled", nullable = false)
  private boolean pushInviteEnabled;

  @Column(name = "push_concert_enabled", nullable = false)
  private boolean pushConcertEnabled;

  @Column(name = "push_ticket_enabled", nullable = false)
  private boolean pushTicketEnabled;

  @Column(name = "withdrawn_at")
  private LocalDateTime withdrawnAt;

  private User(UserStatus status) {
    this.status = status;
    this.pushEnabled = true;
    this.pushChatEnabled = true;
    this.pushInviteEnabled = true;
    this.pushConcertEnabled = true;
    this.pushTicketEnabled = true;
  }

  // 소셜 로그인으로 처음 가입한 회원. 약관·프로필을 끝내면 ACTIVE가 된다
  public static User signUp() {
    return new User(UserStatus.PENDING);
  }
}
