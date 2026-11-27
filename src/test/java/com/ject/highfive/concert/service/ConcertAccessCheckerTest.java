package com.ject.highfive.concert.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

import com.ject.highfive.concert.dummy.DummyBandStore;
import com.ject.highfive.concert.entity.ConcertTeamStatus;
import com.ject.highfive.concert.repository.ConcertTeamRepository;
import com.ject.highfive.global.exception.AuthErrorCode;
import com.ject.highfive.global.exception.BusinessException;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConcertAccessCheckerTest {

  private static final Long CONCERT_ID = 12L;

  @Mock ConcertTeamRepository concertTeamRepository;

  // 임시 멤버십은 실제 DummyBandStore를 쓴다: 밴드 i의 멤버가 사용자 i
  private ConcertAccessChecker checker() {
    return new ConcertAccessChecker(new DummyBandStore(), concertTeamRepository);
  }

  private void givenParticipating(Set<Long> bandIds, boolean joined) {
    given(
            concertTeamRepository.existsByConcertIdAndStatusAndBandIdIn(
                eq(CONCERT_ID), eq(ConcertTeamStatus.JOINED), eq(bandIds)))
        .willReturn(joined);
  }

  @Test
  @DisplayName("속한 밴드가 이 공연의 참여 팀이면 통과한다")
  void participantPasses() {
    givenParticipating(Set.of(1L), true);

    assertThatCode(() -> checker().checkParticipant(CONCERT_ID, 1L)).doesNotThrowAnyException();
  }

  @Test
  @DisplayName("속한 밴드가 참여 팀이 아니면 AUTH_005")
  void nonParticipantRejected() {
    givenParticipating(Set.of(2L), false);

    assertThatThrownBy(() -> checker().checkParticipant(CONCERT_ID, 2L))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(AuthErrorCode.FORBIDDEN);
  }

  @Test
  @DisplayName("어느 밴드에도 속하지 않으면 공연을 조회하지 않고 AUTH_005")
  void userWithoutBandRejected() {
    assertThatThrownBy(() -> checker().checkParticipant(CONCERT_ID, 99L))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(AuthErrorCode.FORBIDDEN);
    verifyNoInteractions(concertTeamRepository);
  }
}
