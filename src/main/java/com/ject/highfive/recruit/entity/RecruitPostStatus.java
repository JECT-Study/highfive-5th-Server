package com.ject.highfive.recruit.entity;

// 직접 마감했을 때만 CLOSED. 기한 마감은 저장하지 않고 isClosed로 계산한다 (의사결정 D10)
public enum RecruitPostStatus {
  RECRUITING,
  CLOSED
}
