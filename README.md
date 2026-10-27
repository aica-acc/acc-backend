# acc-backend

ACC(축제 홍보물 자동생성 서비스)의 **Spring Boot 백엔드**입니다.
모든 데이터가 오가는 **중심**입니다. 프론트(`acc-front`)는 백엔드만 호출하고, AI 서버(`acc-ai`)와 MySQL 은 백엔드만 접근합니다.

```
브라우저 ──▶ acc-front(nginx) ──/api/, /static/──▶ acc-backend ──▶ MySQL
                                                       └──────▶ acc-ai (FastAPI, 외부 비노출)
```

- 인증(JWT)·회원·권한 관리
- 프로젝트 / 기획서 / 홍보물 메타데이터를 DB 에 저장하고 조회
- AI 서버 호출 (기획서 분석, 트렌드 분석, 프롬프트·이미지 생성, 리포트 등)
- AI 가 만든 파일을 프론트가 볼 수 있는 위치로 복사·관리
- AI 서버 정적 파일(`/static/**`) 중계 (`AiStaticProxyController`) — 브라우저가 AI 서버에 직접 붙지 않도록
- DB 스키마 마이그레이션 (Flyway)

## 기술 스택

| 구분 | 내용 |
|---|---|
| 언어 / 프레임워크 | Java 17, Spring Boot 3.4.10 |
| 데이터 접근 | MyBatis 3.0.5 (매퍼 XML), 일부 `JdbcTemplate` |
| 보안 | Spring Security + JWT (jjwt 0.12.6), BCrypt |
| DB | MySQL 8 (Docker 에서는 8.4.7) |
| 마이그레이션 | Flyway 10 (`flyway-core`, `flyway-mysql`) |
| 빌드 | Maven (`mvnw` 포함) |

## 빠른 실행 (로컬)

사전 준비: JDK 17, MySQL 8, `acc-ai`(선택, AI 기능을 쓸 때).

1. **DB 만들기** (빈 데이터베이스만 있으면 됩니다. 테이블은 기동 시 자동 생성)
   ```sql
   CREATE DATABASE acc_database CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   CREATE USER 'acc_admin_id'@'%' IDENTIFIED BY '비밀번호';
   GRANT ALL ON acc_database.* TO 'acc_admin_id'@'%';
   ```
2. **설정 파일 만들기**: `src/main/resources/application.properties.example` 을 같은 폴더의
   `application.properties` 로 복사한 뒤 값을 채웁니다. (`application.properties` 는 `.gitignore` 대상이라 커밋되지 않습니다)
   최소한 `spring.datasource.*`, `auth.admin.password`, `auth.jwt.secret` 을 채우세요.
3. **실행**
   ```bash
   ./mvnw spring-boot:run          # Windows: mvnw.cmd spring-boot:run
   ```
   기본 포트는 `8081` 입니다. 기동하면 Flyway 가 스키마를 만들고, `test` / `admin` 계정이 자동 생성됩니다.

### jar 로 빌드

```bash
./mvnw -DskipTests package
java -jar target/acc-0.0.1-SNAPSHOT.jar
```

## 설정

모든 설정은 `application.properties` 로도, **환경변수**로도 줄 수 있습니다.
환경변수 이름은 속성 이름을 대문자로 바꾸고 `.` 와 `-` 를 `_` 로 바꾼 것입니다.
(예: `spring.datasource.url` → `SPRING_DATASOURCE_URL`, `react.base-path` → `REACT_BASE_PATH`)

| 속성 | 설명 | 예시 / 기본값 |
|---|---|---|
| `server.port` | 서버 포트 | `8081` |
| `spring.datasource.url` / `username` / `password` | MySQL 접속 정보 | `jdbc:mysql://localhost:3306/acc_database?...` |
| `python.api.url` | AI 서버 주소 (API 호출과 `/static/**` 중계에 사용) | `http://localhost:5000` |
| `accai.base-url` | 배너 트렌드용 AI 서버 주소 | `http://localhost:5000` |
| `react.base-path` | 프론트 레포 루트. 하위 `public/data/promotion/...` 에 결과물을 복사 | `../acc-front` |
| `python.base-dir` | AI 서버가 홍보물을 생성하는 폴더 | `../acc-ai/promotion` |
| `files.generated-root` | 생성물 루트 | `./data/promotion/` |
| `files.path` | 기획서 업로드 저장 폴더 | `./data/proposal` |
| `files.poster-images-dir` | `/poster-images/**` 로 서빙할 폴더 | `./홍보물` |
| `cors.allowed-origin-patterns` | CORS 허용 Origin (쉼표 구분) | `http://localhost:*,http://127.0.0.1:*` |
| `auth.jwt.secret` | JWT 서명 키 (32바이트 이상). 비우면 기동마다 랜덤 키 → 재시작 시 로그인 풀림 | |
| `auth.jwt.expire-hours` | 토큰 유효 시간 | `12` |
| `auth.test-login.enabled` | "test 계정으로 로그인" 허용 여부. **외부 공개 서버는 `false` 권장** | `true` |
| `auth.test.email` | test 계정 이메일 | `test@acc.local` |
| `auth.admin.email` / `auth.admin.password` | 관리자 계정. 비밀번호(8자 이상)를 주면 최초 기동 시 생성 | `admin@acc.local` / (없음) |
| `test.samples-dir` | test 계정 자동 입력에 쓰는 샘플 기획서 폴더 (`samples.json` + 파일) | `./data/samples/proposals` |
| `spring.flyway.enabled` | 마이그레이션 사용 여부 | `true` |

> 경로 속성은 절대경로와 실행 위치 기준 상대경로 모두 됩니다.

## 인증 / 권한

- 로그인하면 JWT 를 발급하고, 클라이언트는 `Authorization: Bearer <token>` 으로 보냅니다.
- 토큰이 유효해도 **매 요청마다 DB 에서 회원 상태·권한을 다시 확인**합니다. 관리자가 권한을 바꾸거나 계정을 정지하면 즉시 적용됩니다.
- 필터가 로그인한 회원번호를 `request.getAttribute("m_no")` 에 넣습니다. 서비스 계층에서는 `CurrentMember.no()` 로 읽습니다.
- 공개 경로: `/api/auth/config`, `/api/auth/signup`, `/api/auth/login`, `/api/auth/test-login`, `/poster-images/**`, `/static/**`(이미지를 `<img>` 로 불러오므로). 나머지는 모두 로그인이 필요합니다.

| 권한 | 설명 |
|---|---|
| `USER` | 일반 회원 (회원가입으로 생성) |
| `TEST` | 공용 테스트 계정(`M000001`). 샘플 기획서 자동 입력을 쓸 수 있고, 정보·비밀번호는 변경할 수 없음 |
| `ADMIN` | 관리자. `/api/admin/**` 사용 가능 |

회원번호는 `M000001` 형식(M + 6자리)입니다. 기동 시 `DataInitializer` 가 `test` 계정을 항상 보장하고, `auth.admin.password` 가 있으면 관리자 계정도 만듭니다.

## API 요약

| 경로 | 설명 |
|---|---|
| `POST /api/auth/signup`, `/login`, `/test-login` | 가입 / 로그인 / test 로그인 |
| `GET /api/auth/me`, `PATCH /api/auth/me`, `POST /api/auth/password` | 내 정보 조회 / 수정 / 비밀번호 변경 |
| `GET /api/admin/members` 외 | 회원 목록, 권한 변경, 정지, 회원 데이터 초기화 (ADMIN) |
| `GET /api/test/samples`, `/samples/{id}/file` | 샘플 기획서 목록·파일 (TEST, ADMIN) |
| `POST /api/project/analyze/proposal`, `/analyze/total_trend`, `/analyze/region_trend` | 기획서 / 키워드 트렌드 / 지역 트렌드 분석 |
| `POST /api/generate-prompt`, `/api/create-image`, `/api/posters/{id}/regenerate` | 프롬프트·이미지 생성, 재생성 |
| `POST /api/editor/build`, `/ai-render`, `/save-image` | 에디터 템플릿 생성·AI 색상 추천·저장 |
| `POST /api/liveposter/create` | 라이브 포스터 생성 |
| `POST /api/report/article`, `/notice`, `/sns`, `/package` | 홍보 리포트 생성 |
| `GET /api/mypage/projects`, `/projects/{id}`, `/assets` | 내 프로젝트 / 상세 / 저장소 |

## DB 마이그레이션 (Flyway)

스키마는 `src/main/resources/db/migration/` 의 SQL 로 관리합니다. **백엔드가 기동할 때 자동으로 적용**됩니다.

| 파일 | 내용 |
|---|---|
| `V1__init_schema.sql` | 기본 테이블 (팀 공유 DDL 기준) |
| `V2__schema_patch.sql` | 코드와 어긋나던 부분 보정 (`user_input.create_at`, `poster_archive`) |
| `V3__auth.sql` | `member.password_hash`, `member.role` |

- **새 DB**: V1 → V3 순서로 적용됩니다.
- **이미 테이블이 있는 기존 DB(이력 없음)**: 자동으로 baseline 후 V1 부터 적용합니다. V1~V3 은 여러 번 실행해도 안전하게 작성되어 있습니다.
- **앱을 띄우지 않고 적용만 하려면**
  ```bash
  ./mvnw flyway:migrate \
    -Dflyway.url="jdbc:mysql://localhost:3306/acc_database?serverTimezone=UTC&characterEncoding=UTF-8" \
    -Dflyway.user=acc_admin_id -Dflyway.password=비밀번호
  ```
- 끄려면 `SPRING_FLYWAY_ENABLED=false`.
- **스키마를 바꿀 때는 기존 파일을 고치지 말고 `V4__설명.sql` 처럼 새 파일을 추가하세요.** 이미 적용된 파일이 바뀌면 Flyway 가 기동을 거부합니다.

## Docker

`Dockerfile` 은 멀티 스테이지(Maven 빌드 → JRE 17 실행)입니다. `application.properties` 는 이미지에 들어가지 않으므로 **모든 설정은 환경변수로 주입**합니다.

```bash
docker build -t scv3114/acc-backend .
docker run -p 8081:8081 \
  -e SPRING_DATASOURCE_URL="jdbc:mysql://db:3306/acc_database?serverTimezone=UTC&characterEncoding=UTF-8" \
  -e SPRING_DATASOURCE_USERNAME=acc_admin_id -e SPRING_DATASOURCE_PASSWORD=... \
  -e SPRING_DATASOURCE_DRIVER_CLASS_NAME=com.mysql.cj.jdbc.Driver \
  -e MYBATIS_MAPPER_LOCATIONS='classpath:/mappers/**/*.xml' -e MYBATIS_TYPE_ALIASES_PACKAGE=com.assistant.acc \
  -e PYTHON_API_URL=http://ai:5000 -e ACCAI_BASE_URL=http://ai:5000 \
  -e REACT_BASE_PATH=/data/acc-front -e PYTHON_BASE_DIR=/data/acc-ai/promotion \
  -e FILES_GENERATED_ROOT=/data/acc/data/promotion/ -e FILES_PATH=/data/acc/data/proposal \
  -e AUTH_JWT_SECRET=... -e AUTH_ADMIN_PASSWORD=... \
  scv3114/acc-backend
```

전체 서비스(DB + AI + 백엔드 + 프론트)를 한 번에 띄우는 `docker-compose.yml` 과 환경변수 정리는 배포용 폴더의 README 를 참고하세요.
AI 서버 주소는 `PYTHON_API_URL`, `ACCAI_BASE_URL` 두 값으로 모두 바꿀 수 있습니다. 컨테이너를 분리해 띄울 때는 둘 다 AI 서비스 주소(예: `http://ai:5000`)로 지정하세요.

## 폴더 구조

```
src/main/java/com/assistant/acc
├─ config/        WebConfig(보안·CORS), FlywayConfig, DataInitializer, RestTemplateConfig
├─ security/      JwtTokenProvider, JwtAuthFilter, CurrentMember
├─ controller/    auth, admin, test, project, poster, editor, mypage, report, liveposter, file, image ...
├─ service/       비즈니스 로직 (프로젝트·포스터·에디터·파일·리포트 등)
├─ mapper/        MyBatis 매퍼 인터페이스
├─ domain/, dto/  엔티티 / 요청·응답 객체
└─ utility/       파일 저장 등
src/main/resources
├─ mappers/       MyBatis 매퍼 XML
└─ db/migration/  Flyway 마이그레이션 (V1~V3)
```

## 알아두면 좋은 점

- 다른 컨트롤러가 `m_no` 를 요청 본문으로 받는 곳이 있지만, **회원 판단은 토큰의 회원번호 기준**으로 해야 합니다. 새 API 를 만들 때는 `CurrentMember.no()` 를 사용하세요.
- `p_no` 로 직접 조회하는 일부 API(포스터 목록 등)는 아직 소유자 검사가 없습니다. 마이페이지 상세에만 적용되어 있습니다.
- 매퍼 SQL 의 테이블 별칭은 대소문자를 구분해서 쓰세요. 윈도우 MySQL 과 달리 Linux MySQL 은 `P.col` 과 `p.col` 을 다르게 봅니다.
