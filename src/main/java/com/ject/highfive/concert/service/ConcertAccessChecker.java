package com.ject.highfive.concert.service;

import com.ject.highfive.concert.dummy.DummyBandStore;
import com.ject.highfive.concert.entity.ConcertTeamStatus;
import com.ject.highfive.concert.repository.ConcertTeamRepository;
import com.ject.highfive.global.exception.AuthErrorCode;
import com.ject.highfive.global.exception.BusinessException;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// 합동공연 페이지 API의 "참여 팀 멤버만" 검사. 공연이 있는지는 보지 않으니 호출자가 404를 먼저 낸다.
// 멤버십은 band_members가 없어 DummyBand에서 가져온다(JECT-253). JECT-72 공통 권한 로직이 나오면 교체
@Component
@RequiredArgsConstructor
public class ConcertAccessChecker {

  private final DummyBandStore dummyBandStore;
  private final ConcertTeamRepository concertTeamRepository;

  public void checkParticipant(Long concertId, Long userId) {
    Set<Long> bandIds = dummyBandStore.findBandIdsByUserId(userId);
    if (bandIds.isEmpty()
        || !concertTeamRepository.existsByConcertIdAndStatusAndBandIdIn(
            concertId, ConcertTeamStatus.JOINED, bandIds)) {
      throw new BusinessException(AuthErrorCode.FORBIDDEN);
    }
  }
}
