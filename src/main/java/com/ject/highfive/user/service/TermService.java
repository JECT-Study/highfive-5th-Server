package com.ject.highfive.user.service;

import com.ject.highfive.global.exception.BusinessException;
import com.ject.highfive.global.exception.CommonErrorCode;
import com.ject.highfive.user.dto.TermAgreementRequest;
import com.ject.highfive.user.dto.TermAgreementRequest.Agreement;
import com.ject.highfive.user.dto.TermAgreementResponse;
import com.ject.highfive.user.dto.TermListResponse;
import com.ject.highfive.user.entity.Term;
import com.ject.highfive.user.entity.User;
import com.ject.highfive.user.entity.UserTermAgreement;
import com.ject.highfive.user.exception.UserErrorCode;
import com.ject.highfive.user.repository.UserRepository;
import com.ject.highfive.user.repository.UserTermAgreementRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 약관 목록 조회·약관 동의 저장 (온보딩 #2, 약관 개정 후 재동의)
@Service
@RequiredArgsConstructor
public class TermService {

  private final CurrentTerms currentTerms;
  private final UserRepository userRepository;
  private final UserTermAgreementRepository userTermAgreementRepository;
  private final NextStepResolver nextStepResolver;
  private final Clock clock;

  @Transactional(readOnly = true)
  public TermListResponse getTerms() {
    return TermListResponse.from(currentTerms.find());
  }

  // 요청이 지금 시행 중인 약관 목록과 맞는지 확인하고(COMMON_004), 필수 약관을 거부하지 않았는지
  // 확인한 뒤(USER_003) 동의·거부를 저장한다. 회원 상태는 바꾸지 않는다 (ACTIVE는 프로필 저장 때)
  @Transactional
  public TermAgreementResponse agree(Long userId, TermAgreementRequest request) {
    // 같은 회원이 동의를 두 번 눌러도 차례로 처리해 (user_id, term_id) UNIQUE에 걸리지 않게 한다
    User user =
        userRepository
            .findByIdForUpdate(userId)
            .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));
    // 시행 중인 약관 판단과 동의 시각을 같은 시각 기준으로 맞춘다
    LocalDateTime now = LocalDateTime.now(clock);
    Map<Long, Term> current =
        currentTerms.find(now).stream().collect(Collectors.toMap(Term::getId, Function.identity()));
    Map<Long, Boolean> answers = toAnswers(request.agreements(), current);
    if (declinedRequired(current, answers)) {
      throw new BusinessException(UserErrorCode.REQUIRED_TERMS_NOT_AGREED);
    }
    save(user, answers, current, now);
    return new TermAgreementResponse(nextStepResolver.resolve(user));
  }

  // termId → 동의 여부. 요청이 지금 약관 목록과 맞지 않으면(없는 약관·지난 버전·중복, 시행 중인 필수 약관 누락)
  // 앱이 오래된 목록을 들고 있는 것이므로 COMMON_004로 알려 목록을 다시 받게 한다
  private static Map<Long, Boolean> toAnswers(List<Agreement> agreements, Map<Long, Term> current) {
    boolean allCurrent = agreements.stream().allMatch(a -> current.containsKey(a.termId()));
    long distinct = agreements.stream().map(Agreement::termId).distinct().count();
    if (!allCurrent || distinct != agreements.size()) {
      throw new BusinessException(CommonErrorCode.INVALID_INPUT);
    }
    Map<Long, Boolean> answers =
        agreements.stream().collect(Collectors.toMap(Agreement::termId, Agreement::agreed));
    boolean requiredMissing =
        current.values().stream()
            .anyMatch(term -> term.isRequired() && !answers.containsKey(term.getId()));
    if (requiredMissing) {
      throw new BusinessException(CommonErrorCode.INVALID_INPUT);
    }
    return answers;
  }

  private static boolean declinedRequired(Map<Long, Term> current, Map<Long, Boolean> answers) {
    return current.values().stream()
        .anyMatch(term -> term.isRequired() && !answers.get(term.getId()));
  }

  private void save(
      User user, Map<Long, Boolean> answers, Map<Long, Term> current, LocalDateTime now) {
    Map<Long, UserTermAgreement> existing =
        userTermAgreementRepository
            .findAllByUserIdAndTermIdIn(user.getId(), answers.keySet())
            .stream()
            .collect(Collectors.toMap(a -> a.getTerm().getId(), Function.identity()));
    answers.forEach(
        (termId, agreed) -> {
          UserTermAgreement saved = existing.get(termId);
          if (saved != null) {
            saved.respond(agreed, now);
          } else {
            userTermAgreementRepository.save(
                new UserTermAgreement(user, current.get(termId), agreed, now));
          }
        });
  }
}
