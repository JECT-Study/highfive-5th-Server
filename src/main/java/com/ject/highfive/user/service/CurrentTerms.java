package com.ject.highfive.user.service;

import com.ject.highfive.user.entity.Term;
import com.ject.highfive.user.repository.TermRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// 지금 시행 중인 약관: code별로 시행일이 지난 버전 중 가장 최근 것 하나 (필수 먼저, 그다음 ID 순).
// 약관 목록 조회·동의 저장·필수 약관 동의 여부 확인이 모두 같은 기준을 쓴다
@Component
@RequiredArgsConstructor
public class CurrentTerms {

  private static final Comparator<Term> NEWER =
      Comparator.comparing(Term::getEffectiveAt).thenComparing(Term::getId);
  private static final Comparator<Term> DISPLAY_ORDER =
      Comparator.comparing((Term term) -> !term.isRequired()).thenComparing(Term::getId);

  private final TermRepository termRepository;
  private final Clock clock;

  public List<Term> find() {
    return find(LocalDateTime.now(clock));
  }

  // 시행일이 now와 같은 버전도 시행 중으로 본다. 같은 code에서 시행일까지 같으면 나중에 추가한 행(ID 큰 것)
  public List<Term> find(LocalDateTime now) {
    Map<String, Term> latestByCode =
        termRepository.findAllByEffectiveAtLessThanEqual(now).stream()
            .collect(Collectors.toMap(Term::getCode, term -> term, BinaryOperator.maxBy(NEWER)));
    return latestByCode.values().stream().sorted(DISPLAY_ORDER).toList();
  }

  public List<Long> requiredIds() {
    return find().stream().filter(Term::isRequired).map(Term::getId).toList();
  }
}
