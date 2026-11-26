package com.ject.highfive.user.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ject.highfive.global.security.jwt.JwtTokenProvider;
import com.ject.highfive.user.entity.Term;
import com.ject.highfive.user.entity.User;
import com.ject.highfive.user.repository.TermRepository;
import com.ject.highfive.user.repository.UserRepository;
import com.ject.highfive.user.repository.UserTermAgreementRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

// 약관 목록·동의 저장과 nextStep(TERMS)을 실제 DB(H2)까지 거쳐 확인한다.
// 다른 테스트는 약관이 없는 DB를 쓰므로, 여기서 넣은 약관과 동의 기록은 테스트마다 지운다
@SpringBootTest
@AutoConfigureMockMvc
class TermApiTest {

  private static final LocalDateTime PAST = LocalDateTime.now().minusDays(1);

  @Autowired MockMvc mockMvc;
  @Autowired JwtTokenProvider jwtTokenProvider;
  @Autowired UserRepository userRepository;
  @Autowired TermRepository termRepository;
  @Autowired UserTermAgreementRepository userTermAgreementRepository;

  private Term oldService;
  private Term service;
  private Term privacy;
  private Term marketing;

  private static Term term(String code, String version, boolean required, LocalDateTime at) {
    return Term.builder()
        .code(code)
        .version(version)
        .title(code + " " + version)
        .required(required)
        .contentUrl(required ? "https://example.com/" + code : null)
        .effectiveAt(at)
        .build();
  }

  @BeforeEach
  void setUpTerms() {
    oldService = termRepository.save(term("SERVICE", "0.9", true, PAST.minusDays(30)));
    service = termRepository.save(term("SERVICE", "1.0", true, PAST));
    privacy = termRepository.save(term("PRIVACY", "1.0", true, PAST));
    marketing = termRepository.save(term("MARKETING", "1.0", false, PAST));
    // 아직 시행 전인 개정판은 목록에 나오지 않는다
    termRepository.save(term("PRIVACY", "2.0", true, LocalDateTime.now().plusDays(7)));
  }

  @AfterEach
  void cleanUp() {
    userTermAgreementRepository.deleteAll();
    termRepository.deleteAll();
  }

  private Long newUser() {
    return userRepository.save(User.signUp()).getId();
  }

  private String bearer(Long userId) {
    return "Bearer " + jwtTokenProvider.createAccessToken(userId);
  }

  private ResultActions agree(Long userId, String agreementsJson) throws Exception {
    return mockMvc.perform(
        post("/api/v1/users/me/term-agreements")
            .header(HttpHeaders.AUTHORIZATION, bearer(userId))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"agreements\":" + agreementsJson + "}"));
  }

  private static String item(Long termId, boolean agreed) {
    return "{\"termId\":%d,\"agreed\":%b}".formatted(termId, agreed);
  }

  private static String list(String... items) {
    return "[" + String.join(",", items) + "]";
  }

  private String answers(boolean service, boolean privacy, boolean marketing) {
    return list(
        item(this.service.getId(), service),
        item(this.privacy.getId(), privacy),
        item(this.marketing.getId(), marketing));
  }

  private ResultActions myInfo(Long userId) throws Exception {
    return mockMvc.perform(
        get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, bearer(userId)));
  }

  @Test
  @DisplayName("약관 목록은 종류별 시행 중인 최신 버전만, 필수 먼저 돌려준다")
  void listsCurrentTermsRequiredFirst() throws Exception {
    mockMvc
        .perform(get("/api/v1/terms").header(HttpHeaders.AUTHORIZATION, bearer(newUser())))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.data.terms[*].id")
                .value(
                    contains(
                        service.getId().intValue(),
                        privacy.getId().intValue(),
                        marketing.getId().intValue())))
        .andExpect(jsonPath("$.data.terms[0].code").value("SERVICE"))
        .andExpect(jsonPath("$.data.terms[0].version").value("1.0"))
        .andExpect(jsonPath("$.data.terms[0].required").value(true))
        .andExpect(jsonPath("$.data.terms[0].contentUrl").value("https://example.com/SERVICE"))
        .andExpect(jsonPath("$.data.terms[2].required").value(false))
        .andExpect(jsonPath("$.data.terms[2].contentUrl").value(nullValue()));
  }

  @Test
  @DisplayName("필수 약관에 동의하면 TERMS에서 PROFILE로 넘어가고, 회원 상태는 PENDING 그대로다")
  void agreeingMovesToProfile() throws Exception {
    Long userId = newUser();
    myInfo(userId).andExpect(jsonPath("$.data.nextStep").value("TERMS"));

    agree(userId, answers(true, true, false))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.nextStep").value("PROFILE"));

    myInfo(userId)
        .andExpect(jsonPath("$.data.status").value("PENDING"))
        .andExpect(jsonPath("$.data.nextStep").value("PROFILE"));
  }

  @Test
  @DisplayName("필수 약관을 거부하거나 빼면 USER_003이고 아무것도 저장하지 않는다")
  void missingOrDeclinedRequiredTermIsUser003() throws Exception {
    Long userId = newUser();

    agree(userId, answers(true, false, true))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("USER_003"));
    agree(userId, "[{\"termId\":%d,\"agreed\":true}]".formatted(service.getId()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("USER_003"));

    assertThat(userTermAgreementRepository.count()).isZero();
  }

  @Test
  @DisplayName("없는 약관, 지난 버전, 같은 약관 중복은 COMMON_004")
  void unknownOldOrDuplicateTermIsInvalidInput() throws Exception {
    Long userId = newUser();
    String withOld =
        list(
            item(oldService.getId(), true),
            item(service.getId(), true),
            item(privacy.getId(), true));
    String withUnknown =
        list(item(service.getId(), true), item(privacy.getId(), true), item(999_999L, true));
    String duplicated =
        list(
            item(service.getId(), true),
            item(privacy.getId(), true),
            item(marketing.getId(), true),
            item(marketing.getId(), false));

    for (String body : new String[] {withOld, withUnknown, duplicated}) {
      agree(userId, body)
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("COMMON_004"));
    }
  }

  @Test
  @DisplayName("다시 동의하면 행을 새로 만들지 않고 동의 여부만 바꾼다 (선택 약관 철회 포함)")
  void respondingAgainUpdatesSameRows() throws Exception {
    Long userId = newUser();

    agree(userId, answers(true, true, true)).andExpect(status().isOk());
    agree(userId, answers(true, true, false)).andExpect(status().isOk());

    assertThat(userTermAgreementRepository.count()).isEqualTo(3);
    assertThat(
            userTermAgreementRepository
                .findAllByUserIdAndTermIdIn(userId, List.of(marketing.getId()))
                .get(0)
                .isAgreed())
        .isFalse();
  }

  @Test
  @DisplayName("필수 약관에 동의하지 않으면 프로필을 저장할 수 없다 (USER_003)")
  void profileRequiresTerms() throws Exception {
    Long userId = newUser();

    mockMvc
        .perform(
            put("/api/v1/users/me/profile")
                .header(HttpHeaders.AUTHORIZATION, bearer(userId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"nickname\":\"약관전\","
                        + "\"genreCodes\":[\"ROCK\"],\"positionCodes\":[\"VOCAL\"]}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("USER_003"));
  }

  @Test
  @DisplayName("필수 약관이 개정돼 시행되면 동의했던 회원도 다시 TERMS (결정 #37)")
  void revisedRequiredTermNeedsReconsent() throws Exception {
    Long userId = newUser();
    agree(userId, answers(true, true, false)).andExpect(status().isOk());
    myInfo(userId).andExpect(jsonPath("$.data.nextStep").value("PROFILE"));

    Term revised =
        termRepository.save(term("SERVICE", "1.1", true, LocalDateTime.now().minusSeconds(1)));

    myInfo(userId).andExpect(jsonPath("$.data.nextStep").value("TERMS"));
    agree(
            userId,
            "[{\"termId\":%d,\"agreed\":true},{\"termId\":%d,\"agreed\":true}]"
                .formatted(revised.getId(), privacy.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.nextStep").value("PROFILE"));
  }
}
