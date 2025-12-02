# Comment/Like 서비스 단위 테스트·통합 테스트 평가

## 1. CommentService / CommentServiceTest 평가

- `@UnitTest` + Mockito 기반 순수 단위 테스트.
- `CommentRepository`, `MemberRepository`, `PostRepository`를 모두 `@Mock`으로 주입하고, `OwnershipPolicy`만 실제 객체를 사용.
- 주요 시나리오:
  - 댓글 생성 성공/실패
  - 댓글 수정/삭제 성공
  - 존재하지 않는 댓글/게시글/회원 예외 검증
  - 소유자 검증 예외
  - 댓글 카운트 증감 시나리오(`incrementCommentCount`, `decrementCommentCount`)에서 `verify` 사용.
- 검증 방식:
  - 대부분은 리턴값을 이용한 **상태 검증**(content, 예외 메시지 등).
  - 카운트 증가/감소에 대해서만 **행위 검증(Mock verify)** 적용.

## 2. Mock 사용 관점에서의 평가

### 2.1 장점

- **외부 의존성 격리**:
  - DB 접근(Repository)들을 Mock 처리하여 테스트를 빠르게 유지하고, `OwnershipPolicy` 도메인 로직만 실제로 활용.
- **비즈니스 규칙 중심 검증**:
  - 댓글 내용 변경, 예외 코드/메시지, 소유자 정책 등 “무엇을 하는지(What)”를 검증하는 테스트가 다수.
- **행위 검증의 적절한 사용**:
  - `incrementCommentCount`, `decrementCommentCount`는 도메인 규칙상 “해당 메서드가 호출되어야 한다”는 의미가 있으므로, `verify(postRepository)` 사용이 자연스러운 편.

### 2.2 개선 여지가 있는 부분

- **모든 Repository를 Mock으로만 다루는 패턴 고정**
  - 단순 CRUD 및 조회 로직까지 모두 Mocking 되면서, 향후 도메인 모델이 풍부해지면 “Mock 설정이 많아지는 테스트”로 변질될 가능성이 있음.
  - 현재는 과도하지 않지만, **서비스 메서드가 복잡해질수록 Stub/Mock 설정이 구현 세부에 더 많이 노출될 위험**이 있다.
- **페이지 조회 테스트의 의존성**
  - `getCommentPageByPostId` 테스트는 `CommentRepository.findCommentPageByPostIdWithMemberAsDto`를 Mock하여 `Page<CommentQueryDto>`를 직접 구성하고 있음.
  - 현재 수준에서는 허용 가능한 복잡도지만, 조회 조건이 늘어나면 **Mock을 위한 DTO 준비 코드가 테스트 대부분을 차지할 여지**가 있다.
- **통합 테스트와의 역할 분리 부족**
  - 현재 CommentService에 대한 **통합 테스트 / 슬라이스 테스트(Spring Data + H2 등)**가 별도로 정의되어 있지 않기 때문에,
  - “실제 Repository 구현과의 협력”은 다른 계층에서 검증되고 있는지 확인이 필요하다.

## 3. “구현에 대한 과도한 명세” 관점에서의 진단

요청에서 제시된 문제의식(테스트 더블 과사용, 내부 구현 명세화) 기준으로 보면:

1. **불필요한 verify 남용은 거의 없음**
   - verify는 댓글 카운트 증가/감소처럼 비즈니스적으로 의미 있는 행위만 검증하고 있으며,
   - 내부 메서드 호출 순서나 세부 구현(예: `findById`, `save` 호출 횟수 등)을 구체적으로 명세하지 않는다.
2. **테스트는 주로 결과와 예외 코드에 초점**
   - 예외 메시지 및 ErrorCode, 응답 DTO의 내용 중심으로 검증하고 있어 “어떻게(How)”보다는 “무엇(What)”에 더 가깝다.

결론적으로, **현재 CommentServiceTest는 Mock을 사용하지만 “과도한 행위 검증으로 인한 깨지기 쉬운 테스트” 수준은 아니다.**  
테스트 더블 가이드에서 말하는 “구현에 대한 과도한 명세” 문제는 거의 없고, **현재 형태를 유지하되 통합 테스트와의 역할만 명확히 나누면 충분하다.**

## 2. PostLikeService / PostLikeServiceTest 평가

### 2.1 현재 구조 요약

- `@UnitTest` + Mockito 기반 순수 단위 테스트.
- 의존성:
  - `PostRepository`, `PostLikeRepository`, `MemberRepository`, `PostLikePolicy`를 모두 `@Mock`으로 주입.
  - `PostLikeService`는 `@InjectMocks`로 생성.
- 주요 시나리오:
  - 좋아요 성공
  - 존재하지 않는 게시글/회원 예외
  - 이미 좋아요한 게시글 예외 (`PostLikePolicy.validateCanLike` 예외 발생)
  - 좋아요 취소 성공
  - 존재하지 않는 게시글 좋아요 취소 예외
  - 좋아요하지 않은 게시글의 좋아요 취소 예외 (`PostLikePolicy.validateCanUnlike` 예외 발생)
- 검증 방식:
  - 예외 발생 여부 위주의 **상태 검증**.
  - `incrementLikeCount`, `decrementLikeCount`, `save`, `deleteByPostIdAndMemberId` 등에 대해 `verify`는 하지 않고 있음.

### 2.2 Mock 사용 관점 평가

- **장점**
  - `PostLikePolicy`를 분리하여, “좋아요 가능 여부” 규칙은 정책 객체에서, 서비스는 “흐름 제어 + Repository 호출”에 집중하게 설계되어 있다.
  - 단위 테스트는 대부분 “어떤 경우에 어떤 CustomException이 나야 하는가”를 검증하며, 내부 구현 디테일(저장/삭제 호출 여부 등)에 크게 집착하지 않는다.
- **개선 여지**
  - `likePost_success`, `unlikePost_success`는 현재 아무 검증도 하지 않고 호출만 하고 끝난다.
    - 최소한 “예외가 발생하지 않는다” 수준으로도 Assert를 명시하거나,
    - 비즈니스적으로 중요하다면 `postLikeRepository.save`, `postLikeRepository.deleteByPostIdAndMemberId`, `postRepository.incrementLikeCount`, `postRepository.decrementLikeCount`에 대한 호출 여부를 검증할 수 있다.
    - 다만, 이 부분은 정책/트랜잭션 측면에서 이미 통합 테스트(`PostIntegrationTest`)가 있기 때문에, **굳이 강하게 verify를 걸어 “구현 명세”로 만들 필요까지는 없다.**
  - 정책 객체 `PostLikePolicy`는 완전히 Mock 처리되어 있어,
    - 정책 로직에 대한 테스트는 별도 `PostLikePolicyTest`와 같은 단위 테스트에서 담당하는 편이 좋다.

### 2.3 “구현 과명세” 관점 진단

- 현재 PostLikeServiceTest는
  - Repository/Policy를 모두 Mock으로 두고 있지만,
  - 호출 순서/횟수/인자 등에 대해 verify를 거의 사용하지 않아, **구현에 강하게 결합된 테스트는 아니다.**
- 오히려 반대 방향으로,
  - 성공 시나리오가 “아무 것도 검증하지 않는 테스트”에 가깝기 때문에,
  - “테스트가 도메인 규칙을 충분히 문서화하고 있느냐” 관점에서 아쉬움이 조금 있다.

요약하면, **PostLikeServiceTest는 Mock 과사용/구현 과명세 문제는 없지만, 성공 시나리오의 검증이 다소 약한 편**이다.  
정책/통합 테스트가 이미 보완하고 있으므로, 현재도 치명적인 문제는 아니다.

## 3. 전체 목표 (Comment + PostLike)

1. **Mock은 “필수적인 외부 의존성”과 “행위에 의미가 있는 곳”에만 사용**한다.
2. **조회/상태 위주의 시나리오는 Stub/Fake 또는 통합 테스트에 분산**해 “Mock 설정에 의존하는 테스트”를 줄인다.
3. **CommentService의 핵심 비즈니스 규칙을 상위 수준에서 검증**하여 리팩토링 내성을 높인다.

## 4. 구체적인 수정/보완 계획

### 4.1 CommentServiceTest 유지/정리 방향

- **유지할 테스트**
  - 댓글 생성/수정/삭제의 성공/실패 시나리오
  - 회원/게시글/댓글 미존재 예외 시나리오
  - 소유자 검증 예외 시나리오
  - 댓글 카운트 증가/감소 `verify` 시나리오
- **정리 아이디어**
  - 공통 Given 부분(`postRepository.existsById`, `postRepository.getReferenceById`, `memberRepository.findById`)을 작은 헬퍼 메서드로 추출하면,  
    Mock 설정이 중복되는 것을 줄이고 테스트 의도를 더 잘 드러낼 수 있다.
  - 테스트 이름/DisplayName을 “규칙 설명 위주 문장”으로 유지해, 이후 리팩토링 시에도 테스트가 문서 역할을 하도록 유지.

### 4.2 통합 테스트(이미 존재하는 CommentIntegrationTest / PostIntegrationTest)와의 역할 분리

- 이미 `src/test/java/com/devon/techblog/integration/comment/CommentIntegrationTest.java`가 존재하며,
  - API 레벨에서 댓글 생성/수정/삭제, 목록/단건 조회, 검증/권한/예외까지 전체 흐름을 검증하고 있다.
- 따라서 별도의 `CommentServiceIntegrationTest`를 추가하기보다는,
  - **Service 단위 테스트(CommentServiceTest)**는 순수 비즈니스 규칙/예외/카운트 증가·감소 로직에 집중하고,
  - **통합 테스트(CommentIntegrationTest)**는 실제 Repository/트랜잭션/시큐리티와의 협력을 포함한 “엔드 투 엔드 흐름”에 집중하도록 역할을 분리한다.
- 구체적으로:
  - **CommentServiceTest**
    - 현재 구조를 유지하되, 공통 Given 헬퍼 도입 등으로 가독성을 높이고, Mock 설정을 최소화한다.
  - **PostLikeServiceTest**
    - 성공 시나리오에 대해 “예외가 발생하지 않는다”는 정도의 명시적 Assert를 추가하거나,
    - 필요 시 `postLikeRepository.save` / `deleteByPostIdAndMemberId`, `postRepository.incrementLikeCount` / `decrementLikeCount` 호출 여부를 가볍게 검증해 흐름을 문서화할 수 있다.
    - 단, 이 verify가 리팩토링을 과도하게 막지 않도록, “정말 필수적인 행위”에만 한정한다.
  - **CommentIntegrationTest / PostIntegrationTest**
    - 댓글/좋아요 카운트 증가·감소, 페이징, 권한/검증 흐름 등 실제 DB와의 협력을 포함한 대표 시나리오만 풍부하게 유지한다.

### 4.3 테스트 더블 전략과의 정렬

docs/test/TEST_DOUBLES_GUIDE.md 및 관련 문서에서 제시한 원칙과 맞추어:

- CommentServiceTest, PostLikeServiceTest는 **“서비스 로직 유닛 테스트 + Mock으로 외부 의존성 격리” 역할**을 유지한다.
- 서비스와 DB·시큐리티·정책 구현 간의 실제 상호작용은 **CommentIntegrationTest, PostIntegrationTest**에서 검증한다.
- 새로운 기능 추가 시:
  - 단위 테스트: 서비스 메서드 단위의 성공/실패 규칙, 예외 코드, 카운트 증가·감소 등 핵심 로직 검증.
  - 통합 테스트: 실제 엔드포인트/DB/보안/정책이 연결된 대표 시나리오만 골라 추가.

## 5. 요약

- **CommentServiceTest**
  - Mock을 사용하지만 과도한 행위 검증은 없고, 결과/예외 중심의 건강한 단위 테스트.
  - 현재 구조를 유지하되, 통합 테스트와 역할 분리를 명시하면 충분하다.
- **PostLikeServiceTest**
  - Mock 과사용/구현 과명세 문제는 없고, 정책/통합 테스트가 보완하고 있다.
  - 다만 성공 시나리오의 검증이 약하므로, 필요한 최소한의 Assert 또는 verify로 흐름을 문서화할 여지가 있다.
- **전체 구조**
  - 서비스 단위 테스트 + 통합 테스트(CommentIntegrationTest, PostIntegrationTest)가 함께 존재하며,
  - 테스트 더블 가이드에서 말하는 “Mock 과사용으로 인한 깨지기 쉬운 테스트” 상황은 아니다.
  - 앞으로도 Mock은 외부 의존성과 의미 있는 행위에만 제한하고, 나머지는 통합 테스트/Fake로 보완하는 전략을 유지하면 된다.
