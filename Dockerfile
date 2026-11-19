# 배포용 이미지. docker build -t highfive-api . 만으로 빌드된다 (로컬에 JDK 없어도 됨)

# 1단계: jar 빌드
FROM eclipse-temurin:17-jdk AS build
WORKDIR /workspace

# 의존성만 먼저 받아 두면 소스만 바뀐 빌드는 이 레이어를 다시 쓴다
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN ./gradlew dependencies --no-daemon > /dev/null

# 테스트·포맷 검사는 CI(./gradlew check)에서 이미 돌린다
COPY src src
RUN ./gradlew bootJar --no-daemon

# 2단계: 실행 (JRE만)
FROM eclipse-temurin:17-jre
ENV TZ=Asia/Seoul
WORKDIR /app

RUN groupadd --system app && useradd --system --gid app app
COPY --from=build /workspace/build/libs/*.jar app.jar
USER app

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
