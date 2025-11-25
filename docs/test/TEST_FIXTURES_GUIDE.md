# 테스트 픽스처 가이드 – 왜, 어떻게, 무엇이 달라졌는가

> 이 문서는 `docs/test` 아래 정리된 테스트 전략/의존성 관리 문서  
> (특히 TOSS Gradle 테스트 의존성 글 요약, 카카오페이 Given 글 Part 2, 프로젝트 테스트 전략, 컨트롤러 테스트 가이드, 테스트 더블 가이드, 테스트 베스트 프랙티스)를 바탕으로  
> 우리 프로젝트에서 **테스트 픽스처를 왜/어떻게 사용할지**를 정리한 가이드입니다.

---

## 1. 테스트 픽스처를 왜 사용하는가 (도입 배경)

### 1.1 Given 지옥, 중복, 의존성 지옥

여러 문서에서 공통으로 지적하는 문제는 다음과 같습니다.

- **Given 절이 너무 길고 복잡**하다.  
  - 엔티티/DTO 필드가 많아 테스트마다 모든 필드를 채워야 한다.
  - 외부 인프라 Request/Response 생성 코드가 테스트 본문을 가린다.
- **멀티 모듈 환경에서 Given/헬퍼/픽스처가 복붙**된다.  
  - 각 모듈 `src/test`는 서로 의존할 수 없어, 동일한 헬퍼를 모듈마다 다시 작성한다.
- **테스트 전용 의존성(H2 등) 관리가 꼬인다.**  
  - 하위 모듈에만 추가한 테스트 의존성이 상위 모듈 테스트에는 전파되지 않는다.
  - 어디서 무엇을 의존하는지 파악하기 어렵고, 환경을 옮길 때마다 깨진다.

이 결과:

- 테스트가 **쓰기도, 읽기도, 유지보수하기도** 어려워지고
- 테스트 자체가 **기술 부채**가 되는 상황이 발생한다.

### 1.2 테스트도 “구조적으로 관리”해야 한다

TOSS Gradle 요약, 카카오페이 Given 글 요약, PROJECT_TEST_STRATEGY.md는 공통으로 다음을 강조한다.

- 테스트 코드도 소프트웨어다.  
  - **Builder, Helper, Test Double, 테스트 전용 의존성** 자체가 관리 대상이다.
- “필요할 때 그때그때 복붙”이 아니라  
  - **픽스처(재사용 가능한 테스트 자원)**를 구조적으로 정의하고
  - 모듈/레이어를 넘어 재사용해야 한다.

이를 위해 등장하는 핵심 도구가 바로 **`java-test-fixtures` 기반의 테스트 픽스처**다.

---

## 2. 테스트 픽스처를 어떻게 사용하는가

여기서 말하는 테스트 픽스처는 크게 두 가지를 포괄한다.

- **테스트 데이터/헬퍼/빌더**: `given*` 함수, 엔티티/DTO 빌더, Request/Response 생성기 등
- **테스트 인프라/환경 구성 요소**: Fake Repository, Fake Security, 공통 Resolver/Advice, H2 등 테스트 전용 의존성

### 2.1 Gradle `java-test-fixtures`로 픽스처를 구조화

TOSS Gradle 요약, 카카오페이 Given 글, PROJECT_TEST_STRATEGY.md, TEST_DOUBLES_GUIDE.md는  
멀티 모듈/레이어 환경에서 **픽스처를 공유하는 표준 방법**으로 `java-test-fixtures`를 제안한다.

핵심 아이디어:

1. **테스트 전용 소스셋 분리**
   - 각 모듈에 `java-test-fixtures` 플러그인을 적용한다.
   - 테스트용 Builder/Helper/Fake/Stub 등을 `src/testFixtures` 아래로 이동한다.
   - 빌드 시 `test-fixtures.jar`가 생성되고, 이 안에 **테스트 전용 코드만** 담긴다.
2. **다른 모듈에서 픽스처 의존**
   - `testImplementation(testFixtures(project(":domain")))` 처럼 선언하여
   - 도메인 모듈의 픽스처를 서비스/웹/배치 모듈 테스트에서 재사용한다.
3. **테스트 전용 의존성 전파**
   - H2 같은 테스트 전용 의존성을 `testFixturesRuntimeOnly`로 지정하고  
     상위 모듈에서 `testRuntimeOnly(testFixtures(project(":db")))`로 가져온다.
   - 이렇게 하면 상위 모듈은 “어떤 DB를 쓰는지”를 구체적으로 알 필요 없이  
     동일한 테스트 환경을 누릴 수 있다.

이 패턴의 효과:

- **동일한 픽스처 코드 복붙 제거**  
  - 한 곳에서만 관리하고, 모든 모듈이 재사용한다.
- **테스트 전용 코드/의존성이 main과 분리**  
  - 프로덕션 JAR에는 포함되지 않고, 오직 테스트에서만 사용된다.
- **의존성 전파 경로가 명시적**  
  - “어디에서 무엇을 테스트용으로 제공하는지”를 Gradle에서 드러낼 수 있다.

### 2.2 Given 헬퍼/테스트 데이터 빌더로 테스트를 단순하게

카카오페이 Given 글, TEST_BEST_PRACTICES.md, PROJECT_TEST_STRATEGY.md는  
**Given 절을 단순하게 만드는 도구**로 테스트 데이터 빌더/헬퍼를 강조한다.

패턴:

- 필수가 아닌 필드는 기본값으로 채우고,  
  테스트에서 정말 **차이를 만들고 싶은 필드만 인자로 받는** `given*` 함수를 만든다.
- 예: `givenProductHistory(시작일, 종료일)` 만 넣으면  
  나머지 필드는 픽스처에서 알아서 넣어준다.
- 복잡한 Request/Response, 외부 인프라용 DTO도  
  `givenXXXRequest(도메인 객체, 추가 옵션)` 같은 헬퍼로 감싸, 테스트 본문에서 사라지게 한다.

이 헬퍼/빌더들을 `src/testFixtures`로 모으면:

- 도메인, 서비스, API, 배치 등 **여러 레이어에서 동일한 Given 표현**을 쓸 수 있고
- 필드가 변경되더라도 픽스처 한 곳만 수정하면 된다.

### 2.3 테스트 인프라 픽스처: Fake/Stub/Resolver/Config

SPRINGBOOT34_CONTROLLER_TEST_GUIDE.md, TEST_DOUBLES_GUIDE.md는  
다음과 같은 **인프라 레벨 픽스처**를 제안한다.

- 컨트롤러 테스트용 공통 설정
  - 글로벌 `@RestControllerAdvice`
  - 공통 `HandlerMethodArgumentResolver` (`@CurrentUser` 등)
  - Fake Security 설정 / 테스트용 `SecurityFilterChain`
- 인메모리 Fake/Stub 구현
  - `InMemoryMemberRepository`, Fake 외부 API 클라이언트 등
- 테스트 전용 Configuration
  - `@TestConfiguration` + Stub/Fake Bean

이런 공통 테스트 인프라도 `java-test-fixtures`나 전용 테스트 패키지로 뽑아  
여러 테스트에서 재사용하도록 권장하고 있다.

---

## 3. 우리 프로젝트에서의 테스트 픽스처 설계 원칙

위 문서들에서 뽑은 내용을 바탕으로, 우리 프로젝트 관점에서 정리하면 다음과 같다.

### 3.1 어떤 것들을 픽스처로 빼야 하는가

- **도메인/엔티티/VO 생성 헬퍼**
  - 예: `givenMember`, `givenPost`, `givenComment`, `givenLikeHistory` 등
  - 기본적으로 “유효한 객체”를 만들어 주고, 테스트에서 필요한 필드만 지정.
- **복잡한 Request/Response/DTO 생성 헬퍼**
  - 예: `givenCreatePostRequest`, `givenOAuthLoginRequest`, `givenNotificationRequest` 등
  - 외부 인프라와의 필드 매핑을 숨기고, 테스트는 핵심 의도만 표현.
- **공통 테스트 인프라**
  - 인메모리 Fake Repository/Client
  - 테스트용 ArgumentResolver (`@CurrentUser` 등)
  - 테스트 전용 Security Config, AOP/Logging 비활성화 설정 등
- **테스트 전용 의존성 구성**
  - H2, Testcontainers, WireMock 등

위 항목들은 **테스트에서 반복적으로 등장하지만, 테스트의 핵심 관심사는 아닌 준비 코드**이므로  
픽스처로 묶는 것이 적합하다.

### 3.2 어디에 두고 어떻게 나눌 것인가

기본 원칙:

- 하나의 모듈/레이어 안에서만 쓰는 것 → 해당 모듈의 `src/testFixtures`  
- 여러 모듈/레이어에서 공유하는 것 → 공용 모듈의 `src/testFixtures` (예: `domain` 픽스처를 다른 모듈이 사용)

예시 구조 (개념):

- `domain/src/testFixtures/java/.../fixture/DomainFixtures`  
  - 도메인 엔티티/값 객체 생성 헬퍼
- `application/src/testFixtures/java/.../fixture/ApplicationFixtures`  
  - 서비스/유즈케이스 레벨의 복합 시나리오 헬퍼
- `api/src/testFixtures/java/.../fixture/ApiFixtures`  
  - 컨트롤러 Request/Response, MockMvc 설정, 공통 Resolver/Advice 등

Gradle에서는:

- `domain` 모듈에 `java-test-fixtures` 적용
- `api`/`application` 모듈에서 `testImplementation(testFixtures(project(":domain")))` 선언

### 3.3 픽스처 작성 시 유의점

- **가독성이 최우선**  
  - 헬퍼/빌더가 너무 추상적이면 오히려 테스트를 읽기 어렵게 만든다.
  - “테스트의 의도가 더 잘 보이도록” 만드는 수준까지만 추상화한다.
- **비즈니스 규칙은 픽스처가 아니라 도메인에서 검증**  
  - 픽스처 안에서 복잡한 로직을 넣기 시작하면, 픽스처가 또 하나의 도메인이 된다.
  - 픽스처는 “유효한 기본값 + 테스트 편의” 정도로만 사용한다.
- **테스트 더블과 픽스처 구분**  
  - Stub/Fake/Mock은 행위/의존성 관리에 대한 개념이고,
  - 픽스처는 “반복되는 준비 상태/데이터/환경”을 캡슐화하는 도구라고 생각하면 이해가 쉽다.
  - 두 개념은 섞어 쓰되, 테스트에서 어떤 역할을 하는지 명확히 인식하고 사용한다.

---

## 4. 테스트 픽스처 도입으로 얻는 효과

### 4.1 테스트 작성/추가 비용 감소

- Given 절을 한 줄, 두 줄로 줄일 수 있어  
  새 테스트 케이스를 추가하는 비용이 크게 줄어든다.
- **“테스트 쓰기 귀찮아서 최소 케이스만 남기는”** 상황을 완화한다.
- PROJECT_TEST_STRATEGY.md, KAKAOPAY_GIVEN_TEST_CODE2_SUMMARY.md가 말하듯  
  작은 헬퍼 하나, fixture 모듈 하나에서 시작한 스노우볼이  
  장기적으로 테스트 생태계를 개선한다.

### 4.2 중복 감소와 일관성 향상

- 동일한 도메인 객체를 여러 모듈에서 테스트할 때  
  **항상 같은 기본값/패턴**으로 생성하게 되어, 테스트 간 일관성이 확보된다.
- 필드가 추가되거나 정책이 바뀌어도  
  픽스처 한 곳만 수정하면 연관된 테스트들이 한 번에 맞춰진다.

### 4.3 테스트 의도 가시성 향상

- TEST_BEST_PRACTICES.md의 FIRST 원칙 중  
  특히 **Self-validating, Readability** 측면이 크게 좋아진다.
- 테스트 본문이 “무슨 데이터를 어떻게 만드는지”가 아니라  
  **“어떤 상황에서 어떤 결과를 기대하는지”**에 집중할 수 있다.
- 컨트롤러 테스트에서는 Request/Response/시큐리티 준비 코드가 사라지고  
  실제 검증하고 싶은 API 동작에만 집중할 수 있다.

### 4.4 설계 피드백 루프 강화

- Given 절이 길어지거나 픽스처가 과하게 복잡해지면  
  “테스트 도구 부족 + 설계 개선 필요” 신호로 볼 수 있다.
- 도메인/서비스/컨트롤러 설계를 바꿔 테스트하기 쉽게 만들고,  
  픽스처는 그 설계를 더 잘 활용하도록 돕는 방향으로 발전한다.
- 이는 **테스트가 설계를 끌어올린다**는 프로젝트 방향성과 일치한다.

---

## 5. 도입 로드맵 (실행 순서 제안)

1. **공통 도메인 픽스처부터 시작**
   - 가장 많이 쓰이는 엔티티/값 객체에 대한 `given*` 헬퍼를 만들고,  
     `src/testFixtures`로 이동한다.
2. **Gradle에 `java-test-fixtures` 적용**
   - 공통 모듈(예: `domain`)에 플러그인 추가 후,  
     다른 모듈 테스트에서 `testImplementation(testFixtures(project(":domain")))`로 의존.
3. **컨트롤러 테스트 인프라 픽스처화**
   - 공통 Resolver/Advice, Fake Security, MockMvc 설정을 테스트 전용 Config나  
     testFixtures로 옮기고, 컨트롤러 테스트에서 재사용한다.
4. **테스트 전용 의존성 정리**
   - H2, Testcontainers, WireMock 등 테스트용 의존성을  
     `testFixturesRuntimeOnly` + `testRuntimeOnly(testFixtures(project(":module")))` 패턴으로 정리.
5. **기존 Given 절 리팩터링**
   - 자주 수정되는 테스트부터 차근차근 픽스처/헬퍼를 적용해  
     가독성과 유지보수성을 개선한다.

---

## 6. 마무리

테스트 픽스처는 단순히 “공통 코드를 뽑아낸 유틸”이 아니라,

- **테스트 데이터를 일관되고 이해하기 쉬운 방식으로 표현**하고
- **멀티 모듈/레이어를 가로질러 테스트 자원을 공유**하며
- **테스트 의존성과 인프라를 구조적으로 관리**하기 위한 핵심 도구다.

우리 프로젝트에서는 `java-test-fixtures`와 테스트 데이터 빌더/헬퍼,  
공통 테스트 인프라 픽스처를 함께 활용해,

- 테스트가 빠르고 읽기 좋으며
- 설계 개선에 도움이 되는 방향으로

테스트 생태계를 꾸준히 다듬어 나간다.

