package com.ject.highfive.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 회원 가입 약관의 버전별 원본. 개정하면 같은 code로 새 버전 행을 추가하고 이전 행은 지우지 않는다.
// 약관 목록은 데이터라 code는 enum이 아닌 문자열 (SERVICE / PRIVACY / AGE_OVER_14 …, 결정 #34)
@Getter
@Entity
@Table(
    name = "terms",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_terms_code_version",
            columnNames = {"code", "version"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Term {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 30)
  private String code;

  @Column(nullable = false, length = 20)
  private String version;

  @Column(nullable = false, length = 100)
  private String title;

  @Column(name = "is_required", nullable = false)
  private boolean required;

  // 약관 전문(공개 웹페이지) 주소. 공개 페이지(Notion)를 만들기 전까지는 null.
  // 개정해도 같은 페이지 내용을 고쳐 URL은 유지하고 버전 행만 추가한다 (결정 #35)
  @Column(name = "content_url", length = 500)
  private String contentUrl;

  // 시행일. 이 시각이 지난 버전 중 가장 최근 것이 그 약관의 현재 버전이다
  @Column(name = "effective_at", nullable = false)
  private LocalDateTime effectiveAt;

  @Builder
  private Term(
      String code,
      String version,
      String title,
      boolean required,
      String contentUrl,
      LocalDateTime effectiveAt) {
    this.code = code;
    this.version = version;
    this.title = title;
    this.required = required;
    this.contentUrl = contentUrl;
    this.effectiveAt = effectiveAt;
  }
}
