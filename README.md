# Heat Trip Backend

Spring Boot 기반 여행 추천 및 일정 관리 백엔드입니다.

이 저장소는 public 전환을 전제로 정리 중입니다. 코드와 예시 설정만 저장소에 두고, 실제 운영 비밀값과 운영 runbook, 배포 상세는 저장소 밖에서 관리하는 구조를 기준으로 합니다.

## Project Links

- Portfolio / project overview: [Heat Trip Notion](https://app.notion.com/p/Heat-Trip-321b82bc8b718166a51fd382c51d96b5?source=copy_link)
- Refactoring notes: [코틀린 관련 플러그인과 라이브러리](https://velog.io/@sanghyunbang/%EC%BD%94%ED%8B%80%EB%A6%B0-%EA%B4%80%EB%A0%A8-%ED%94%8C%EB%9F%AC%EA%B7%B8%EC%9D%B8%EA%B3%BC-%EB%9D%BC%EC%9D%B4%EB%B8%8C%EB%9F%AC%EB%A6%AC)

## Stack

- Java 21
- Spring Boot 3.5.x
- Spring Security / OAuth2 / JWT
- Spring Data JPA
- MySQL
- WebClient / WebFlux
- AWS S3
- Spring AI / OpenAI
- Docker Compose

## Quick Start

### 1. Example config copy

다음 예시 파일을 복사해서 로컬 실행용 설정을 준비합니다.

- `.env.example`

예시:

```bash
cp .env.example .env
```

로컬 개발 실행은 `.env`의 `SPRING_PROFILES_ACTIVE=dev`를 사용합니다. 운영 배포는 서버의 `.env`에서 `SPRING_PROFILES_ACTIVE=prod`로 지정합니다.

### 2. Secrets injection

다음 값들은 Git에 커밋하면 안 됩니다.

- DB password
- JWT secret
- OAuth client secret
- AWS access key / secret key
- OpenAI API key
- Slack webhook
- Tour API key

### 3. Run with Docker Compose

```bash
docker compose up -d --build
```

### 4. Run with Gradle

```bash
SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun
```

Windows:

```powershell
$env:SPRING_PROFILES_ACTIVE="dev"
.\gradlew.bat bootRun
```

## Config Layout

- Public base config: `src/main/resources/application.properties`
- Development profile: `src/main/resources/application-dev.properties`
- Production profile: `src/main/resources/application-prod.properties`
- Test overrides: local to individual tests, for example `@DataJpaTest(properties = ...)`
- Local/runtime secrets: `.env`

The base config contains shared non-secret defaults only. Profile files contain environment-specific wiring. Secrets are injected through `.env` or real environment variables.

Docker MySQL initialization values live in `.env` and are read by `docker-compose.yml` under `services.mysql.environment`:

- `MYSQL_ROOT_PASSWORD`
- `MYSQL_DATABASE`
- `MYSQL_USER`
- `MYSQL_PASSWORD`

These values are applied only when the `mysql_data` volume is first created. Changing them later does not update an existing MySQL volume automatically.

LLM recommender base URL examples:

- Docker internal network: `http://recommender:8000`
- Host local access: `http://127.0.0.1:8000`

## Security Notes

- 실제 secret 값은 저장소에 두지 않습니다.
- 운영 배포 방식과 내부 운영 문서는 저장소 밖 또는 로컬 전용으로 관리합니다.
- public 저장소에서는 운영용 self-hosted workflow를 제외하는 방향을 권장합니다.

## Automation

### Secret scan

GitHub Actions에서 `gitleaks` 기반 secret scan을 실행합니다.

- Workflow: `.github/workflows/secret-scan.yml`

### Rate limit

애플리케이션 레벨의 최소 rate limit 보호가 포함되어 있습니다.

- Implementation: `src/main/java/com/heattrip/heat_trip_backend/security/ApiRateLimitFilter.java`
- Config: `src/main/resources/application.properties`, `.env.example`

운영에서는 애플리케이션 내부 제한만으로 끝내지 말고, gateway / WAF / edge 계층의 1차 제한을 같이 두는 편이 안전합니다.

## Documentation Policy

- `docs/` 아래에서 역할별로 문서를 관리합니다.
- 운영용 workflow 와 내부 배포 상세는 로컬 전용으로 관리합니다.
