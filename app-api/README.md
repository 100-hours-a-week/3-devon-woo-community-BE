# T-Log API Module (`app-api`)

T-Log의 **메인 REST API (Spring MVC) 애플리케이션**입니다.  
게시글, 댓글, 회원, 좋아요, 조회수, 파일 업로드 등 기술 블로그 핵심 도메인을 처리하며, `/api/**` 경로로 Nginx 뒤에서 WebFlux 모듈(`app-webflux`)과 함께 동작합니다.

---

## 1. 모듈 역할

- **기술 블로그 도메인 API**
  - 게시글 CRUD, 댓글, 좋아요, 조회수, 회원/인증, 파일 업로드·이미지 관리
- **클린 아키텍처 3-레이어 구조**
  - `application / domain / infra` + `common` 모듈을 기준으로 책임 분리
- **전통적인 MVC + JPA 기반 서버**
  - Spring MVC + Spring Data JPA + QueryDSL
  - Spring Security + JWT + OAuth2 기반 인증/인가
  - Redis, Cloudinary 등의 인프라 연동

전체적인 아키텍처와 레이어 구조는 다음 문서를 참고할 수 있습니다.
- [CLEAN_ARCHITECTURE.md](../docs/architecture/CLEAN_ARCHITECTURE.md)
- [ENTITY_CONVENTION.md](../docs/architecture/ENTITY_CONVENTION.md)

---

## 2. 디렉터리 / 패키지 구조

### 2-1. 디렉터리 구조 (요약)

```text
app-api/
 ├── build.gradle
 ├── Dockerfile
 └── src
     ├── main
     │   ├── java/com/devon/techblog
     │   │   ├── application   # 유스케이스 / 컨트롤러 / 서비스 / 시큐리티
     │   │   ├── domain        # 엔티티 / 리포지토리 / 정책 / QueryDSL 설정
     │   │   ├── infra         # Redis, 이미지 스토리지 등 외부 연동
     │   │   └── common        # 예외, 공통 DTO, AOP, Swagger, validation 등
     │   └── resources
     │       └── application.yml
     └── test
         └── java/com/devon/techblog
             └── ... 테스트 코드 (계층별 컨벤션 문서 참조)
```

### 2-2. 패키지 구조 (레이어 기준)

`com.devon.techblog` 하위에 레이어/도메인별 패키지를 구성합니다.

- `common`
  - `exception`, `dto`, `validation`, `aop`, `swagger`, `utils`
  - 공통 예외/응답 포맷, 검증 상수, 로깅/성능 AOP, Swagger 설정
- `domain`
  - 도메인 엔티티, 리포지토리 인터페이스, QueryDSL 설정, 도메인 정책 등
  - `post`, `comment`, `member`, `file`, `common`, `config`
- `infra`
  - 외부 시스템 연동 및 기술 구현
  - `redis`: 조회수/캐시 전략 구현  
  - `image`: Cloudinary 기반 이미지 업로드 등
- `application`
  - 사용자의 유스케이스를 구현하는 계층
  - `post`, `comment`, `member`, `file`, `media`, `security`, `common`
  - 컨트롤러, 서비스, 요청/응답 DTO, Validator 등

도메인/엔티티 설계 컨벤션은 아래 문서를 참고합니다.
- [ENTITY_CONVENTION.md](../docs/architecture/ENTITY_CONVENTION.md)
- [VALIDATION_LAYER_CONVENTION.md](../docs/validation/VALIDATION_LAYER_CONVENTION.md)
- [VALIDATOR_VS_POLICY_PATTERN.md](../docs/validation/VALIDATOR_VS_POLICY_PATTERN.md)

---

## 3. 주요 의존성

`app-api/build.gradle` 기준 핵심 의존성입니다.

- Spring Boot
  - `spring-boot-starter-web`
  - `spring-boot-starter-validation`
  - `spring-boot-starter-data-jpa`
  - `spring-boot-starter-security`
  - `spring-boot-starter-oauth2-client`
- Database & Query
  - MySQL (`mysql-connector-j`)
  - H2 (테스트용)
  - QueryDSL JPA (`com.querydsl:querydsl-jpa`)
- Infra
  - Redis (`spring-boot-starter-data-redis`)
  - Cloudinary (`com.cloudinary:cloudinary-http44`) – 이미지 스토리지
- Auth
  - JJWT (`io.jsonwebtoken:*`) – JWT 발급/검증
  - Spring Security / OAuth2 Client
- API & 품질
  - springdoc-openapi (`springdoc-openapi-starter-webmvc-ui`)
  - AOP (`spring-boot-starter-aop`)
  - JaCoCo (커버리지 리포트)
- Test
  - `spring-boot-starter-test`, `spring-security-test`
  - Testcontainers (MySQL, Redis)
  - `java-test-fixtures` (testFixtures 기반 공용 테스트 유틸)

자세한 의존성 및 설정은 [app-api/build.gradle](./build.gradle) 을 참고하세요.

---

## 4. 레이어별 컨벤션

### 4-1. Application Layer (`application`)

- **Controller**
  - `@RestController` + `/api/v1/**` 패턴으로 엔드포인트 구성
  - 요청 DTO: `request` 패키지, 응답 DTO: `response` 패키지로 분리
  - 검증: `@Valid` + Bean Validation, 추가 비즈니스 검증은 `validator` 또는 Policy로 위임
- **Service**
  - 트랜잭션 경계 설정 (`@Transactional`)
  - 도메인 모델/리포지토리/외부 클라이언트를 조합해 **유스케이스 단위**로 로직 구성
- **Security (application.security)**
  - JWT 필터, 예외 핸들러, 인증/인가 설정
  - OAuth2 로그인 후 JWT 발급/연동
  - 현재 사용자 주입, 권한 체크 등

관련 문서:
- Spring Security 구성: [SPRING_SECURITY_SETUP.md](../docs/security/SPRING_SECURITY_SETUP.md)
- 현재 사용자 주입: [CURRENT_USER_IMPLEMENTATION.md](../docs/security/CURRENT_USER_IMPLEMENTATION.md), [CURRENT_USER_GUIDE.md](../docs/security/CURRENT_USER_GUIDE.md)
- 예외/보안 응답 컨벤션: [SPRING_SECURITY_EXCEPTION_CONVENTION.md](../docs/security/SPRING_SECURITY_EXCEPTION_CONVENTION.md)

### 4-2. Domain Layer (`domain`)

- 엔티티는 **도메인 용어 중심**으로 설계하고, JPA 매핑·비즈니스 로직을 함께 포함합니다.
- 도메인 정책(조회수, 좋아요 등)은 별도의 Policy/도메인 서비스로 분리합니다.
- QueryDSL을 이용한 복합 조회, 동적 검색, 정렬은 `repository.impl` + 공통 유틸을 사용합니다.

관련 문서:
- 엔티티/도메인 설계: [ENTITY_CONVENTION.md](../docs/architecture/ENTITY_CONVENTION.md)
- 조회수/좋아요 정책 ADR:
  - [ADR_POST_VIEW_COUNT_STRATEGY.md](../docs/adr/ADR_POST_VIEW_COUNT_STRATEGY.md)
  - [ADR_POST_VIEW_COUNT_POLICY.md](../docs/adr/ADR_POST_VIEW_COUNT_POLICY.md)
  - [ADR_POSTLIKE_CONCURRENCY.md](../docs/adr/ADR_POSTLIKE_CONCURRENCY.md)
  - [ADR_POSTLIKE_COMPOSITE_KEY.md](../docs/adr/ADR_POSTLIKE_COMPOSITE_KEY.md)
- QueryDSL 설계/사용:
  - [QUERYDSL_USAGE.md](../docs/querydsl/QUERYDSL_USAGE.md)
  - [QUERYDSL_ADVANCED_USAGE.md](../docs/querydsl/QUERYDSL_ADVANCED_USAGE.md)
  - [QUERYDSL_DECISIONS.md](../docs/querydsl/QUERYDSL_DECISIONS.md)

### 4-3. Infra Layer (`infra`)

- Redis
  - 조회수·캐시 전략 구현, DB 반영 타이밍/정합성을 고려한 정책 적용
- 이미지/파일
  - Cloudinary 기반 이미지 업로드, URL 생성, 삭제 등
  - 파일/이미지 도메인과의 경계는 `domain.file` + `infra.image` 조합으로 관리

파일 저장/업로드 구조는 다음 문서를 참고합니다.
- [FILE-ARCHITECTURE.md](../docs/architecture/FILE-ARCHITECTURE.md)
- [IMAGE_UPLOAD.md](../docs/file-upload/IMAGE_UPLOAD.md)
- [FILE-UPLOAD-FLOW.md](../docs/file-upload/FILE-UPLOAD-FLOW.md)
- [FILE-UPLOAD-PRESIGNED-VS-MULTIPART.md](../docs/file-upload/FILE-UPLOAD-PRESIGNED-VS-MULTIPART.md)

### 4-4. Common Module (`common`)

- 예외 (`common.exception`): `ErrorCode`, `CustomException`, 전역 예외 처리
- 응답 DTO (`common.dto`): 공통 `ApiResponse` 등
- 검증 (`common.validation`): Validation 메시지/패턴 상수
- AOP (`common.aop`): 로깅, 성능 측정, 쿼리 로깅 등
- Swagger (`common.swagger`): OpenAPI/Swagger UI 설정

---

## 5. 테스트 전략 (app-api 관점)

이 모듈의 테스트 전략은 프로젝트 전반 테스트 가이드에 따릅니다.

- **단위 테스트**
  - 도메인 엔티티/정책, Validator, 순수 서비스 로직
- **리포지토리 테스트**
  - JPA 매핑, QueryDSL 쿼리, 페이징/정렬, 복합 조건 등
  - H2 또는 Testcontainers 기반 MySQL
- **서비스/통합 테스트**
  - 트랜잭션, 조회수/좋아요 정책, 파일 업로드 플로우 등
- **컨트롤러/보안 테스트**
  - `@WebMvcTest`, `@SpringBootTest` + `MockMvc` 와 `spring-security-test` 를 사용

자세한 테스트 가이드는 아래 문서를 참고합니다.
- 전체 전략: [PROJECT_TEST_STRATEGY.md](../docs/test/PROJECT_TEST_STRATEGY.md)
- 계층별 컨벤션:
  - [DOMAIN_TEST_CONVENTION.md](../docs/test/DOMAIN_TEST_CONVENTION.md)
  - [SERVICE_TEST_CONVENTION.md](../docs/test/SERVICE_TEST_CONVENTION.md)
  - [REPOSITORY_TEST_CONVENTION.md](../docs/test/REPOSITORY_TEST_CONVENTION.md)
  - [CONTROLLER_WEBMVC_TEST_CONVENTION.md](../docs/test/CONTROLLER_WEBMVC_TEST_CONVENTION.md)
  - [INTEGRATION_TEST_CONVENTION.md](../docs/test/INTEGRATION_TEST_CONVENTION.md)
- Fixture/더블:
  - [TEST-FIXTURE-CONVENTION.md](../docs/test/TEST-FIXTURE-CONVENTION.md)
  - [TEST_FIXTURES_GUIDE.md](../docs/test/TEST_FIXTURES_GUIDE.md)
  - [TEST_DOUBLE_CONVENTION.md](../docs/test/TEST_DOUBLE_CONVENTION.md)

---

## 6. 관련 문서 모음

app-api 모듈과 직접적으로 연관된 주요 문서를 한 번 더 모아두었습니다.

- 아키텍처/레이어
  - [CLEAN_ARCHITECTURE.md](../docs/architecture/CLEAN_ARCHITECTURE.md)
  - [ENTITY_CONVENTION.md](../docs/architecture/ENTITY_CONVENTION.md)
- 도메인/정책
  - [ADR_POST_VIEW_COUNT_STRATEGY.md](../docs/adr/ADR_POST_VIEW_COUNT_STRATEGY.md)
  - [ADR_POST_VIEW_COUNT_POLICY.md](../docs/adr/ADR_POST_VIEW_COUNT_POLICY.md)
  - [ADR_POSTLIKE_CONCURRENCY.md](../docs/adr/ADR_POSTLIKE_CONCURRENCY.md)
  - [ADR_POSTLIKE_COMPOSITE_KEY.md](../docs/adr/ADR_POSTLIKE_COMPOSITE_KEY.md)
- QueryDSL
  - [QUERYDSL_USAGE.md](../docs/querydsl/QUERYDSL_USAGE.md)
  - [QUERYDSL_ADVANCED_USAGE.md](../docs/querydsl/QUERYDSL_ADVANCED_USAGE.md)
  - [QUERYDSL_DECISIONS.md](../docs/querydsl/QUERYDSL_DECISIONS.md)
- 파일/이미지 업로드
  - [FILE-ARCHITECTURE.md](../docs/architecture/FILE-ARCHITECTURE.md)
  - [IMAGE_UPLOAD.md](../docs/file-upload/IMAGE_UPLOAD.md)
  - [FILE-UPLOAD-FLOW.md](../docs/file-upload/FILE-UPLOAD-FLOW.md)
  - [FILE-UPLOAD-PRESIGNED-VS-MULTIPART.md](../docs/file-upload/FILE-UPLOAD-PRESIGNED-VS-MULTIPART.md)
- 보안
  - [SPRING_SECURITY_SETUP.md](../docs/security/SPRING_SECURITY_SETUP.md)
  - [CURRENT_USER_IMPLEMENTATION.md](../docs/security/CURRENT_USER_IMPLEMENTATION.md)
  - [CURRENT_USER_GUIDE.md](../docs/security/CURRENT_USER_GUIDE.md)
  - [SPRING_SECURITY_EXCEPTION_CONVENTION.md](../docs/security/SPRING_SECURITY_EXCEPTION_CONVENTION.md)
- 테스트
  - [PROJECT_TEST_STRATEGY.md](../docs/test/PROJECT_TEST_STRATEGY.md)
  - [TEST-FIXTURE-CONVENTION.md](../docs/test/TEST-FIXTURE-CONVENTION.md)
  - [TEST_FIXTURES_GUIDE.md](../docs/test/TEST_FIXTURES_GUIDE.md)
  - [DOMAIN_TEST_CONVENTION.md](../docs/test/DOMAIN_TEST_CONVENTION.md)
  - [SERVICE_TEST_CONVENTION.md](../docs/test/SERVICE_TEST_CONVENTION.md)
  - [REPOSITORY_TEST_CONVENTION.md](../docs/test/REPOSITORY_TEST_CONVENTION.md)
  - [CONTROLLER_WEBMVC_TEST_CONVENTION.md](../docs/test/CONTROLLER_WEBMVC_TEST_CONVENTION.md)
  - [INTEGRATION_TEST_CONVENTION.md](../docs/test/INTEGRATION_TEST_CONVENTION.md)
