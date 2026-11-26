package com.ject.highfive.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.ject.highfive.global.config.ClockConfig;
import com.ject.highfive.user.entity.Term;
import com.ject.highfive.user.repository.TermRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class CurrentTermsTest {

  private static final LocalDateTime NOW = LocalDateTime.of(2026, 12, 1, 0, 0);

  private final TermRepository termRepository = mock(TermRepository.class);
  private final CurrentTerms currentTerms =
      new CurrentTerms(
          termRepository,
          Clock.fixed(NOW.atZone(ClockConfig.SERVER_ZONE).toInstant(), ClockConfig.SERVER_ZONE));

  private static Term term(long id, String code, boolean required, LocalDateTime effectiveAt) {
    Term term =
        Term.builder()
            .code(code)
            .version("v" + id)
            .title(code)
            .required(required)
            .effectiveAt(effectiveAt)
            .build();
    ReflectionTestUtils.setField(term, "id", id);
    return term;
  }

  @Test
  @DisplayName("code별로 시행일이 가장 늦은 버전을 고르고, 시행일이 같으면 나중에 추가한 행(ID 큰 것)")
  void picksLatestPerCodeWithIdTieBreak() {
    given(termRepository.findAllByEffectiveAtLessThanEqual(NOW))
        // 조회 순서와 상관없이 골라야 하므로 정답(2·4)을 일부러 앞에 둔다 ("마지막 원소" 같은 실수 방지)
        .willReturn(
            List.of(
                term(2, "SERVICE", true, NOW.minusDays(1)),
                term(1, "SERVICE", true, NOW.minusDays(30)),
                term(4, "PRIVACY", true, NOW.minusDays(1)),
                term(3, "PRIVACY", true, NOW.minusDays(1))));

    assertThat(currentTerms.find()).extracting(Term::getId).containsExactly(2L, 4L);
  }

  @Test
  @DisplayName("시행일이 지금과 같은 버전도 시행 중이고, 시각은 주입한 Clock 기준이다")
  void effectiveAtEqualToNowIsCurrent() {
    given(termRepository.findAllByEffectiveAtLessThanEqual(NOW))
        .willReturn(
            List.of(term(1, "SERVICE", true, NOW.minusDays(1)), term(2, "SERVICE", true, NOW)));

    assertThat(currentTerms.find()).extracting(Term::getId).containsExactly(2L);
  }

  @Test
  @DisplayName("필수 약관 먼저, 그다음 ID 순으로 정렬하고 requiredIds는 필수만 돌려준다")
  void sortsRequiredFirstThenById() {
    given(termRepository.findAllByEffectiveAtLessThanEqual(NOW))
        .willReturn(
            List.of(
                term(5, "MARKETING", false, NOW.minusDays(1)),
                term(7, "SERVICE", true, NOW.minusDays(1)),
                term(6, "PRIVACY", true, NOW.minusDays(1))));

    assertThat(currentTerms.find()).extracting(Term::getId).containsExactly(6L, 7L, 5L);
    assertThat(currentTerms.requiredIds()).containsExactly(6L, 7L);
  }
}
