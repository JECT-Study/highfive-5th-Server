package com.ject.highfive.user.controller;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ject.highfive.global.security.jwt.JwtTokenProvider;
import com.ject.highfive.user.entity.User;
import com.ject.highfive.user.repository.UserRepository;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

// 로그인한 회원으로 내 정보 조회·닉네임 확인·프로필 저장을 실제 DB(H2)까지 거쳐 확인한다.
// 테스트끼리 DB를 공유하므로 닉네임은 테스트마다 겹치지 않게 만든다
@SpringBootTest
@AutoConfigureMockMvc
class UserProfileApiTest {

  private static final AtomicInteger SEQUENCE = new AtomicInteger();

  @Autowired MockMvc mockMvc;
  @Autowired JwtTokenProvider jwtTokenProvider;
  @Autowired UserRepository userRepository;

  private static String uniqueNickname(String prefix) {
    return prefix + SEQUENCE.incrementAndGet();
  }

  private static String profileBody(String nickname) {
    return "{\"nickname\":\""
        + nickname
        + "\",\"genreCodes\":[\"ROCK\",\"ETC\",\"ROCK\"],"
        + "\"genreCustomName\":\" 시티팝 \",\"positionCodes\":[\"VOCAL\"],"
        + "\"positionCustomName\":null,\"profileImageUrl\":null}";
  }

  private Long newUser() {
    return userRepository.save(User.signUp()).getId();
  }

  private String bearer(Long userId) {
    return "Bearer " + jwtTokenProvider.createAccessToken(userId);
  }

  private ResultActions saveProfile(Long userId, String body) throws Exception {
    return mockMvc.perform(
        put("/api/v1/users/me/profile")
            .header(HttpHeaders.AUTHORIZATION, bearer(userId))
            .contentType(MediaType.APPLICATION_JSON)
            .content(body));
  }

  private ResultActions checkNickname(Long userId, String nickname) throws Exception {
    return mockMvc.perform(
        get("/api/v1/users/nickname-availability")
            .header(HttpHeaders.AUTHORIZATION, bearer(userId))
            .param("nickname", nickname));
  }

  @Test
  // 테스트 DB(H2)에는 약관이 없어 약관 단계를 건너뛰고 PROFILE이 된다 (약관이 있을 때는 TermApiTest)
  @DisplayName("가입 직후 내 정보는 PENDING, 프로필 없음, 프로필 화면으로")
  void newUserInfo() throws Exception {
    Long userId = newUser();

    mockMvc
        .perform(get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, bearer(userId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.id").value(userId))
        .andExpect(jsonPath("$.data.status").value("PENDING"))
        .andExpect(jsonPath("$.data.nickname").value(nullValue()))
        .andExpect(jsonPath("$.data.genreCodes").value(empty()))
        .andExpect(jsonPath("$.data.positionCodes").value(empty()))
        .andExpect(jsonPath("$.data.genreCustomName").value(nullValue()))
        .andExpect(jsonPath("$.data.hasBand").value(false))
        .andExpect(jsonPath("$.data.nextStep").value("PROFILE"));
  }

  @Test
  @DisplayName("프로필을 저장하면 ACTIVE가 되고, 내 정보에 장르·포지션·직접 입력 값이 나온다")
  void saveProfileActivatesUser() throws Exception {
    Long userId = newUser();
    String nickname = uniqueNickname("두현");

    saveProfile(userId, profileBody(nickname))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("ACTIVE"))
        .andExpect(jsonPath("$.data.nextStep").value("BAND_START"));

    mockMvc
        .perform(get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, bearer(userId)))
        .andExpect(jsonPath("$.data.status").value("ACTIVE"))
        .andExpect(jsonPath("$.data.nickname").value(nickname))
        // 중복 코드는 한 번만, 직접 입력 값은 앞뒤 공백 제거
        .andExpect(jsonPath("$.data.genreCodes").value(contains("ROCK", "ETC")))
        .andExpect(jsonPath("$.data.genreCustomName").value("시티팝"))
        .andExpect(jsonPath("$.data.positionCodes").value(contains("VOCAL")))
        .andExpect(jsonPath("$.data.positionCustomName").value(nullValue()))
        .andExpect(jsonPath("$.data.nextStep").value("BAND_START"));
  }

  @Test
  @DisplayName("프로필을 다시 저장하면 장르·포지션을 통째로 바꾼다 (같은 코드를 다시 골라도 된다)")
  void saveProfileAgainReplacesCodes() throws Exception {
    Long userId = newUser();
    String nickname = uniqueNickname("수정");
    saveProfile(userId, profileBody(nickname)).andExpect(status().isOk());

    saveProfile(
            userId,
            "{\"nickname\":\""
                + nickname
                + "\",\"genreCodes\":[\"ROCK\",\"JAZZ\"],"
                + "\"positionCodes\":[\"CUSTOM\",\"VOCAL\"],\"positionCustomName\":\"하모니카\"}")
        .andExpect(status().isOk());

    mockMvc
        .perform(get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, bearer(userId)))
        .andExpect(jsonPath("$.data.genreCodes").value(contains("ROCK", "JAZZ")))
        .andExpect(jsonPath("$.data.genreCustomName").value(nullValue()))
        .andExpect(jsonPath("$.data.positionCodes").value(contains("CUSTOM", "VOCAL")))
        .andExpect(jsonPath("$.data.positionCustomName").value("하모니카"));
  }

  @Test
  @DisplayName("다른 회원이 쓰는 닉네임으로 저장하면 409 USER_001, 대소문자만 다르면 저장된다")
  void duplicateNicknameIsRejected() throws Exception {
    String nickname = uniqueNickname("Rock");
    saveProfile(newUser(), profileBody(nickname)).andExpect(status().isOk());

    saveProfile(newUser(), profileBody(nickname))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("USER_001"));
    saveProfile(newUser(), profileBody(nickname.toLowerCase())).andExpect(status().isOk());
  }

  @Test
  @DisplayName("직접 입력 값을 빠뜨리거나 고르지 않은 코드의 값을 보내면 COMMON 004")
  void customNameMustMatchSelectedCodes() throws Exception {
    Long userId = newUser();

    saveProfile(
            userId,
            "{\"nickname\":\""
                + uniqueNickname("짝")
                + "\",\"genreCodes\":[\"ETC\"],"
                + "\"positionCodes\":[\"VOCAL\"],\"positionCustomName\":\"하모니카\"}")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("COMMON_004"))
        .andExpect(
            jsonPath("$.errors[*].field")
                .value(
                    org.hamcrest.Matchers.containsInAnyOrder(
                        "genreCustomName", "positionCustomName")));
  }

  @Test
  @DisplayName("없는 장르·포지션 코드는 COMMON 004이고 필드 위치를 알려 준다")
  void unknownCodesAreRejected() throws Exception {
    saveProfile(
            newUser(),
            "{\"nickname\":\""
                + uniqueNickname("코드")
                + "\",\"genreCodes\":[\"ROCK\",\"TROT\"],"
                + "\"positionCodes\":[\"SINGER\"]}")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("COMMON_004"))
        .andExpect(
            jsonPath("$.errors[*].field")
                .value(
                    org.hamcrest.Matchers.containsInAnyOrder("genreCodes[1]", "positionCodes[0]")));
  }

  @Test
  @DisplayName("닉네임 확인: 길이·문자 오류와 중복은 200 + available false, 내 닉네임·대소문자 다른 값은 사용 가능")
  void nicknameAvailability() throws Exception {
    Long owner = newUser();
    String taken = uniqueNickname("Band");
    saveProfile(owner, profileBody(taken)).andExpect(status().isOk());
    Long other = newUser();

    checkNickname(other, "").andExpect(jsonPath("$.data.reason").value("INVALID_LENGTH"));
    checkNickname(other, "가").andExpect(jsonPath("$.data.reason").value("INVALID_LENGTH"));
    checkNickname(other, "가나다라마바사아자차카")
        .andExpect(jsonPath("$.data.reason").value("INVALID_LENGTH"));
    checkNickname(other, "두현!").andExpect(jsonPath("$.data.reason").value("INVALID_CHARACTER"));
    checkNickname(other, "두 현").andExpect(jsonPath("$.data.reason").value("INVALID_CHARACTER"));
    checkNickname(other, taken)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.available").value(false))
        .andExpect(jsonPath("$.data.reason").value("DUPLICATED"));
    checkNickname(other, taken.toLowerCase())
        .andExpect(jsonPath("$.data.available").value(true))
        .andExpect(jsonPath("$.data.reason").value(nullValue()));
    checkNickname(owner, taken).andExpect(jsonPath("$.data.available").value(true));
  }

  @Test
  @DisplayName("토큰의 회원이 없으면 404 USER_005")
  void unknownUserIsNotFound() throws Exception {
    mockMvc
        .perform(get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, bearer(999_999_999L)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("USER_005"));
  }

  // 장르 ETC만 고르고 직접 입력 값을 바꿔 가며 저장한다
  private ResultActions saveWithGenreCustomName(String genreCustomNameJson) throws Exception {
    return saveProfile(
        newUser(),
        "{\"nickname\":\""
            + uniqueNickname("입력")
            + "\",\"genreCodes\":[\"ETC\"],"
            + "\"genreCustomName\":"
            + genreCustomNameJson
            + ",\"positionCodes\":[\"VOCAL\"]}");
  }

  @Test
  @DisplayName("직접 입력 값: 10자 초과·중간 공백·자모·특수문자·공백만은 COMMON 004")
  void customNameFormatIsValidated() throws Exception {
    for (String invalid :
        new String[] {"\"가나다라마바사아자차카\"", "\"시티 팝\"", "\"ㅋㅋ\"", "\"팝#\"", "\"   \""}) {
      saveWithGenreCustomName(invalid)
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("COMMON_004"))
          .andExpect(jsonPath("$.errors[0].field").value("genreCustomName"));
    }
    // 앞뒤 공백만 빼면 10자면 통과
    saveWithGenreCustomName("\"  가나다라마바사아자차  \"").andExpect(status().isOk());
  }

  @Test
  @DisplayName("직접입력(CUSTOM)을 고르지 않았으면 공백만 있는 값은 보낸 것으로 보지 않는다")
  void blankCustomNameWithoutSelectionIsIgnored() throws Exception {
    saveProfile(
            newUser(),
            "{\"nickname\":\""
                + uniqueNickname("공백")
                + "\",\"genreCodes\":[\"ROCK\"],"
                + "\"positionCodes\":[\"VOCAL\"],\"positionCustomName\":\"  \"}")
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("닉네임에 자모·이모지가 있으면 확인은 INVALID_CHARACTER, 저장은 COMMON 004")
  void nicknameRejectsJamoAndEmoji() throws Exception {
    Long userId = newUser();
    for (String nickname : new String[] {"ㅋㅋㅋ", "🎸🎸"}) {
      checkNickname(userId, nickname)
          .andExpect(jsonPath("$.data.available").value(false))
          .andExpect(jsonPath("$.data.reason").value("INVALID_CHARACTER"));
      saveProfile(userId, profileBody(nickname))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("COMMON_004"))
          .andExpect(jsonPath("$.errors[0].field").value("nickname"));
    }
  }

  @Test
  @DisplayName("프로필 사진 주소가 500자를 넘으면 COMMON 004, 빈 문자열은 사진 없음(null)으로 저장")
  void profileImageUrlLength() throws Exception {
    String tooLong = "https://cdn.example.com/" + "a".repeat(500);
    saveProfile(
            newUser(),
            profileBody(uniqueNickname("사진"))
                .replace("\"profileImageUrl\":null", "\"profileImageUrl\":\"" + tooLong + "\""))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("COMMON_004"))
        .andExpect(jsonPath("$.errors[0].field").value("profileImageUrl"));

    Long userId = newUser();
    saveProfile(
            userId,
            profileBody(uniqueNickname("빈사진"))
                .replace("\"profileImageUrl\":null", "\"profileImageUrl\":\"\""))
        .andExpect(status().isOk());
    mockMvc
        .perform(get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, bearer(userId)))
        .andExpect(jsonPath("$.data.profileImageUrl").value(nullValue()));
  }

  @Test
  @DisplayName("프로필 저장도 토큰의 회원이 없으면 404 USER_005")
  void saveProfileOfUnknownUserIsNotFound() throws Exception {
    saveProfile(999_999_999L, profileBody(uniqueNickname("없음")))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("USER_005"));
  }
}
