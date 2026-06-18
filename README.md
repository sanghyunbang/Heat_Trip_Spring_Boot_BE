# Heat Trip Backend

Spring Boot 기반 여행 추천 및 일정 관리 백엔드입니다.

## 주요 프로젝트 링크

- **포트폴리오 / 프로젝트 설명**: [Heat Trip Notion](https://app.notion.com/p/Heat-Trip-321b82bc8b718166a51fd382c51d96b5?source=copy_link)
- **리팩토링 기록**: [Heat Trip 프로젝트 리펙토링](https://velog.io/@sanghyunbang/series/Heat-Trip-%ED%94%84%EB%A1%9C%EC%A0%9D%ED%8A%B8-%EB%A6%AC%ED%8E%99%ED%86%A0%EB%A7%81)

이 저장소는 public 전환을 전제로 정리 중입니다. 코드와 예시 설정만 저장소에 두고, 실제 운영 비밀값과 운영 runbook, 배포 상세는 저장소 밖에서 관리하는 구조를 기준으로 합니다.

## 기술 스택

- Java 21
- Spring Boot 3.5.x
- Spring Security / OAuth2 / JWT
- Spring Data JPA
- MySQL
- WebClient / WebFlux
- AWS S3
- Spring AI / OpenAI
- Docker Compose

## 빠른 시작

### 1. 예시 설정 복사

다음 예시 파일을 복사해서 로컬 실행용 설정을 준비합니다.

- `.env.example`

예시:

```bash
cp .env.example .env
```

로컬 개발 실행은 `.env`의 `SPRING_PROFILES_ACTIVE=dev`를 사용합니다. 운영 배포는 서버의 `.env`에서 `SPRING_PROFILES_ACTIVE=prod`로 지정합니다.

### 2. 비밀값 주입

다음 값들은 Git에 커밋하면 안 됩니다.

- DB 비밀번호
- JWT secret
- OAuth client secret
- AWS access key / secret key
- OpenAI API key
- Slack webhook
- Tour API key

### 3. Docker Compose로 실행

```bash
docker compose up -d --build
```

### 4. Gradle로 실행

```bash
SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun
```

Windows 환경:

```powershell
$env:SPRING_PROFILES_ACTIVE="dev"
.\gradlew.bat bootRun
```

## 설정 구조

- 공통 설정: `src/main/resources/application.properties`
- 개발 프로필 설정: `src/main/resources/application-dev.properties`
- 운영 프로필 설정: `src/main/resources/application-prod.properties`
- 테스트 전용 설정: 필요한 테스트에서 `@DataJpaTest(properties = ...)`처럼 개별 지정
- 로컬/런타임 비밀값: `.env`

공통 설정에는 비밀값이 아닌 기본값만 둡니다. 개발/운영 프로필 파일에는 환경별 연결 설정을 두고, 실제 비밀값은 `.env` 또는 서버 환경변수로 주입합니다.

Docker MySQL 초기화 값은 `.env`에 두고, `docker-compose.yml`의 `services.mysql.environment`에서 읽습니다.

- `MYSQL_ROOT_PASSWORD`
- `MYSQL_DATABASE`
- `MYSQL_USER`
- `MYSQL_PASSWORD`

이 값들은 `mysql_data` 볼륨이 처음 생성될 때만 적용됩니다. 이후 `.env`를 수정해도 기존 MySQL 볼륨의 계정이나 비밀번호가 자동으로 변경되지는 않습니다.

LLM recommender base URL 예시:

- Docker 내부 네트워크: `http://recommender:8000`
- 호스트 로컬 접근: `http://127.0.0.1:8000`

## 보안 메모

- 실제 secret 값은 저장소에 두지 않습니다.
- 운영 배포 방식과 내부 운영 문서는 저장소 밖 또는 로컬 전용으로 관리합니다.
- public 저장소에서는 운영용 self-hosted workflow를 제외하는 방향을 권장합니다.

## 자동화

### 비밀값 스캔

GitHub Actions에서 `gitleaks` 기반 secret scan을 실행합니다.

- 워크플로 파일: `.github/workflows/secret-scan.yml`

### 요청 제한

애플리케이션 레벨의 최소 rate limit 보호가 포함되어 있습니다.

- 구현 위치: `src/main/java/com/heattrip/heat_trip_backend/security/ApiRateLimitFilter.java`
- 설정 위치: `src/main/resources/application.properties`, `.env.example`

운영에서는 애플리케이션 내부 제한만으로 끝내지 말고, gateway / WAF / edge 계층의 1차 제한을 같이 두는 편이 안전합니다.

## 문서 관리

- `docs/` 아래에서 역할별로 문서를 관리합니다.
- 운영용 workflow 와 내부 배포 상세는 로컬 전용으로 관리합니다.
