# FairwayGMS — 골프장 캐디 배정 관리 시스템 (Backend)

골프장의 **캐디 배정·순번 관리·정산·공지**를 통합 관리하는 백오피스(관리자/매니저 웹) + 캐디 모바일 PWA의 백엔드입니다.

골프장 실무 경험을 바탕으로 도메인을 직접 설계한 **1인 개발 프로젝트**이며,
기능 완성도와 함께 **AWS 클라우드 배포·CI/CD 파이프라인을 처음부터 끝까지 직접 구축·운영하는 경험**에 초점을 두었습니다.
현재도 지속적으로 기능 보완과 리팩토링을 진행 중입니다.

---

## 배포 링크

| 구분 | 링크 |
|---|---|
| 서비스 (관리자 웹 + 캐디 PWA) | https://fairway-gms-fe.vercel.app |
| API 서버 | https://fairway-gms.duckdns.org |
| API 문서 (Swagger UI) | https://fairway-gms.duckdns.org/swagger-ui.html |

## 테스트 계정

| 구분 | 이메일 | 비밀번호 |
|---|---|---|
| 관리자 | admin1234@fwgms.com | Admin123! |
| 매니저 | manager1@fwgms.com | test1234! |
| 캐디 | caddy1@fwgms.com | test1234! |

> 관리자 계정은 최초 기동 시 환경변수 기반으로 자동 생성되는 부트스트랩 계정입니다.

---

## 기술 스택

| 분류 | 기술 |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.1.0 (WebMVC) |
| Security | Spring Security + JWT (jjwt 0.12.6, HttpOnly Cookie), OAuth2 Client (카카오) |
| ORM | Spring Data JPA + Hibernate |
| DB | MySQL (AWS RDS 8.4) |
| Push | Firebase Admin SDK 9.4.3 (FCM) |
| Docs | springdoc-openapi 2.8.9 (Swagger UI) |
| File | Apache POI 5.3.0 (엑셀 업로드/다운로드) |
| Infra | AWS EC2 · RDS · ECR, Docker, nginx + Let's Encrypt(HTTPS), DuckDNS |
| CI/CD | GitHub Actions (main push → 빌드 → ECR → EC2 배포 → 헬스체크) |
| Build | Gradle 9 |

---

## 인프라 & CI/CD (이 프로젝트의 핵심 목표)

```
git push (main)
   │
   ▼
GitHub Actions ─ Gradle 빌드(멀티스테이지 Dockerfile) ─ linux/amd64 이미지
   │                                                    (latest + 커밋 sha 태그 → 롤백 대비)
   ▼
AWS ECR ──▶ EC2 (SSH) : 새 이미지 pull → 컨테이너 교체 → /actuator/health 폴링(최대 180초)
                │
                ├─ nginx (HTTPS, Let's Encrypt + DuckDNS)
                └─ AWS RDS MySQL 8.4
프론트엔드: Vercel — main push 시 자동 배포
```

- **비밀값 무노출 구조**: 운영 환경변수는 EC2의 `.env.production` 파일로 주입(`--env-file`) — GitHub 워크플로우 로그·이미지에 비밀값이 남지 않음
- **헬스체크 기반 배포 검증**: 고정 대기 대신 최대 180초 폴링으로 기동 완료를 확인, 실패 시 컨테이너 로그를 워크플로우에 출력
- **RDS 메이저 업그레이드 경험**: MySQL 8.0 → 8.4 (지원 종료 대응)
- 리버스 프록시 뒤 OAuth2 redirect_uri 문제를 `forward-headers-strategy`로 해결하는 등 배포 과정의 트러블슈팅을 문서로 축적

---

## 아키텍처

**Modular Monolith** — 단일 Spring Boot 애플리케이션, 단일 MySQL DB

MSA 전환 가능성을 고려해 도메인 경계를 명확히 분리했으나, 1인 개발과 운영 비용을 고려해 단일 배포 단위로 유지합니다.
(도메인 간 협력은 Application Service / Facade / `@TransactionalEventListener` 내부 이벤트로만 처리 — 자기 앱 HTTP 호출 금지)

```
com.fairwaygms.fairwaygmsbe
├── common        # 공통 응답/예외/보안/골프장 컨텍스트
├── auth          # 인증(이메일+카카오), 가입 승인 흐름
├── golfcourse    # 골프장·코스·카트
├── caddie        # 캐디, 그룹, 순번, 근무 상태
├── operation     # 운영 설정, 티타임, 예약팀(엑셀 업로드)
├── assignment    # 자동/수동 배정, 배정표, 카트 배정
├── settlement    # 캐디피 정책, 월 정산(스냅샷), 엑셀
├── notification  # FCM 알림, 알림 설정
└── board         # 게시글, 댓글, 순번 교환 요청
```

각 도메인은 `application / domain / exception / infrastructure` 4계층 구조를 따릅니다.

---

## 주요 기능

### 캐디 배정 (핵심 도메인)
- **그룹 기반 자동배정**: 캐디 그룹(하우스/우선배정/세션고정)별 **순번 이월(CARRY_OVER) 알고리즘** — 오늘 마지막 배정 캐디 다음 순번부터 내일 시작
- 부(部) 단위 실행, 지정 캐디 우선(잠금), 투근무 자동 처리, 하프백
- 동시 실행 방지: 골프장+날짜 단위 **비관적 락**
- 배정표 DRAFT → CONFIRMED → COMPLETED 상태 전이, 전 변경에 감사 이력

### 이벤트 기반 후속 처리
`@TransactionalEventListener(AFTER_COMMIT)` + `REQUIRES_NEW` 조합으로 본 트랜잭션과 격리:

| 이벤트 | 후속 처리 |
|---|---|
| 배정 확정 | 캐디 FCM 알림, 시간표 공지 게시글 자동 생성 |
| 배정 완료 | **정산 스냅샷** 생성, 카트 AVAILABLE 자동 복원 |

### 정산 (settlement)
- 배정 완료 시점의 캐디피를 **스냅샷으로 고정** → 이후 정책이 바뀌어도 과거 정산을 그대로 재현 (감사 목적)
- 월 마감 확정/취소, 수동 조정 이력, 정산·국세청 과세자료 엑셀 다운로드

### 인증 (auth)
- 이메일 회원가입 + **카카오 소셜 로그인** (최초 가입은 단기 HttpOnly 임시 토큰으로 역할/골프장 선택 연결)
- 가입 승인 흐름: 캐디 → 매니저 승인 / 매니저 → 관리자 승인
- Refresh Token Rotation, 탈퇴 시 이메일 익명화(소프트 삭제 + UNIQUE 공존)

### 그 외
- 예약팀 **엑셀 일괄 업로드** (미리보기 검증 → 확정), 배정표 엑셀 다운로드
- FCM은 인터페이스 분리(`Mock`/`Real`)로 local·test 환경에서 외부 의존 제거

---

## 인증 방식

JWT를 **HttpOnly Cookie**로만 관리합니다. 프론트엔드는 토큰을 읽거나 저장하지 않습니다.

```
AccessToken  → 쿠키명: at  (경로: /)
RefreshToken → 쿠키명: rt  (경로: /api/auth — 재발급 + 로그아웃 시 DB 토큰 폐기용)
```

## 역할 구조

| 역할 | 설명 |
|---|---|
| `ADMIN` | 플랫폼 관리자. `X-Selected-Golf-Course-Id` 헤더로 골프장을 선택해 **전 관리 기능에 접근** |
| `MANAGER` | 소속 골프장 운영 관리 (JWT claim 기준) |
| `CADDY` | 캐디 모바일 PWA 사용자 — 관리 API 접근 차단 |

RequestBody/QueryParam의 골프장 ID는 권한 판단에 사용하지 않습니다 (컨텍스트 리졸버가 역할별로 결정).

---

## API 문서

**Swagger UI가 단일 소스**입니다 — 코드에서 런타임 생성되므로 항상 최신입니다.

- 26개 컨트롤러, 도메인별 Tag 그룹핑
- HttpOnly Cookie 인증 문서화 — 로그인 API 실행 후 바로 보호 API를 Try-out 가능
- `@AdminScopeApi` 커스텀 애노테이션: ADMIN 골프장 범위 API에만 `X-Selected-Golf-Course-Id` 헤더 파라미터 자동 표시

---

## 로컬 실행

### 사전 준비

- Java 21, MySQL (스키마 `fairway_gms` 수동 생성)
- 프로젝트 루트에 `.env` 생성 (`.env.example`의 키 목록 참고)

```properties
DB_URL=jdbc:mysql://localhost:3306/fairway_gms
DB_USERNAME=root
DB_PASSWORD=your_password
JWT_SECRET=your-256bit-secret
FAIRWAY_GMS_BOOTSTRAP_ADMIN_ENABLED=true
FAIRWAY_GMS_BOOTSTRAP_ADMIN_EMAIL=admin@fairway.com
FAIRWAY_GMS_BOOTSTRAP_ADMIN_PASSWORD=Admin1234!
FAIRWAY_GMS_FRONTEND_URL=http://localhost:5173
# 전체 키 목록은 .env.example 참고 (카카오/메일/Firebase는 미설정 시 해당 기능만 비활성)
```

### 실행

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

Swagger UI: `http://localhost:8080/swagger-ui.html`

### 테스트

```bash
./gradlew test          # 단위 + 통합 260건 (통합 테스트는 로컬 MySQL 필요, @Transactional 자동 롤백)
./gradlew clean build
```

---

## 프로젝트 상태 & 로드맵

이 프로젝트는 **"완성 후 배포"가 아니라 "배포 파이프라인 위에서 성장"**하는 방식으로 만들고 있습니다.
핵심 도메인(배정·정산·알림)과 배포/CI/CD는 완료됐고, 아래를 지속 업데이트할 예정입니다.

- [ ] 토큰 정리 스케줄러 (만료 refresh/reset 토큰 배치 삭제)
- [ ] 프로필(이름/연락처) 수정 API, 매니저 대상 알림
- [ ] 캐디 본인 월 수입 조회 (모바일)
- [ ] 관리자 화면 UI/UX 고도화 (공용 테이블 컴포넌트 확대 적용)

설계 결정은 ADR(11건), 장애·삽질 기록은 트러블슈팅 문서(9건)로 함께 관리하고 있습니다.

## 관련 저장소

| 구분 | 저장소 |
|---|---|
| Frontend (Vue 3) | https://github.com/soyoung-v/fairway-gms-fe |
