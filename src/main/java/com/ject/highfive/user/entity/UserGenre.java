package com.ject.highfive.user.entity;

import com.ject.highfive.code.entity.GenreCode;
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

// 개인 프로필의 선호 장르 (여러 개). 수정할 때는 회원의 행을 모두 지우고 다시 저장한다.
// 장르 기타(ETC) 행에만 custom_name이 있다 (결정 #62)
@Getter
@Entity
@Table(
    name = "user_genres",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_user_genres_user_genre",
            columnNames = {"user_id", "genre_code"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserGenre {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Enumerated(EnumType.STRING)
  @Column(name = "genre_code", nullable = false, length = 20)
  private GenreCode genreCode;

  @Column(name = "custom_name", length = 10)
  private String customName;

  public UserGenre(User user, GenreCode genreCode, String customName) {
    this.user = user;
    this.genreCode = genreCode;
    this.customName = genreCode.requiresCustomName() ? customName : null;
  }
}
