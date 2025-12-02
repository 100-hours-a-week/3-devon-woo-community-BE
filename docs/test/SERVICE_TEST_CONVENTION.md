# 서비스 단위 테스트 컨벤션 (Service + @UnitTest)

> 대상: `@UnitTest` 기반 서비스 테스트  
> 예: `PostServiceTest`, `CommentServiceTest`, `PostLikeServiceTest` 등 `application.*.service` 패키지

서비스 단위 테스트는 **Controller 아래, Repository/도메인 위**에서 동작하는 비즈니스 플로우를 검증하는 레이어이다.  
JPA/HTTP/보안/인프라 세부는 다른 레이어에 맡기고, **분기·예외·오케스트레이션**에 집중한다.

---

## 0. @UnitTest 메타 어노테이션

`src/test/java/com/devon/techblog/config/annotation/UnitTest.java`:

- `@ExtendWith(MockitoExtension.class)`  
  - Spring 컨텍스트 없이 JUnit + Mockito만 사용하는 **순수 유닛 환경**.
- `@Tag("unit")`  
  - `-t unit`으로 단위 테스트만 실행 가능.

서비스 테스트에서는 이 어노테이션을 붙이고, 의존성은 `@Mock`, 대상 서비스는 `@InjectMocks`로 구성한다.

---

## 1. 무엇을 검증하는가 (역할)

서비스 단위 테스트는 다음을 검증한다.

- **도메인/Repository/정책 오케스트레이션**
  - 예: `CommentService.createComment`
    - 게시글 존재 확인 → 회원 조회 → `Comment.create` → 저장 → 댓글 카운트 증가.
  - 예: `PostLikeService.likePost`
    - 게시글 조회 → 회원 존재 확인 → `PostLikePolicy` 검증 → `PostLike` 저장 → 좋아요 카운트 증가.
- **비즈니스 분기와 예외 흐름**
  - 대상이 없을 때(`POST_NOT_FOUND`, `COMMENT_NOT_FOUND`, `USER_NOT_FOUND` 등) 어떤 `CustomException`이 나는지.
  - 소유권/권한 정책(`OwnershipPolicy`, `PostLikePolicy`)을 어떻게 적용하는지.
- **서비스 레벨에서 의미 있는 부작용**
  - 댓글/좋아요 생성·삭제 시 카운터 증가/감소 메서드 호출 여부.
  - 외부 시스템(메일, 알림, 결제 등)이 있다면 해당 클라이언트 호출 여부.
- **조회/필터/페이징 조합 로직**
  - Repository에서 가져온 DTO/Entity를 Response DTO로 어떻게 변환·조합하는지.
  - 검색 조건/태그 필터/좋아요 여부 플래그 등이 올바르게 계산되는지.

---

## 2. 무엇을 검증하지 않는가 (경계)

서비스 단위 테스트는 다음을 다루지 않는다.

- **JPA 매핑/쿼리/트랜잭션 동작**  
  → `@RepositoryJpaTest` + 통합 테스트(`INTEGRATION_TEST_CONVENTION.md`)에서 검증.
- **HTTP 레이어 계약(Request/Response)**  
  → `@ControllerWebMvcTest` (`CONTROLLER_WEBMVC_TEST_CONVENTION.md`)가 담당.
- **Security/Filter/AOP/환경 설정**  
  → `@IntegrationTest` / `@IntegrationSecurityTest` 통합 테스트에서 검증.

즉, 서비스 테스트는 **순수 비즈니스 플로우**만 본다.

---

## 3. 기본 패턴

구조: `@UnitTest` + `@Mock` 의존성 + `@InjectMocks` 서비스.

### 3.1 성공 플로우

- Given
  - Request DTO / 도메인 엔티티를 Fixture로 준비 (`PostRequestFixture`, `CommentFixture`, `MemberFixture` 등).
  - Repository/Policy Mock에 `given(...)` / `willReturn(...)` 으로 정상 플로우 설정.
- When
  - 서비스 메서드 호출 (`service.createXxx`, `service.updateXxx`, `service.getXxx` 등).
- Then
  - 반환 DTO의 핵심 필드 검증 (ID, 제목, 내용, 플래그, 카운트 등).
  - 필요 시 의미 있는 부작용에 대해서만 `verify(...)` 수행  
    (예: `postRepository.incrementCommentCount(postId)` 호출 여부).

### 3.2 실패/예외 플로우

- Given
  - Repository/Policy Mock이 실패 상황을 만들도록 설정  
    (조회 결과 없음, 정책 위반 시 예외 던지기 등).
- When & Then
  - 서비스 메서드를 호출하고 `assertThatThrownBy`로 예외 타입/메시지 안에 ErrorCode 메시지가 포함되는지 검증.  
    예: `createPost_memberNotFound_throwsException`, `updateComment_notOwner_throwsException`.

### 3.3 조회/필터/페이징

- Given
  - Repository Mock이 `Page<Dto>` 또는 리스트를 반환하도록 설정.
- When
  - 서비스 메서드로 페이지/리스트 조회.
- Then
  - 결과 개수, 첫 요소의 ID, 핵심 필드 검증.  
  - 쿼리 구현 자체는 Repository 테스트가 책임지고, 서비스 테스트는 “결과를 어떻게 감싸고 가공하는지”만 본다.

---

## 4. Mock vs 실제 객체 사용 기준

- **Repository / 외부 시스템 / 복잡 정책**
  - 기본적으로 `@Mock` 사용.
  - 서비스 테스트에서는 “이 입력 → Mock이 이렇게 응답 → 서비스가 이렇게 동작/예외” 흐름에 집중.
- **단순 정책(외부 의존성 없음)**
  - `OwnershipPolicy`처럼 단순 비교/예외만 있는 정책은 실제 객체를 써도 무방하다  
    (예: `CommentServiceTest`에서 실제 `OwnershipPolicy` 사용).
  - 단, 해당 정책의 유닛 테스트(`OwnershipPolicyTest`)는 별도로 존재해야 한다.
- **외부 클라이언트**
  - HTTP/메시지 클라이언트는 인터페이스로 추상화하고, 서비스 테스트에서는 Fake/Mock을 주입해 호출 여부/파라미터만 검증한다.

요약: **협력자는 대부분 Mock**, 단순 정책은 상황에 따라 실제 객체 사용 가능.

---

## 5. 테스트 픽스처 사용 컨벤션 (Service)

- **Request/도메인 Fixture 적극 활용**
  - Given 단계에서 `*RequestFixture`, `*Fixture`를 사용해 의도를 드러낸다.
  - 예: `PostRequestFixture.createRequest()`, `CommentRequestFixture.updateRequest()`, `MemberFixture.createWithId(1L)`.
- **Response는 핵심 필드 위주로 검증**
  - 서비스 테스트에서는 Response DTO 전체를 Fixture로 감추기보다는,
  - 반환 DTO의 핵심 필드만 `assertThat`으로 직접 검증하는 것을 기본으로 한다.
  - 여러 테스트에서 똑같은 Response 구성이 반복될 때만, Response Fixture/헬퍼 도입을 고려한다.

---

## 6. 단순 위임/얇은 서비스에 대한 기준

- **테스트 대상이 되는 경우**
  - 서비스 메서드 내부에 분기/정책/검증/여러 협력자 호출이 있다.
  - 도메인/Repository/외부 시스템 간 **오케스트레이션**을 수행한다.
  - 과거에 버그가 있었거나, 실수하기 쉬운 플로우다.
- **별도 테스트를 생략해도 되는 경우**
  - 단순히 다른 레이어로 메서드만 위임하는 thin service  
    (`return repository.findById(id);`)에 불과하고,
  - 해당 로직이 Repository/도메인/통합 테스트에서 이미 충분히 검증되고 있다.

서비스 테스트도 도메인 테스트와 마찬가지로,  
**“규칙/플로우/분기”에 집중하고, 단순 위임까지 모두 테스트 대상으로 삼지는 않는다.**

---

## 7. 언제 서비스 단위 테스트를 추가/수정할까?

- **새 서비스 메서드/유즈케이스 추가 시**
  - 성공/실패/대표 경계 케이스를 커버하는 테스트를 함께 작성.
- **도메인 정책·분기 로직 변경 시**
  - 변경된 규칙이 서비스 레벨에서 어떻게 반영되는지(예외 타입/메시지, 호출 흐름)를 테스트에 반영.
- **버그 발생 시**
  - 재현 가능한 최소한의 서비스 테스트를 추가해 회귀 방지.
- **통합 테스트에서 발견된 문제를 좁히고 싶을 때**
  - 같은 플로우를 서비스 단위 테스트로 재현해, 문제 원인을 레벨별로 분리·확인한다.

---

## 8. 요약

- 서비스 단위 테스트는 **비즈니스 플로우와 분기·예외 처리**를 빠르게 검증하는 레이어다.
- JPA/HTTP/보안/인프라 세부는 다른 레이어에 맡기고,  
  “이 입력/상태에서 서비스가 어떤 도메인/리포지토리/정책을 어떻게 조합해 어떤 결과/예외를 내야 하는가”에 집중한다.
- Mock/Fixture를 적절히 활용해 테스트를 빠르고 가볍게 유지하면서, 핵심 유즈케이스를 촘촘하게 보호한다.

