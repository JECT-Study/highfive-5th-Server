package com.ject.highfive.user.service;

import com.ject.highfive.user.entity.User;
import org.springframework.stereotype.Component;

// ACTIVE 상태로 속한 밴드가 있는지. 내 정보 조회의 hasBand와 nextStep(BAND_START/HOME)이 쓴다
@Component
public class ActiveBandChecker {

  // TODO: band_members에서 status = ACTIVE인 행이 있는지로 판단한다 (JECT-71 밴드 생성 후)
  public boolean hasActiveBand(User user) {
    return false;
  }
}
