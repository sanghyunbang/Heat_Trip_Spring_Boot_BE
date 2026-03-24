# Heat Trip Backend

Spring Boot 기반 여행 추천 및 일정 관리 백엔드입니다.

이 저장소는 public 전환을 전제로 정리 중입니다. 코드와 예시 설정만 저장소에 두고, 실제 운영 비밀값과 운영 runbook, 배포 상세는 저장소 밖에서 관리하는 구조를 기준으로 합니다.

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
- `application-private.properties.example`

예시:

```bash
cp .env.example .env
cp application-private.properties.example application-private.properties
```

Docker 실행 시에는 `./config/application-private.properties` 경로로 마운트해도 됩니다.

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
./gradlew bootRun
```

Windows:

```powershell
.\gradlew.bat bootRun
```

## Config Layout

- Public base config: `src/main/resources/application.properties`
- Private local or runtime config: `application-private.properties`
- Docker environment: `.env`

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

## Public Conversion Policy

- `documents/` 는 로컬 전용 내부 문서로 유지하고 Git 추적에서 제외합니다.
- 운영용 workflow 와 history cleanup 보조 파일도 로컬 전용으로 관리합니다.
- public 전환 전에는 history rewrite 와 secret scan 재검증이 필요합니다.
