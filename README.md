# Heat Trip Backend

Spring Boot 기반의 여행 추천 및 탐색 백엔드입니다.

이 저장소는 `public 저장소를 실제 운영 기준 저장소로 사용`하는 구조를 전제로 정리되고 있습니다.  
즉, 소스코드는 공개하되 실제 secret과 운영 환경 정보는 저장소 밖에서 주입합니다.

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

## 프로젝트 목적

- 장소 탐색 API
- 사용자 인증 및 프로필 API
- 여행 일정 및 기록 API
- 북마크 / 컬렉션 API
- 감정 기반 추천 API
- 미디어 업로드 API

## 빠른 시작

### 1. 예시 설정 파일 준비

다음 두 파일을 참고해서 실제 실행용 파일을 준비합니다.

- [.env.example](/C:/Users/mm206/git_projects/heat_trip_backend/.env.example)
- [application-private.properties.example](/C:/Users/mm206/git_projects/heat_trip_backend/application-private.properties.example)

로컬 또는 서버에서 아래처럼 준비합니다.

- `.env.example` -> `.env`
- `application-private.properties.example` -> `application-private.properties`

또는 Docker 실행 시 `./config/application-private.properties`로 마운트할 수 있습니다.

### 2. 비밀값 주입

아래 값들은 저장소에 커밋하면 안 됩니다.

- DB 비밀번호
- JWT secret
- OAuth client secret
- AWS access key / secret key
- OpenAI API key
- Slack webhook
- Tour API key
- 운영용 `.env` 실제값
- 운영용 `docker-compose.yml` 내 평문 secret

### 3. Docker Compose 실행

```bash
docker compose up -d --build
```

### 4. Gradle로 로컬 실행

```bash
./gradlew bootRun
```

Windows:

```powershell
.\gradlew.bat bootRun
```

## 설정 구조

- 공개 가능한 기본 설정: [src/main/resources/application.properties](/C:/Users/mm206/git_projects/heat_trip_backend/src/main/resources/application.properties)
- 실제 운영/로컬 비밀 설정: `application-private.properties`
- Docker용 환경변수: `.env`

LLM recommender 연결 원칙:

- Docker Compose 내부 통신이면 `LLM_RECOMMENDER_BASE_URL=http://recommender:8000`
- host-local 직접 호출일 때만 `http://127.0.0.1:8000`
- 운영에서는 recommender 를 `*:8000` 으로 열지 않는 편이 맞습니다.

애플리케이션은 아래 위치의 private 설정을 읽습니다.

- 현재 작업 디렉터리의 `./application-private.properties`
- Docker 마운트 경로 `/config/application-private.properties`

## 공개 운영 원칙

- 코드는 public이어도 실제 secret은 저장소에 두지 않습니다.
- 운영 배포 정보와 민감한 인프라 정보는 저장소 밖에서 관리합니다.
- 비용이 큰 API와 사용자 데이터 API는 인증 및 남용 방어가 필요합니다.

## 보안 자동화

### Secret Scan

GitHub Actions에서 `gitleaks`로 secret scan을 수행합니다.

- workflow: [secret-scan.yml](/C:/Users/mm206/git_projects/heat_trip_backend/.github/workflows/secret-scan.yml)
- 목적: 실수로 API key, webhook, 비밀번호 등이 커밋되는 것을 방지

### Rate Limit

현재는 앱 레벨 최소 보호선이 들어가 있습니다.

- 구현: [ApiRateLimitFilter.java](/C:/Users/mm206/git_projects/heat_trip_backend/src/main/java/com/heattrip/heat_trip_backend/security/ApiRateLimitFilter.java)
- 설정: [application.properties](/C:/Users/mm206/git_projects/heat_trip_backend/src/main/resources/application.properties), [.env.example](/C:/Users/mm206/git_projects/heat_trip_backend/.env.example)

현재 기본 보호 대상:

- 로그인
- 추천 API
- 검색 API
- 피드백 API
- 업로드 API

중요:

- 이 rate limit은 앱 내부 fallback 보호선입니다.
- 실제 운영에서는 Nginx, Gateway, WAF, Cloudflare 같은 바깥 계층에서 1차 rate limit을 두는 것이 더 좋습니다.

## 관련 문서

공개 운영 전환 관련 문서는 아래를 먼저 보면 됩니다.

- [documents/public_operation_transition/0_plan.md](/C:/Users/mm206/git_projects/heat_trip_backend/documents/public_operation_transition/0_plan.md)
- [documents/PublicTransitionPlan.md](/C:/Users/mm206/git_projects/heat_trip_backend/documents/PublicTransitionPlan.md)
- [documents/PublicRepoFilePolicy.md](/C:/Users/mm206/git_projects/heat_trip_backend/documents/PublicRepoFilePolicy.md)
- [documents/PublicSecurityAssessment.md](/C:/Users/mm206/git_projects/heat_trip_backend/documents/PublicSecurityAssessment.md)

단계별 작업 기록:

- [documents/public_operation_transition/1_db_refactoring.md](/C:/Users/mm206/git_projects/heat_trip_backend/documents/public_operation_transition/1_db_refactoring.md)
- [documents/public_operation_transition/2_config_examples.md](/C:/Users/mm206/git_projects/heat_trip_backend/documents/public_operation_transition/2_config_examples.md)
- [documents/public_operation_transition/3_deployment_publicization.md](/C:/Users/mm206/git_projects/heat_trip_backend/documents/public_operation_transition/3_deployment_publicization.md)
- [documents/public_operation_transition/4_security_boundary_redesign.md](/C:/Users/mm206/git_projects/heat_trip_backend/documents/public_operation_transition/4_security_boundary_redesign.md)
- [documents/public_operation_transition/5_current_status.md](/C:/Users/mm206/git_projects/heat_trip_backend/documents/public_operation_transition/5_current_status.md)
- [documents/public_operation_transition/6_secret_scan.md](/C:/Users/mm206/git_projects/heat_trip_backend/documents/public_operation_transition/6_secret_scan.md)
- [documents/public_operation_transition/7_rate_limit.md](/C:/Users/mm206/git_projects/heat_trip_backend/documents/public_operation_transition/7_rate_limit.md)
- [documents/public_operation_transition/20_mac_mini_actual_state_2026_03_18.md](/C:/Users/mm206/git_projects/heat_trip_backend/documents/public_operation_transition/20_mac_mini_actual_state_2026_03_18.md)
- [documents/public_operation_transition/21_secret_separation_and_recommender_hardening.md](/C:/Users/mm206/git_projects/heat_trip_backend/documents/public_operation_transition/21_secret_separation_and_recommender_hardening.md)
- [documents/public_operation_transition/22_cicd_option_review.md](/C:/Users/mm206/git_projects/heat_trip_backend/documents/public_operation_transition/22_cicd_option_review.md)
- [documents/public_operation_transition/23_backend_and_recommender_cicd_design.md](/C:/Users/mm206/git_projects/heat_trip_backend/documents/public_operation_transition/23_backend_and_recommender_cicd_design.md)

## 현재 상태

현재 저장소는 public 운영형 구조로 정리 중입니다.

이미 반영된 항목:

- `docker-compose.yml`의 평문 DB 비밀번호 제거
- 예시 설정 파일 추가
- 배포 workflow 절대경로 제거
- 공개 범위가 넓던 일부 보안 설정 축소
- `gitleaks` 기반 secret scan CI 추가
- 앱 레벨 최소 rate limit 추가

아직 권장되는 항목:

- gateway 또는 WAF 레벨 rate limit 추가
- secret scan 정책 커스터마이징
- README와 배포 문서 추가 보완
