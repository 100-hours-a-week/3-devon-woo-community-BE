# 테스트 컨벤션

> 목적: 우리 프로젝트 테스트를 **어느 레이어에서 무엇을 어떻게 테스트할지** 한눈에 보이도록 정리한 컨벤션입니다.  
> 상세 내용은 각 레이어별 문서를 참고합니다.  
> - 도메인: `DOMAIN_TEST_CONVENTION.md`  
> - 리포지토리: `REPOSITORY_TEST_CONVENTION.md`  
> - 서비스: `SERVICE_TEST_CONVENTION.md`  
> - 컨트롤러(WebMvc): `CONTROLLER_WEBMVC_TEST_CONVENTION.md`  
> - 통합: `INTEGRATION_TEST_CONVENTION.md`  
> - 픽스처: `test-fixture.md`, `TEST_FIXTURES_GUIDE.md`

---

## 1. 테스트 레이어 개요

- **도메인 테스트 (Entity / Policy)**  
  - 대상: 엔티티 도메인 메서드, 정책/불변식, 값 객체.  
  - 환경: 순수 JUnit, 스프링/DB 의존 없음.  
  - 역할: 생성/상태 전이/불변식/권한 정책 등 **핵심 비즈니스 규칙** 검증.  
  - 참고: `DOMAIN_TEST_CONVENTION.md`

- **Repository 테스트 (JPA)**  
  - 대상: JPA 매핑, 커스텀 쿼리, 카운터/삭제 쿼리, Cascade.  
  - 환경: `@RepositoryJpaTest` + 실제 테스트 DB.  
  - 역할: 엔티티 매핑과 쿼리가 **실제 DB 위에서 설계대로 동작하는지** 검증.  
  - 참고: `REPOSITORY_TEST_CONVENTION.md`

- **서비스 단위 테스트**  
  - 대상: 서비스 레이어의 분기/예외/오케스트레이션.  
  - 환경: `@UnitTest` (Mockito 기반, 스프링 컨텍스트 없음).  
  - 역할: 도메인/Repository/정책/외부 클라이언트를 **어떻게 조합하는지** 검증.  
  - 참고: `SERVICE_TEST_CONVENTION.md`

- **Controller WebMvc 테스트**  
  - 대상: HTTP 요청 매핑, 바인딩, Validation, 예외 → 응답 매핑.  
  - 환경: `@ControllerWebMvcTest` (WebMvc 슬라이스, MockMvc).  
  - 역할: 각 엔드포인트의 **HTTP 계약(Request/Response)** 검증 및 문서화.  
  - 참고: `CONTROLLER_WEBMVC_TEST_CONVENTION.md`

- **통합 테스트 (Integration)**  
  - 대상: Controller → Service → Repository → DB (+ Security/필터/설정).  
  - 환경: `@IntegrationTest` / `@IntegrationSecurityTest` / `@ServiceIntegrationTest` / `@JobIntegrationTest`.  
  - 역할: 실제 애플리케이션 환경에서 **대표 플로우와 인프라 협력** 검증.  
  - 참고: `INTEGRATION_TEST_CONVENTION.md`

---

## 2. 레이어 선택 규칙

테스트를 새로 작성할 때는 다음 순서로 레이어를 결정합니다.

1. **규칙/계산/상태 전이인가?**  
   - 예: 제목 길이, 소유권, 좋아요 가능 여부, 카운터 증감, soft delete 등  
   → 도메인 테스트 (Entity / Policy)
2. **쿼리/매핑/트랜잭션 동작인가?**  
   - 예: `findBy*`, `deleteBy*`, Cascade, 카운터 쿼리 등  
   → Repository 테스트
3. **여러 도메인/Repository/외부 시스템을 조합하는 플로우인가?**  
   - 예: 게시글 생성 시 게시글 저장 + 태그 저장 + 카운터 증가 + 알림 발송 등  
   → 서비스 단위 테스트
4. **HTTP 계약(요청/응답/Validation/에러 코드)인가?**  
   - 예: DTO 바인딩, 400/404/403 매핑, JSON 구조  
   → Controller WebMvc 테스트
5. **전체 스택이 함께 동작하는지 확인해야 하는 대표 플로우인가?**  
   - 예: 게시글/댓글 CRUD, 회원 탈퇴, 인증/인가 플로우, 배치 잡  
   → 통합 테스트

하나의 기능에 대해 **모든 레이어에 테스트를 만들 필요는 없고**,  
핵심 규칙은 도메인/서비스, 대표 플로우는 통합, HTTP 계약은 WebMvc로 나누어 커버합니다.

---

## 3. 공통 컨벤션

- **네이밍**
  - 도메인: `PostTest`, `CommentTest`, `OwnershipPolicyTest` 등  
  - Repository: `PostRepositoryTest`, `CommentRepositoryTest` 등  
  - 서비스: `PostServiceTest`, `CommentServiceTest` 등  
  - WebMvc: `PostControllerTest`, `AuthControllerTest` 등  
  - 통합: `PostIntegrationTest`, `AuthIntegrationTest` 등
- **Given–When–Then 구조**
  - Given: Fixture/헬퍼로 준비 코드 최소화.  
  - When: 테스트 대상 메서드/엔드포인트 한 개만 호출.  
  - Then: 결과/상태/예외만 검증 (내부 구현 검증 금지).
- **테스트 더블**
  - 우선 순위: 실제 객체 → Fake/Stub → Mock 순.  
  - Mock은 주로 서비스 단위 테스트에서 외부 시스템/Repository 상호작용 검증에 한정.  
  - 상세 규칙: `TEST_DOUBLE_CONVENTION.md`, `TEST_DOUBLES_GUIDE.md` 참고.
- **프로필/태그**
  - Repository: `@RepositoryJpaTest` (`@Tag("repository")`)  
  - 서비스 유닛: `@UnitTest` (`@Tag("unit")`)  
  - 통합: `@IntegrationTest` / `@IntegrationSecurityTest` / `@ServiceIntegrationTest` / `@JobIntegrationTest`  
  - 공통: `test` 프로필 사용 (`application-test.yml`)

---

## 4. Test Fixture 컨벤션 요약

자세한 내용은 `test-fixture.md`, `TEST_FIXTURES_GUIDE.md`를 따릅니다.

- 클래스
  - 네이밍: **대상 + Fixture** (`PostFixture`, `PostRequestFixture` 등).  
  - `final` + `private` 기본 생성자, `static` 메서드/상수만 사용.
- 상수
  - 기본값: `DEFAULT_` 접두어 (`DEFAULT_EMAIL`, `DEFAULT_TITLE` 등).  
  - 대표 변경값: `UPDATED_` 접두어.
- 메서드
  - 엔티티: `create(...)`, 필요 시 `createWithId(...)` (도메인 테스트에서는 되도록 `create` 사용).  
  - 요청 DTO: `createRequest()`, `updateRequest()`, 실패/경계값은 이름으로 명시 (`createRequestWithoutTitle()` 등).  
  - 응답/조회 DTO: `create()`, `createWithAllFields(...)`.
- 레이어별 사용
  - 도메인: SUT는 팩토리/생성자 직접 호출, 협력 엔티티는 Fixture 사용.  
  - Repository: 저장 전 엔티티는 Fixture로 생성.  
  - 서비스/WebMvc/통합: Request/도메인 Fixture 적극 사용, Response는 핵심 필드를 직접 검증.

---

## 5. 테스트 작성 체크리스트

새 테스트를 추가할 때는 다음을 확인합니다.

1. 이 규칙/플로우는 어느 레이어에서 검증하는 것이 적절한가?  
   - 도메인 / Repository / 서비스 / WebMvc / 통합 중 하나 선택.
2. 이미 존재하는 컨벤션 문서와 충돌하지 않는가?  
   - 해당 레이어의 `*_TEST_CONVENTION.md`를 한 번 확인.
3. Fixture/테스트 더블 사용이 과하지 않은가?  
   - Given은 간결하게, Then은 결과 위주로.  
   - 비즈니스 로직은 Fixture나 Mock 내부가 아니라 **테스트 대상 코드**에서 검증.
4. 테스트 이름과 내용이 “무슨 시나리오를 검증하는지”를 분명히 드러내는가?

위 규칙을 기본으로, 세부 구현은 각 레이어별 컨벤션 문서를 따라 작성합니다.

