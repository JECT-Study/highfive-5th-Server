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

  // 닉네임 규칙 (결정 #31): 2~10자, 한글(완성형)·영문·숫자만. 요청 검증과 닉네임 확인 API가 함께 쓴다
  public static final int NICKNAME_MIN_LENGTH = 2;
  public static final int NICKNAME_MAX_LENGTH = 10;
  public static final String NICKNAME_REGEX = "^[가-힣a-zA-Z0-9]+$";

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  // 프로필을 만들기 전과 탈퇴 후에는 null. 영문 대소문자를 구분하므로(Rock ≠ rock, 결정 #32)
  // DB 기본 정렬 규칙(utf8mb4_unicode_ci) 대신 utf8mb4_bin으로 저장·비교한다 (UNIQUE도 같은 기준)
  @Column(unique = true, columnDefinition = "varchar(10) collate utf8mb4_bin")
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

  // 개인 프로필 저장. 온보딩 중(PENDING)이면 이때 ACTIVE가 된다 (마이페이지 수정에도 쓴다)
  public void updateProfile(String nickname, String profileImageUrl) {
    this.nickname = nickname;
    this.profileImageUrl = profileImageUrl;
    if (status == UserStatus.PENDING) {
      status = UserStatus.ACTIVE;
    }
  }
}
