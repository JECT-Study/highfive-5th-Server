# highfive-5th-Server

아마추어 밴드들이 합동 공연을 함께 준비하는 앱 highfive의 백엔드입니다.

- Java 17, Spring Boot 4.1.2, Gradle
- MySQL 8.4, Redis 7

## 로컬 실행

### 1. MySQL·Redis 준비

**Docker를 쓰는 경우**

```bash
docker compose up -d
```

`compose.yaml`이 MySQL과 Redis를 띄웁니다. DB(`highfive`)와 계정(`highfive` / `highfive`)이 자동으로 만들어져서 따로 설정할 것이 없습니다.

끌 때는 `docker compose down`을 실행합니다. 데이터는 볼륨에 남습니다.

**MySQL을 직접 설치한 경우**

MySQL 8.4, Redis 7로 버전을 맞추고, DB를 먼저 만듭니다.

```sql
CREATE DATABASE highfive CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

그다음 `.env.example`을 `.env`로 복사하고 자기 계정과 포트를 적습니다.

```
DB_PORT=3306
DB_USERNAME=root
DB_PASSWORD=내_비밀번호
```

### 2. 서버 실행

```bash
./gradlew bootRun
```

프로필을 지정하지 않으면 `local`로 실행됩니다. `local` 프로필은 루트의 `.env`를 읽고, 없으면 `compose.yaml`의 기본값으로 접속합니다.

### 포트가 겹칠 때

이미 3306이나 6379를 쓰고 있다면 `.env`에서 `DB_PORT`, `REDIS_PORT`를 바꿉니다. `compose.yaml`과 Spring이 같은 값을 읽습니다.

## 설정 파일

| 파일 | 용도 |
| --- | --- |
| `application.yml` | 공통 설정. 접속 정보는 환경변수로 받는다 |
| `application-local.yml` | 로컬 개발용 (기본 프로필) |
| `src/test/resources/application.yml` | 테스트용. 기본은 H2 인메모리 DB, MySQL 전용 쿼리를 검사하는 리포지토리 테스트는 Testcontainers로 MySQL을 띄운다 |

비밀값은 레포에 커밋하지 않습니다. 로컬 값은 `.env`에 두고, 새 변수를 추가하면 `.env.example`에도 이름을 적어 주세요.

테스트에 Testcontainers가 포함돼 있어서 `./gradlew check`를 돌릴 때는 Docker가 켜져 있어야 합니다.

## 배포 (dev 서버)

dev 서버(EC2)는 GitHub Actions → ECR → CodeDeploy로 배포합니다. 배포 워크플로는 AWS 구성 후에 추가합니다.

| 파일 | 용도 |
| --- | --- |
| `Dockerfile` | 배포 이미지. `docker build -t highfive-api .`로 로컬에서도 빌드할 수 있다 |
| `deploy/compose.yaml` | dev 서버에서 앱·MySQL·Redis를 띄운다 |
| `deploy/appspec.yml`, `deploy/scripts/` | CodeDeploy가 실행하는 배포·헬스 체크 스크립트 |
| `deploy/.env.example` | dev 서버의 `/home/ubuntu/config/.env`에 둘 변수 목록 |

헬스 체크는 `/actuator/health`를 씁니다.

기존 DB에 JECT-212를 배포할 때는 [소셜 ID 마이그레이션](deploy/migrations/JECT-212-provider-user-id-collation.sql)의 사전 점검 후 필요하면 SQL을 수동으로 적용합니다.

## PR 전에

```bash
./gradlew spotlessApply   # 포맷 자동 정렬 (Google Java Style)
./gradlew check           # 포맷 검사 + Checkstyle + 테스트
```

`./gradlew check`가 통과해야 PR을 올릴 수 있습니다. PR을 올리면 GitHub Actions(`.github/workflows/ci.yml`)가 같은 검사를 다시 돌립니다. 실패하면 Actions 탭의 `reports` 아티팩트에서 Checkstyle·테스트 리포트를 볼 수 있습니다.
