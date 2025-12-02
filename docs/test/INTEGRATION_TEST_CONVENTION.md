# 통합 테스트 컨벤션 (IntegrationTest)

> 대상: `@IntegrationTest` / `@IntegrationSecurityTest` 기반 통합 테스트  
> 예: `PostIntegrationTest`, `CommentIntegrationTest`, `MemberIntegrationTest`, `AuthIntegrationTest` 등

이 문서는 우리 프로젝트에서 **통합 테스트가 무엇을 검증하고, 무엇은 검증하지 않을지**에 대한 기준을 정리합니다.  
도메인/서비스/WebMvc 테스트와 역할을 나누어, 통합 테스트가 **전체 플로우와 인프라 협력에 집중하는 레이어**가 되도록 하는 것이 목적입니다.

---

## 0. 통합 테스트용 메타 어노테이션들

### 0.1 @IntegrationTest / @IntegrationSecurityTest (HTTP + MockMvc)

`src/test/java/com/devon/techblog/config/annotation/IntegrationTest.java`:

- `@SpringBootTest` + `@AutoConfigureMockMvc`
  - 전체 스프링 컨텍스트를 로드하고, `MockMvc`를 자동 구성합니다.
  - Controller, Service, Repository, Security 설정, Filter, ExceptionHandler 등이 실제와 유사하게 구동됩니다.
- `@ActiveProfiles("test")`
  - 테스트 프로필(`application-test.yml`)을 활성화합니다.
- `@Import(TestSecurityConfig, JpaAuditingTestConfig, ImageStorageMockConfig)`
  - 테스트용 Security 설정, JPA Auditing 설정, 이미지 스토리지 Mock 설정을 주입합니다.
- `@Tag("integration")`
  - Gradle/JUnit에서 `-t integration`으로 통합 테스트만 선택 실행할 수 있습니다.

`@IntegrationSecurityTest`는 Security 쪽 통합 테스트를 위한 변형 메타 어노테이션으로,  
토큰 발급/인증/인가 플로우에 필요한 Security 설정만 적절히 포함해 사용합니다.

### 0.2 @ServiceIntegrationTest (비HTTP 플로우용)

`src/test/java/com/devon/techblog/config/annotation/ServiceIntegrationTest.java`:

- `@SpringBootTest(webEnvironment = NONE)`
  - 전체 스프링 컨텍스트를 로드하지만 웹 서버/MockMvc는 구동하지 않습니다.
  - Service, Repository, 비동기 처리, 배치, 메시지 리스너 등 **비HTTP 플로우** 위한 통합 테스트에 사용합니다.
- `@ActiveProfiles("test")`
- `@Import(JpaAuditingTestConfig, ImageStorageMockConfig)`
- `@Tag("service-integration")`

### 0.3 @JobIntegrationTest (배치/잡 전용 태그)

`src/test/java/com/devon/techblog/config/annotation/JobIntegrationTest.java`:

- `@ServiceIntegrationTest`를 기반으로 하며, 배치/잡/메시지 리스너 플로우를 위한 통합 테스트에 사용합니다.
- `@Tag("job-integration")`을 추가로 부여해, 잡 관련 통합 테스트만 선택 실행할 수 있습니다.

정리하면:

- HTTP 기반 API 플로우 → `@IntegrationTest` / `@IntegrationSecurityTest`
- 비HTTP 서비스/배치/메시지 플로우 → `@ServiceIntegrationTest` / `@JobIntegrationTest`

모두 **실제 애플리케이션과 거의 동일한 컨텍스트에서 전체 플로우를 검증하는 레이어**입니다.

---

## 1. 통합 테스트의 역할: 플로우 중심

통합 테스트는 **계약(HTTP 상세 규약)보다는 플로우와 인프라 협력에 집중**합니다.

- **엔드 투 엔드 플로우 검증**
  - Controller → Service → Repository → DB → (테스트용) Security/인프라까지 연결된 흐름을 한 번에 검증합니다.
  - 예:
    - `PostIntegrationTest`의 게시글 생성/수정/삭제/조회 플로우
    - `CommentIntegrationTest`의 댓글 생성 → 목록/단건 조회 → 수정 → 삭제 플로우
    - `MemberIntegrationTest`의 프로필 조회/수정/비밀번호 변경/탈퇴 플로우
- **실제 DB/트랜잭션과의 협력**
  - Soft delete, 카운터 증가/감소, 트랜잭션 commit 후 DB 상태 등을 실제 Repository + DB를 통해 검증합니다.
  - 예: 게시글 삭제 후 `isDeleted` 플래그가 true로 바뀌는지, 댓글/좋아요 카운트가 실제로 반영되는지.
- **Security/인증/인가 플로우**
  - `AuthIntegrationTest`처럼, 리프레시 토큰 기반 토큰 재발급 등 Security 플로우를 실제 필터/Provider와 함께 검증합니다.
  - 로그인 상태/다른 사용자 컨텍스트(`TestCurrentUserContext`)에 따른 403/401 흐름을 전체 스택과 함께 확인합니다.
- **복잡한 서비스/비동기/배치/메시지 리스너**
  - REST 외에도 배치 작업, 메시지 리스너, 이벤트 핸들러 등 컨트롤러가 없는 복잡한 Service에 대해,
  - Service + Repository + 외부 연동 Stub/Fake를 함께 올려 플로우를 검증하는 용도로 사용할 수 있습니다.

요약하면, 통합 테스트는 **“이 기능이 실제 애플리케이션 환경에서 처음부터 끝까지 제대로 돈다”**는 것을 확인하는 레이어입니다.

---

## 2. 통합 테스트에서 “하지 않는” 것들

- **모든 비즈니스 분기/경계값을 이 레이어에서 전부 검증하지 않는다**
  - 조건별 분기, 예외 메시지, 길이 제한 등은 **도메인/Service/WebMvc 테스트**에서 주로 다룬다.
  - 통합 테스트에서는 대표적인 성공/실패 플로우만 선택해 검증한다.
- **HTTP 계약의 모든 세부 사항**
  - URL/메서드/바인딩/Validation/전역 예외 매핑/`ApiResponse` 구조에 대한 대부분의 검증은  
    `@ControllerWebMvcTest` 기반 WebMvc 테스트에서 담당한다.
  - 통합 테스트는 필요한 최소한의 상태 코드/핵심 필드만 확인하고,  
    나머지는 HTTP 계약 테스트(WebMvc)에 위임한다.
- **JPA 매핑/쿼리 구현의 세부 내부 동작**
  - 통합 테스트는 쿼리 내용을 직접 검증하지 않고,  
    JPA 매핑/쿼리 정확성은 `@RepositoryJpaTest`에서 집중적으로 검증한다.

이렇게 역할을 나누어, 통합 테스트가 “모든 것을 다 하는” 무거운 레이어가 되지 않게 한다.

---

## 3. 테스트 작성 패턴 (HTTP 기반 플로우)

예: `PostIntegrationTest`, `CommentIntegrationTest`, `MemberIntegrationTest`

- **Given**
  - 실제 Repository를 통해 초기 데이터 상태를 구성한다.
    - `MemberFixture`, `PostFixture`, `CommentFixture` 등으로 엔티티를 생성하고 저장.
    - `TestCurrentUserContext` 등을 통해 현재 사용자 ID 설정.
- **When**
  - `MockMvc`로 실제 엔드포인트를 호출한다.
    - `post("/api/v1/posts")`, `patch("/api/v1/comments/{id}")`, `delete("/api/v1/members/me")` 등.
    - Request DTO는 Application 레벨 Fixture(`PostRequestFixture`, `CommentRequestFixture`, `MemberRequestFixture` 등)를 사용.
- **Then**
  - HTTP 상태 코드와 핵심적인 응답 필드만 검증한다.
    - 예: `status().isCreated()`, `jsonPath("$.success").value(true)`, 핵심 데이터 필드 1~2개.
  - 그리고 **실제 DB 상태를 함께 검증**한다.
    - 예: `postRepository.count()`, `commentRepository.findById`, `member.getStatus()` 등이 기대대로 변경되었는지.

패턴 요약:

- “이 API를 이렇게 호출하면, DB/도메인/보안까지 포함한 전체 시스템 상태가 이 방향으로 바뀐다”를 검증한다.
- HTTP 응답 포맷 전체를 여기서 다 검증하려 하지 않고, **플로우와 상태 변화에 초점**을 맞춘다.

---

## 4. 비HTTP 플로우 (Service/배치/메시지 리스너) 통합 테스트 방향

현재 코드는 대부분 HTTP 기반이지만, 향후 다음과 같은 경우 통합 테스트를 추가할 수 있다.

- 스케줄러(배치 잡)에서 특정 Service 메서드를 주기적으로 호출하는 경우
- 메시지 큐/이벤트 리스너가 들어오는 메시지를 처리하는 경우
- 외부 시스템과의 긴 트랜잭션/워크플로우를 담당하는 Service

이 경우:

- **Given**
  - DB 초기 상태/테스트용 외부 Stub/Fake를 구성한다.
- **When**
  - 해당 Service/리스너/배치 진입점을 직접 호출하거나, Spring이 트리거를 실행하도록 구성한다.
- **Then**
  - DB 상태, 발행된 이벤트, 호출된 외부 Stub 등을 검증해 전체 플로우가 기대대로 수행되었는지 확인한다.

이런 통합 테스트는 HTTP 계약보다는 **서비스 플로우 + DB + 외부 시스템 협력**에 초점을 둔다.

---

## 5. 테스트 픽스처 사용 컨벤션 (통합 테스트)

- **도메인/애플리케이션 Fixture 재사용**
  - `MemberFixture`, `PostFixture`, `CommentFixture`, `PostRequestFixture`, `CommentRequestFixture`, `MemberRequestFixture` 등  
    이미 정의된 Fixture를 적극 재사용해 Given 부분을 간결하게 유지한다.
- **현재 사용자/보안 컨텍스트**
  - `TestCurrentUserContext`와 테스트용 Security 설정(`TestSecurityConfig`)을 사용해,
    인증/인가 플로우를 실제와 유사하게 구성한다.
  - 예: `currentUserContext.setCurrentUserId(savedMember.getId());`.
- **필요 이상으로 복잡한 설정 자제**
  - 통합 테스트는 이미 전체 컨텍스트를 올리므로,  
    추가적인 Mock/Spy 설정은 최소화하고, 가급적 실제 빈/설정을 사용한다.

---

## 6. 언제 통합 테스트를 추가/수정할까?

- **핵심 유즈케이스 플로우**
  - 게시글/댓글/회원/인증처럼 제품에서 중요한 플로우는 최소 한 개 이상의 통합 테스트로 커버한다.
  - 예: “게시글 생성→수정→삭제”, “댓글 생성→조회→삭제”, “회원 탈퇴 후 재조회 시 404”.
- **설정/인프라/보안 관련 변경**
  - Security 설정, JPA Auditing, 이미지 스토리지 등 인프라 설정을 변경할 때,
  - 관련 통합 테스트를 추가/수정해 “설정이 실제 동작에 미치는 영향”을 확인한다.
- **복잡한 Service/비동기 처리 추가**
  - 새로운 배치 작업, 메시지 리스너, 복잡한 오케스트레이션 Service가 추가될 때,
  - 해당 플로우를 실제 DB/외부 Stub과 함께 검증하는 통합 테스트를 한두 개 작성한다.

단, 모든 비즈니스 분기를 통합 테스트에 몰지 말고,  
도메인/서비스/WebMvc 테스트와 역할을 분리해 테스트 피라미드의 균형을 유지한다.

---

## 7. 요약

- 통합 테스트는 **전체 플로우와 인프라 협력(Controller/Service/Repository/DB/Security)**을 실제 컨텍스트에서 검증하는 레이어다.
- HTTP 계약 세부사항, 세밀한 비즈니스 분기/경계값, JPA 구현 세부는 다른 레이어(도메인/Repository/Service/WebMvc)가 담당한다.
- 통합 테스트는 대표적인 플로우에만 집중해, “이 기능이 실제 환경에서 처음부터 끝까지 제대로 동작하는지”를 보장하는 역할을 맡는다.
