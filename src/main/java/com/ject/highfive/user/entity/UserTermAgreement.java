package com.ject.highfive.user.entity;

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
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 회원이 어느 약관 버전에 언제 동의(또는 거부)했는지. 선택 약관은 거부(false)도 기록한다
@Getter
@Entity
@Table(
    name = "user_term_agreements",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_user_term_agreements_user_term",
            columnNames = {"user_id", "term_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserTermAgreement {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "term_id", nullable = false)
  private Term term;

  @Column(nullable = false)
  private boolean agreed;

  // 동의·거부를 마지막으로 저장한 시각
  @Column(name = "agreed_at", nullable = false)
  private LocalDateTime agreedAt;

  public UserTermAgreement(User user, Term term, boolean agreed, LocalDateTime agreedAt) {
    this.user = user;
    this.term = term;
    this.agreed = agreed;
    this.agreedAt = agreedAt;
  }

  // 같은 버전에 다시 응답하면 행을 새로 만들지 않고 값만 바꾼다
  public void respond(boolean agreed, LocalDateTime respondedAt) {
    this.agreed = agreed;
    this.agreedAt = respondedAt;
  }
}
