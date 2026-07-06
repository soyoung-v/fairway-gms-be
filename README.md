# Fairway GMS — 골프장 캐디 배정 관리 시스템 백엔드

골프장의 캐디 배정, 순번 관리, 정산, 게시판을 통합 관리하는 백오피스 + 캐디 모바일 앱 백엔드입니다.

---

## 기술 스택

| 구분 | 기술 |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.1.0 (WebMVC) |
| Security | Spring Security 6 + JWT (jjwt 0.12.6) |
| ORM | Spring Data JPA + Hibernate |
| DB | MySQL 9.x |
| Push | Firebase Admin SDK 9.4.3 (FCM) |
| Docs | springdoc-openapi 2.8.9 (Swagger UI) |
| File | Apache POI 5.3.0 (엑셀 업로드/다운로드) |
| Build | Gradle 9 |

---

## 아키텍처

**Modular Monolith** — 단일 Spring Boot 애플리케이션, 단일 MySQL DB

MSA 전환 가능성을 고려해 도메인 경계를 명확히 분리했으나, 팀 규모와 초기 운영 비용을 고려해 단일 배포 단위로 유지합니다.

```
com.fairwaygms.fairwaygmsbe
├── common        # 공통 응답/예외/보안/설정
├── auth          # 인증, 사용자 승인 관리
├── golfcourse    # 골프장·코스·카트 관리
├── caddie        # 캐디 관리, 순번, 일일 상태
├── operation     # 운영 설정, 티타임, 예약팀
├── assignment    # 캐디 배정, 배정표, 카트 배정
├── settlement    # 캐디피 정책, 월 정산
├── notification  # FCM 알림, 알림 설정
└── board         # 게시글, 댓글, 교환 요청
```

각 도메인은 `application / domain / exception / infrastructure` 4계층 구조를 따릅니다.

---

## 주요 기능

### 인증 (auth)
- 이메일 회원가입 / 로그인 / 로그아웃
- **카카오 소셜 로그인** (Spring Security OAuth2 Client) — 최초 가입 시 임시 가입 토큰 쿠키로 역할/골프장 선택 절차 연결
- JWT AccessToken + RefreshToken (HttpOnly Cookie, Refresh Token Rotation)
- 비밀번호 재설정 (Gmail SMTP, SHA-256 토큰 해시)
- 관리자(ADMIN) 사용자 승인·거절·퇴사 처리
- 매니저(MANAGER) 캐디 계정 승인·거절 (소속 골프장 한정)
- 탈퇴 시 이메일 익명화로 소프트 삭제와 UNIQUE(email) 공존 (재가입 가능)

### 캐디 배정 (assignment)
- 수동/자동 배정, 그룹 일괄 배정
- 자동배정: 캐디 그룹별 순번 이월 알고리즘
- 배정표 DRAFT → CONFIRMED → COMPLETED 상태 전이
- 카트 배정 / 반납 / 자동배정

### 이벤트 기반 후속 처리
배정 확정·완료 시 `@TransactionalEventListener(AFTER_COMMIT)` + `@Transactional(REQUIRES_NEW)` 조합으로 독립 처리합니다.

| 이벤트 | 후속 처리 |
|---|---|
| 배정 확정 | 캐디 FCM 알림, 시간표 게시글 자동 생성 |
| 배정 완료 | 정산 기록 스냅샷 생성, 카트 AVAILABLE 자동 복원 |

### 정산 (settlement)
- 배정 완료 시점의 캐디피를 스냅샷으로 저장 → 이후 정책 변경과 무관하게 과거 정산 재현 가능
- 월 정산 집계 / 확정 / 수동 조정 / 이력

### FCM 알림 (notification)
- 인터페이스(`FcmPushService`) 기반 구현 분리: `MockFcmPushService`(local/test) / `RealFcmPushService`(prod)
- 알림 설정, FCM 토큰 등록/해제

---

## 인증 방식

JWT를 **HttpOnly Cookie**로 관리합니다.

```
AccessToken  → 쿠키명: at  (경로: /)
RefreshToken → 쿠키명: rt  (경로: /api/auth — 재발급 + 로그아웃 시 DB 토큰 폐기용)
```

- 프론트엔드는 JWT를 직접 읽거나 저장하지 않습니다.
- Refresh Token Rotation: 재발급 시 기존 RT를 폐기하고 새 RT를 발급합니다.

---

## 역할 구조

| 역할 | 설명 |
|---|---|
| `ADMIN` | 플랫폼 전체 관리. `X-Selected-Golf-Course-Id` 헤더로 대상 골프장 선택 |
| `MANAGER` | 소속 골프장 운영 관리 (JWT claim 기준) |
| `CADDY` | 캐디 모바일 앱 사용자 (JWT claim 기준) |

---

## 로컬 실행

### 사전 준비

- Java 21
- MySQL 9.x (스키마 수동 생성 필요)
- `.env` 파일 생성 (`.env.example` 참고)

### `.env` 파일 설정

```properties
DB_URL=jdbc:mysql://localhost:3306/fairway_gms
DB_USERNAME=root
DB_PASSWORD=your_password
JWT_SECRET=your-256bit-secret
JWT_ISSUER=fairway-gms
JWT_ACCESS_VALIDITY_SECONDS=1800
JWT_REFRESH_VALIDITY_SECONDS=604800
JWT_ACCESS_COOKIE_NAME=at
JWT_REFRESH_COOKIE_NAME=rt
JWT_ACCESS_COOKIE_PATH=/
JWT_REFRESH_COOKIE_PATH=/api/auth
JWT_COOKIE_SECURE=false
JWT_COOKIE_HTTP_ONLY=true
JWT_COOKIE_SAME_SITE=Lax
FAIRWAY_GMS_BOOTSTRAP_ADMIN_ENABLED=true
FAIRWAY_GMS_BOOTSTRAP_ADMIN_EMAIL=admin@fairway.com
FAIRWAY_GMS_BOOTSTRAP_ADMIN_PASSWORD=Admin1234!
FAIRWAY_GMS_BOOTSTRAP_ADMIN_NAME=관리자
FAIRWAY_GMS_FRONTEND_URL=http://localhost:3000
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your@gmail.com
MAIL_PASSWORD=your-app-password
JPA_DDL_AUTO=update
JPA_SHOW_SQL=true
FIREBASE_CREDENTIALS_PATH=
```

### 실행

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

Swagger UI: `http://localhost:8080/swagger-ui.html`

---

## API 문서

Swagger UI에서 전체 API를 확인할 수 있습니다. 코드에서 런타임 자동 생성되므로 항상 최신입니다.

- **25개 컨트롤러**, 도메인별 Tag 그룹핑
- `ADMIN` 골프장 범위 API에만 `X-Selected-Golf-Course-Id` 헤더 자동 표시 (`@AdminScopeApi`)
- HttpOnly Cookie 인증 방식 문서화

---

## 배포

| 구성 | 서비스 |
|---|---|
| 백엔드 | AWS EC2 (t3.micro) + Docker + nginx (HTTPS, Let's Encrypt) |
| DB | AWS RDS MySQL 8.4 |
| 이미지 저장소 | AWS ECR |
| 프론트엔드 | Vercel |
| 도메인 | DuckDNS |

**CI/CD**: GitHub Actions — `main` 브랜치 push 시 자동으로 빌드 → ECR 푸시 → EC2 배포 → `/actuator/health` 헬스체크까지 수행합니다 (`.github/workflows/deploy.yml`).

- prod는 nginx 뒤에서 TLS가 종료되므로 `server.forward-headers-strategy: framework`로 OAuth2 redirect_uri를 https로 복원합니다.
- 운영 환경변수는 EC2의 `.env.production` 파일로 주입하며(`--env-file`), 비밀값은 git과 워크플로우 로그에 노출되지 않습니다.

---

## 테스트

```bash
# 전체 빌드 + 테스트
./gradlew clean build

# 통합 테스트 포함 실행 (로컬 MySQL 필요)
./gradlew test
```

- 단위 테스트: 도메인 핵심 규칙 검증 (Validation, 상태 전이, 권한)
- 통합 테스트: 로컬 MySQL 기반, `@Transactional` 자동 롤백으로 데이터 격리
