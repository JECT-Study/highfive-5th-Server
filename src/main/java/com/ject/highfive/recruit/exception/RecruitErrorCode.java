package com.ject.highfive.recruit.exception;

import com.ject.highfive.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

// 번호는 API 명세 부록 B 그대로
@Getter
@RequiredArgsConstructor
public enum RecruitErrorCode implements ErrorCode {
  RECRUIT_POST_NOT_FOUND(HttpStatus.NOT_FOUND, "RECRUIT_001", "모집 공고를 찾을 수 없습니다."),
  RECRUIT_POST_ALREADY_EXISTS(HttpStatus.CONFLICT, "RECRUIT_002", "이미 모집 공고가 있는 공연입니다."),
  DEADLINE_BEFORE_TODAY(HttpStatus.BAD_REQUEST, "RECRUIT_003", "공고 마감일은 오늘 이후여야 합니다."),
  RECRUIT_POST_ALREADY_CLOSED(HttpStatus.CONFLICT, "RECRUIT_004", "이미 마감된 공고입니다."),
  DEADLINE_AFTER_PERFORM_DATE(HttpStatus.BAD_REQUEST, "RECRUIT_005", "공고 마감일은 공연 날짜보다 늦을 수 없습니다."),
  CONCERT_ALREADY_PERFORMED(HttpStatus.BAD_REQUEST, "RECRUIT_006", "이미 지난 공연에는 공고를 작성할 수 없습니다.");

  private final HttpStatus status;
  private final String code;
  private final String message;
}
