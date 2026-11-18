package com.ject.highfive.recruit.dto;

import com.ject.highfive.recruit.entity.RecruitPost;

public record RecruitPostCreateResponse(Long recruitPostId) {

  public static RecruitPostCreateResponse from(RecruitPost recruitPost) {
    return new RecruitPostCreateResponse(recruitPost.getId());
  }
}
