# 이 프로젝트에서 Test Fixture를 적용한 방법

이 문서는 이 프로젝트에서 **Test Fixture**를 어떻게 설계하고 사용하는지 정리한 문서입니다.  
단순히 “id를 넣어주는 유틸” 수준이 아니라, **도메인/DTO 전반에 걸쳐 중복을 줄이고, 테스트 유지보수 비용을 낮추는 도구**로 사용하는 것을 목표로 합니다.

---

## 1. 처음 Test Fixture를 잘못 이해했던 점

이전 팀에서도 Test Fixture를 사용하긴 했지만, 당시에는 “id 같은 필드를 편하게 채워주는 헬퍼” 정도로만 이해하고 사용했습니다.  
그 결과로 다음과 같은 문제가 있었습니다.

- 도메인 엔티티만 부분적으로 Fixture를 사용하고, Request/Response DTO에는 매번 새로 객체를 생성했습니다.
- 동일한 엔티티/DTO를 여러 테스트에서 반복해서 생성하다 보니, 필드가 추가·변경되면 **여러 테스트 파일을 동시에 고쳐야** 했습니다.
- 테스트에서 사용하는 데이터 패턴이 제각각이라, 어떤 값이 “기본 값(default)”인지 명확하지 않았습니다.

이 프로젝트에서는 이러한 문제를 줄이기 위해 **도메인 + 애플리케이션 계층 전반에 Test Fixture를 일관되게 도입**했습니다.

---

## 2. 이 프로젝트에서의 Test Fixture 설계 원칙

이 프로젝트에서 Test Fixture는 다음과 같은 원칙을 따릅니다.

- **계층별로 Fixture를 분리합니다.**
  - 도메인 엔티티용 Fixture (`MemberFixture`, `PostFixture`, `CommentFixture`)
  - 애플리케이션 계층 DTO용 Fixture (`PostRequestFixture`, `CommentRequestFixture`, `MemberRequestFixture`)
- **“기본값 + 선택적 오버라이드” 형태로 설계합니다.**
  - 자주 사용하는 기본값을 상수로 정의하고, 필요할 때만 특정 필드만 바꿔서 사용할 수 있도록 합니다.
- **ID가 필요한 경우와 아닌 경우를 명확히 나눕니다.**
  - 순수 도메인 테스트에서는 `create(...)`로 ID 없이 생성합니다.
  - Service/Repository/통합 테스트 등 ID가 필요할 때는 `createWithId(...)`를 사용합니다.
- **불변 클래스(final) + private 생성자**로 정의합니다.
  - 상태를 가지지 않고, 정적 메서드만 제공하는 유틸리티 형태로 유지합니다.

이 원칙을 바탕으로 아래와 같이 도메인/DTO Fixture를 구성했습니다.

---

## 3. 도메인 엔티티 Fixture

### 3.1. MemberFixture

`MemberFixture`는 회원 도메인의 기본 정보를 캡슐화합니다.

- 기본 상수: `DEFAULT_EMAIL`, `DEFAULT_PASSWORD`, `DEFAULT_NICKNAME`
- 메서드:
  - `create()` : 기본 값으로 `Member.create(...)` 호출
  - `create(String email, String password, String nickname)` : 원하는 값으로 Member 생성
  - `createWithId(Long id, ...)` : `ReflectionTestUtils`를 사용해 ID 필드까지 채운 Member 생성

이 Fixture를 통해 다음과 같은 장점을 얻습니다.

- 여러 테스트에서 반복적으로 등장하는 이메일/닉네임 값을 한 곳에서 관리할 수 있습니다.
- Member 도메인에 필드가 추가되더라도, 대부분의 테스트는 Fixture만 수정하면 됩니다.

### 3.2. PostFixture

`PostFixture`는 게시글(Post) 엔티티에 대한 기본 데이터를 제공합니다.

- 기본 상수:
  - `DEFAULT_TITLE`, `DEFAULT_CONTENT`, `DEFAULT_IMAGE_URL`
  - `UPDATED_TITLE`, `UPDATED_CONTENT`
- 메서드:
  - `create(Member member)` / `create(Member member, String title, String content)`
  - `createWithId(Long id, Member member, ...)`

Post 관련 단위 테스트(`PostTest`), 서비스 테스트(`PostServiceTest`), 통합 테스트(`PostIntegrationTest`) 등에서 모두 이 Fixture를 활용하여,  
“기본 게시글”과 “수정된 게시글”에 대한 데이터 패턴을 일관되게 유지합니다.

### 3.3. CommentFixture

`CommentFixture`는 댓글(Comment) 엔티티에 대한 기본 데이터를 제공합니다.

- 기본 상수: `DEFAULT_CONTENT`, `UPDATED_CONTENT`
- 메서드:
  - `create(Member member, Post post)`
  - `createWithId(Long id, Member member, Post post, ...)`

댓글 도메인 테스트와 서비스/통합 테스트에서 동일한 기본 댓글 내용을 재사용하여,  
댓글 정책이 바뀌어도 Fixture만 수정하면 대부분의 테스트가 자동으로 따라오도록 설계했습니다.

---

## 4. 애플리케이션 계층 DTO Fixture

도메인 엔티티뿐 아니라, **Request/Response DTO에도 Fixture를 적용**했습니다.  
이를 통해 Controller, Service, 통합 테스트에서 반복되는 DTO 생성 로직을 크게 줄였습니다.

### 4.1. PostRequestFixture

`PostRequestFixture`는 게시글 생성/수정에 사용되는 요청 DTO를 생성합니다.

- 지원 타입:
  - `PostCreateRequest`
  - `PostUpdateRequest`
- 메서드 예시:
  - `createRequest()` : 기본 제목/내용으로 생성 요청 DTO 생성
  - `createRequest(String title, String content, String imageUrl)` : 특정 값으로 생성 요청 DTO 생성
  - `updateRequest()` / `updateRequest(String title, String content, String imageUrl)` : 수정 요청 DTO 생성

이 Fixture는 다음 계층에서 공통으로 사용합니다.

- `PostServiceTest` : 서비스 단위 테스트
- `PostControllerTest` : WebMvc 슬라이스 테스트
- `PostIntegrationTest` : 통합 테스트

따라서 게시글 생성/수정 API의 기본 요청 형태가 바뀌더라도, `PostRequestFixture`만 수정하면 세 계층의 테스트를 함께 정렬할 수 있습니다.

### 4.2. CommentRequestFixture

`CommentRequestFixture`는 댓글 생성/수정 요청 DTO를 생성합니다.

- `CommentCreateRequest`, `CommentUpdateRequest`에 대해
  - 기본 내용으로 생성/수정 요청을 만드는 정적 메서드를 제공합니다.

`CommentServiceTest`, `CommentControllerTest`, `CommentIntegrationTest`에서 동일한 Fixture를 사용하여,  
댓글 요청 형식 변경 시 테스트 수정 범위를 최소화합니다.

### 4.3. MemberRequestFixture

`MemberRequestFixture`는 회원 정보 수정 요청 DTO(`MemberUpdateRequest`)를 생성합니다.

- 기본 상수: `DEFAULT_NEW_NICKNAME`, `DEFAULT_NEW_PROFILE_IMAGE`
- 메서드:
  - `updateRequest()` : 기본 값으로 수정 요청 DTO 생성
  - `updateRequest(String nickname, String profileImageUrl)` : 특정 값으로 수정 요청 DTO 생성

회원 프로필 수정 관련 테스트에서 반복되는 요청 생성 코드를 제거하고,  
닉네임/프로필 이미지 변경 시나리오를 일관되게 표현할 수 있습니다.

---

## 5. Test Fixture가 가져온 효과

이 프로젝트에서 Test Fixture를 적극적으로 도입하면서 다음과 같은 효과를 얻었습니다.

- **중복 코드를 크게 줄였습니다.**
  - 도메인 엔티티와 Request DTO를 생성하는 코드가 Fixture로 모이면서, 테스트 본문은 “무엇을 검증하는지”에 집중할 수 있게 되었습니다.
- **도메인/DTO 변경에 대한 부담을 줄였습니다.**
  - 필드 추가/변경 시 Fixture만 수정하면 되는 경우가 많아, 테스트 수정 범위가 작아졌습니다.
- **테스트 데이터의 일관성이 높아졌습니다.**
  - “기본 회원”, “기본 게시글”, “기본 댓글” 등 공통 패턴이 Fixture 상수로 명확히 정의되어, 테스트를 읽을 때 의도가 더 잘 드러납니다.
- **여러 계층에서 동일한 시나리오를 공유할 수 있게 되었습니다.**
  - 같은 `PostRequestFixture`를 Service, Controller, 통합 테스트에서 사용함으로써,  
    계층별 테스트가 서로 다른 데이터를 쓰는 문제를 줄이고, 시나리오를 수평으로 맞출 수 있습니다.

---

## 6. 앞으로의 개선 방향

현재 Fixture 구조는 도메인/애플리케이션 계층에 대해 기본적인 재사용 패턴을 제공하는 수준입니다.  
앞으로는 다음과 같은 방향으로 확장을 고려할 수 있습니다.

- **더 복잡한 시나리오용 Fixture 메서드 추가**
  - 예: “좋아요가 이미 눌린 게시글”, “여러 댓글이 달린 게시글” 등 상황별 조합 Fixture
- **테스트 데이터 빌더 패턴 도입 여부 검토**
  - 현재는 정적 메서드 위주이지만, 가독성을 위해 빌더 스타일(`PostTestDataBuilder`)을 도입할지 검토할 수 있습니다.
- **Fixture와 Given-When-Then 스타일의 결합**
  - Fixture를 “Given” 단계의 표준 도구로 삼고, 테스트 이름/구조와 함께 일관된 패턴을 만드는 방향입니다.

요약하면, 이 프로젝트의 Test Fixture는  
**“테스트 데이터를 한 곳으로 모으고, 계층 간에 공유하며, 도메인/DTO 변경에 대한 비용을 줄이는 도구”**로 설계되어 있습니다.

