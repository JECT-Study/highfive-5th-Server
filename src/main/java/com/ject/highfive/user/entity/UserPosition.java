package com.ject.highfive.user.entity;

import com.ject.highfive.code.entity.PositionCode;
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
import lombok.Getter;
import lombok.NoArgsConstructor;

// 개인 프로필의 연주 포지션 (여러 개). 수정할 때는 회원의 행을 모두 지우고 다시 저장한다.
// 포지션 직접입력(CUSTOM) 행에만 custom_name이 있다 (결정 #62)
@Getter
@Entity
@Table(
    name = "user_positions",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_user_positions_user_position",
            columnNames = {"user_id", "position_code"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserPosition {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Enumerated(EnumType.STRING)
  @Column(name = "position_code", nullable = false, length = 20)
  private PositionCode positionCode;

  @Column(name = "custom_name", length = 10)
  private String customName;

  public UserPosition(User user, PositionCode positionCode, String customName) {
    this.user = user;
    this.positionCode = positionCode;
    this.customName = positionCode.requiresCustomName() ? customName : null;
  }
}
