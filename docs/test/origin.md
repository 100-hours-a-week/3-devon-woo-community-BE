# 이 프로젝트에서 테스트 커버리지를 이렇게 설계한 이유

이 문서는 “숫자로만 좋은 테스트 커버리지”가 아니라, 실제로 **버그를 막고 리팩터링을 돕는 유효한 테스트 커버리지**를 이 프로젝트에서 어떻게 정의하고 구현했는지 정리한 문서입니다. 
단순히 “커버리지 몇 %”를 맞추는 것을 목표로 하지 않고, **어떤 코드를 얼마나, 어떤 방식으로 검증할지**에 대한 기준과 실제 설정/코드를 함께 설명합니다. 

---

## 1. 이 프로젝트에서 말하는 “유효한 테스트 커버리지”

일반적으로 테스트 커버리지는 JaCoCo 같은 도구가 계산하는 **라인/브랜치 커버리지 숫자**로 표현됩니다.  
그러나 이 숫자 자체가 품질을 보장하지는 않습니다. 이 프로젝트에서 “유효한 테스트 커버리지”는 다음을 만족하는 상태를 의미합니다.

- **핵심 도메인 규칙**이 실패하면 실제 버그로 이어질 만큼 충분히 검증되어 있을 것  
- **입출력(컨트롤러)과 도메인 사이의 흐름**이 통합 테스트로 검증될 것  
- **테스트 대상이 아닌 코드(예: 단순 DTO, 설정, 자동 생성 코드)**는 커버리지 계산 대상에서 제외할 것  
- 숫자 기준(최소 70%)은 **최소선**일 뿐이고, 중요한 도메인/정책 코드는 가능한 한 100%에 가깝게 유지할 것

정리하면 이 프로젝트의 커버리지 전략은 다음과 같습니다.

- “무조건 90% 이상” 같은 **일률적인 목표**를 추구하지 않습니다.
- “도메인 규칙과 주요 플로우는 빠짐없이 검증하고, 나머지는 현실적인 선에서 관리”하는 **가중치 기반 목표**에 가깝게 설계합니다.

---

## 2. 목적과 계층에 맞는 테스트 코드 작성

처음에는 단순히 **단위 테스트**와 **통합 테스트**로만 나누어 생각하고,  
모든 코드에 같은 패턴으로 단위 테스트와 통합 테스트를 작성하는 방식으로 접근했습니다.  
서비스(Service) 레이어에서만 충분히 검증되면, Controller나 Repository의 테스트는 없어도 된다고 판단하기도 했습니다.

이 방식은 각 계층의 역할과 테스트의 목적을 명확하게 정의하지 못했다는 점에서 한계가 있었습니다.  
그래서 이 프로젝트에서는 다음과 같이 애플리케이션을 계층으로 나누고, 계층별로 **무엇을 어떤 범위까지 검증할지**를 다시 정리했습니다.

- **Presentation**  
  - 클라이언트와 직접 통신하는 표현 계층(Controller, Request/Response DTO 등)입니다.
  - 요청/응답 스펙이 올바른지, 검증 로직과 매핑이 정상 동작하는지 확인합니다.
- **Business(UseCase)**  
  - 서비스 레이어에서 도메인 규칙과 인프라를 조합해 실제 유스케이스를 수행하는 계층입니다.
  - 도메인 규칙이 올바르게 호출되고, 의존성(Repository, 외부 서비스 등)과의 협력이 의도한 대로 동작하는지 검증합니다.
- **Domain**  
  - 엔티티와 도메인 정책(Policy) 등, 핵심 비즈니스 규칙을 담당합니다.
  - 외부 의존성이 없는 순수한 검증 로직을 단위 테스트로 집중적으로 검증합니다.
- **Infra/Repository**  
  - DB, 외부 시스템과의 실제 연동을 담당합니다.
  - JPA Repository나 QueryDSL 쿼리가 “동작하는가”를 넘어서, **의도한 조건과 정렬, 필터링이 정확히 반영되는지**를 테스트합니다.

이렇게 계층을 나누어 보니, 각 계층에서 작성해야 하는 테스트의 **목적과 범위**가 더 명확해졌습니다.

- Presentation 계층은 `@WebMvcTest` 기반의 슬라이스 테스트(이 프로젝트에서는 `@ControllerWebMvcTest` 메타 어노테이션)를 사용해,  
  Servlet~Controller 구간에서 요청/응답과 검증 로직을 확인합니다.
- Domain 계층은 `@UnitTest` 기반 단위 테스트로, 경계값과 예외 상황까지 포함해 정책을 세밀하게 검증합니다.
- Repository 계층은 `@DataJpaTest` 기반 슬라이스 테스트(이 프로젝트에서는 `@RepositoryJpaTest`)를 사용해,  
  JPA/QueryDSL 쿼리가 의도한 대로 동작하는지 확인합니다.
- Business(UseCase) 계층은  
  - Service 단위 테스트에서 Repository/외부 모듈을 Stub/Mock/Fake로 대체하여 비즈니스 로직 자체를 검증하고,  
  - 통합 테스트에서 Controller–Service–Repository–DB까지 실제 흐름이 요구사항대로 동작하는지 최종적으로 확인합니다.

이 계층별 관점을 기반으로 이후 섹션들에서 단위 테스트와 통합 테스트, 커버리지 설정을 설명합니다.

---

## 3. 테스트 타입과 역할 분리

이 프로젝트의 테스트는 JUnit 태그와 메타 어노테이션으로 **단위 테스트와 통합 테스트를 명확히 구분**합니다.

- `@UnitTest` (태그: `unit`)
  - Spring 컨텍스트 없이 순수 자바 기반으로 동작하는 단위 테스트입니다.
  - Mockito 기반으로 협력 객체를 대체하여 도메인 로직, 서비스, 검증기(Validator) 등 외부 인프라에 의존하지 않는 코드를 검증합니다.
- `@IntegrationTest` (태그: `integration`)
  - 실제 Spring 컨텍스트, 데이터베이스(H2), MockMvc를 사용하는 통합 테스트입니다.
  - HTTP 요청–응답 흐름, DB 반영, 보안/필터 등 **실제 사용자 시나리오**에 가까운 경로를 검증합니다.

Gradle에서는 태그를 기준으로 테스트 태스크를 분리해 실행합니다 (`build.gradle` 기준).

- `test`: 전체 테스트 실행 (기본)  
- `unitTest`: `@Tag("unit")`만 필터링해 실행  
- `integrationTest`: `@Tag("integration")`만 필터링해 실행  

이렇게 나누는 이유는 다음과 같습니다.

- **단위 테스트**는 실행 속도가 빠르고, 실패 시 “어떤 규칙이 깨졌는지”를 바로 좁혀서 확인할 수 있습니다.
- **통합 테스트**는 상대적으로 느리지만, “실제 사용 시 제대로 동작하는지”를 검증합니다.
- 서로 장단점이 다르기 때문에 둘을 섞지 않고 **역할과 책임을 분리**하는 것이 중요합니다.

---

## 4. 커버리지 도구와 빌드 설정 (JaCoCo)

이 프로젝트는 `build.gradle`에서 JaCoCo를 사용해 테스트 커버리지를 측정하고, **최소 70%**를 강제합니다.

- `jacoco` 플러그인을 적용합니다.
- `jacocoTestReport`를 통해 HTML/XML 리포트를 생성합니다.
- `jacocoTestCoverageVerification`으로 최소 커버리지(0.70) 미만일 때 빌드를 실패하도록 설정합니다.

또한 다음과 같은 클래스들은 커버리지 대상에서 제외합니다.

- `**/Q*.class` : QueryDSL이 생성하는 Q 클래스 (자동 생성 코드)
- `**/*Application.class` : Spring Boot 메인 클래스 (부트스트랩 전용)
- `**/*Config*.class` : 설정 클래스 (주로 wiring / 외부 연동 설정)
- `**/dto/**` : 순수 데이터 전달 객체
- `**/exception/**` : 예외 정의 클래스

이렇게 제외하는 이유는 다음과 같습니다.

- 자동 생성 코드나 단순 DTO/예외는 **테스트를 통해 도메인 규칙을 보장하는 대상이 아니기 때문**입니다.
- 이들을 포함시키면 “커버리지 숫자”는 올라가지만, 실제로 **버그를 줄이는 데 거의 기여하지 않습니다.**
- 오히려 이런 코드까지 커버리지를 맞추기 위해 의미 없는 테스트가 양산될 위험이 큽니다.

결과적으로 JaCoCo 설정은 **테스트가 실제 가치 있는 코드에 집중되도록** 커버리지 계산 범위를 제한하는 역할을 합니다.

---

## 5. 단위 테스트와 계층별 슬라이스 테스트

단위 테스트는 이 프로젝트에서 단순히 “도메인 엔티티만 검증하는 테스트”를 의미하지 않습니다.  
다음과 같이 여러 계층에서, 서로 다른 목적을 가진 단위·슬라이스 테스트를 포함합니다.

- 도메인 정책과 엔티티 규칙 검증
- 서비스 유스케이스와 Validator 검증
- Presentation(WebMvc) 슬라이스 테스트
- Repository(JPA) 슬라이스 테스트

### 5.1. 도메인 정책 테스트 예시

좋아요(Like) 기능의 도메인 정책을 담당하는 `PostLikePolicy`에 대해, 이 프로젝트는 다음과 같은 단위 테스트를 작성합니다 (`PostLikePolicyTest` 기준입니다).

- “좋아요를 눌렀는지 여부”를 `PostLikeRepository`로 조회합니다.
- 그 결과에 따라 `validateCanLike`, `validateCanUnlike`가 예외를 던질지, 통과시킬지를 검사합니다.

테스트는 `@UnitTest` 메타 어노테이션을 사용해 Mockito와 JUnit 태그를 한 번에 적용합니다 (`src/test/java/com/devon/techblog/config/annotation/UnitTest.java` 기준입니다).

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(MockitoExtension.class)
@Tag("unit")
public @interface UnitTest {}
```

이 어노테이션을 사용한 단위 테스트는 다음과 같은 특성을 가집니다.

- Spring 컨텍스트 없이 **순수 자바 코드만** 테스트합니다.
- 협력 객체(`PostLikeRepository`)는 Mockito로 대체하여 **도메인 규칙만** 집중적으로 검증합니다.
- Gradle에서 `./gradlew unitTest`로 단독 실행이 가능하여 빠른 피드백 루프를 제공합니다.

이러한 단위 테스트는 “좋아요 정책이 요구사항대로 동작하는지”를 **정확하게 문서화**하는 역할도 수행합니다.

### 5.2. 서비스와 Validator 단위 테스트

`@UnitTest`는 도메인 정책뿐 아니라 서비스와 Validator에도 적용합니다.  
예를 들어 `PostServiceTest`, `SignupServiceTest`, `MemberServiceTest`, `AuthValidatorTest` 등에서는 다음을 검증합니다.

- 서비스가 도메인 엔티티와 정책을 올바른 순서와 조건으로 호출하는지
- Repository, 외부 모듈(Fake/Mock)과의 상호 작용이 의도한 대로 이루어지는지
- Validator가 잘못된 입력에 대해 적절한 예외나 오류를 발생시키는지

이 계층의 단위 테스트는 “하나의 유스케이스가 도메인 규칙을 어떻게 조합하는지”를 검증하는 역할을 합니다.

### 5.3. Presentation(WebMvc) 슬라이스 테스트

표현 계층은 `@ControllerWebMvcTest` 메타 어노테이션을 사용해 WebMvc 슬라이스 테스트를 수행합니다 (`ControllerWebMvcTest` 기준입니다).

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@WebMvcTest
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityConfig.class)
public @interface ControllerWebMvcTest {
    // ...
}
```

이 테스트들은 `PostControllerTest`, `CommentControllerTest`, `MemberControllerTest` 등에서 사용하며, 다음을 검증합니다.

- HTTP 요청 경로, HTTP 메서드, 상태 코드가 의도한 대로 매핑되는지
- Request DTO의 바인딩과 검증이 올바르게 동작하는지
- Service에서 반환된 Response가 API 스펙에 맞게 직렬화되어 전달되는지

Spring 전체 컨텍스트를 띄우지 않고 WebMvc 영역만 로드하기 때문에, 표현 계층을 빠르게 검증하는 단위/슬라이스 테스트에 가깝습니다.

### 5.4. Repository(JPA) 슬라이스 테스트

Repository 계층은 `@RepositoryJpaTest` 메타 어노테이션을 사용해 JPA 슬라이스 테스트를 수행합니다 (`RepositoryJpaTest` 기준입니다).

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ActiveProfiles("test")
@Import({QueryDslConfig.class, JpaAuditingTestConfig.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DataJpaTest
@Tag("repository")
public @interface RepositoryJpaTest {}
```

`PostRepositoryTest`, `CommentRepositoryTest`, `AttachmentRepositoryTest`, `MemberRepositoryTest` 등에서 이 어노테이션을 사용하여 다음을 검증합니다.

- QueryDSL/JPA 쿼리가 의도한 조건, 정렬, 페이징을 정확하게 반영하는지
- Soft Delete, 연관 관계, 지연 로딩 등 JPA 매핑이 기대대로 동작하는지

이 테스트들은 실제 DB(H2 등 테스트용 설정)를 사용하되, JPA 관련 컴포넌트만 로드하는 슬라이스 테스트로,  
Repository 계층이 “의도한 대로 데이터를 읽고 쓰는지”를 집중적으로 검증합니다.

---

## 6. 통합 테스트: 실제 요청–응답 플로우 검증

### 5.1. 통합 테스트 공통 설정

통합 테스트는 `@IntegrationTest` 메타 어노테이션 하나로 공통 설정을 묶어서 사용합니다 (`src/test/java/com/devon/techblog/config/annotation/IntegrationTest.java` 기준입니다).

핵심 내용만 요약하면 다음과 같습니다.

```java
@ActiveProfiles("test")
@Import({TestSecurityConfig.class, JpaAuditingTestConfig.class, ImageStorageMockConfig.class})
@SpringBootTest
@AutoConfigureMockMvc
@Tag("integration")
public @interface IntegrationTest {}
```

이 설정으로 통합 테스트는 다음과 같이 동작합니다.

- `test` 프로파일 설정을 사용합니다.
- 실제 Spring Boot 컨텍스트를 띄웁니다.
- `MockMvc`를 통해 HTTP 요청–응답을 시뮬레이션합니다.
- 보안/이미지 스토리지/JPA Auditing 등을 **테스트용 설정**으로 교체해 사용합니다.

### 5.2. 게시글 통합 테스트 예시

`PostIntegrationTest`는 `/api/v1/posts` 관련 API에 대해 다음을 검증합니다.

- 게시글 생성 시 201과 함께 생성된 데이터가 반환되는지 여부를 검증합니다.
- 게시글 수정 후 조회 시 수정된 내용이 반영되는지 여부를 검증합니다.
- 삭제 API 호출 후 실제로 Soft Delete 되었는지 여부를 검증합니다.
- 좋아요/좋아요 취소 요청이 DB 상태에 제대로 반영되는지 여부를 검증합니다.

이 통합 테스트는 실제 `PostRepository`, `PostLikeRepository`, `MemberRepository`를 사용하고,  
`MockMvc`를 통해 실제 API 요청처럼 테스트합니다. 이로써 **도메인, 인프라, 웹 계층 전체를 한 번에 검증**합니다.

단위 테스트가 “규칙 단위”를 검증한다면, 통합 테스트는 “사용자 시나리오 단위”를 검증하도록 구성합니다.

---

## 7. 커버리지 기준(70%)을 이렇게 잡은 이유

`jacocoTestCoverageVerification` 설정에서 최소 커버리지 `0.70`을 강제하고 있습니다.  
이 수치는 다음과 같은 현실적인 고려에서 나온 값입니다.

- 새로운 기능을 빠르게 개발하면서도,
- 테스트가 전혀 없는 코드를 방지하고,
- DTO/설정/예외 등을 제외한 **실질적인 로직**에 대해 어느 정도의 안전망을 확보하기 위한 수준입니다.

이 프로젝트의 목표는 “처음부터 90~100%를 달성하는 것”이 아니라 다음에 가깝습니다.

- **도메인/정책/서비스**와 같은 핵심 코드의 커버리지를 점진적으로 끌어올립니다.
- 커버리지가 낮은 영역을 리포트에서 확인하고 **우선순위를 정해 보완**합니다.

즉, 70%는 **품질의 상한선**이 아니라,  
“이 아래로 내려가면 위험 신호이므로 빌드를 실패시킨다”는 **하한선 경고 장치**에 가깝습니다.

---

## 8. 이 프로젝트에서 “좋은 테스트”의 기준

이 프로젝트에서 좋은 테스트는 단순히 커버리지 퍼센트를 올리는 테스트가 아니라, 다음을 만족하는 테스트입니다.

- **도메인 규칙이 명확하게 드러난다.**
  - 예: “이미 좋아요를 누른 상태에서 다시 누르면 예외가 발생한다.”
- **실제 사용 시나리오와 밀접하게 연결된다.**  
  - 예: “게시글을 생성하고 나서 상세 조회하면 같은 내용이 반환된다.”
- **설정/인프라 디테일에 과도하게 의존하지 않는다.**
  - 도메인 규칙은 단위 테스트에서, 인프라 연동은 통합 테스트에서 각각의 책임에 맞게 검증
- **테스트가 깨질 때, 어디가 문제인지 빠르게 파악할 수 있다.**
  - 테스트 이름과 실패 메시지가 “어떤 요구사항이 깨졌는지”를 설명한다.

이 기준을 충족하도록 테스트를 추가하고 수정하면, 자연스럽게 **유효한 테스트 커버리지**가 올라가고,  
JaCoCo 숫자는 그 결과를 숫자로 확인하는 도구가 됩니다.

---

## 9. 앞으로의 확장 방향

현재 설정과 구조는 다음과 같은 확장성을 염두에 두고 있습니다.

- 새로운 도메인 정책이 생기면 `@UnitTest` 기반 테스트를 먼저 작성하여 규칙을 문서화합니다.
- 새로운 API 엔드포인트가 생기면 주요 흐름을 `@IntegrationTest`로 검증합니다.
- 커버리지 리포트를 정기적으로 확인하면서, 커버리지가 낮은 도메인 영역부터 테스트를 보강합니다.

요약하면, 이 프로젝트의 테스트/커버리지 전략은 다음과 같습니다.

- **도메인 규칙과 실제 플로우에 집중한 테스트 작성**
- **DTO/설정/자동 생성 코드 등 비핵심 영역 제외**
- **현실적인 최소 커버리지(70%)를 기준으로 점진적 개선**

이 전략을 통해 “숫자만 높은 커버리지”가 아니라, **실제 버그를 줄이고 리팩터링을 뒷받침하는 테스트 커버리지**를 추구합니다.
