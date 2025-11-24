# 테스트 코드 베스트 프랙티스

> 이 문서는 커뮤니티 프로젝트에서 좋은 테스트 코드를 어떻게 작성할지에 대한 기본 원칙과 실무 가이드를 정리합니다.

---

## 1. 좋은 테스트 코드의 기준: FIRST

좋은 테스트 코드는 보통 **FIRST** 원칙으로 설명할 수 있습니다.

- **F – Fast (빠르게)**
- **I – Independent/Isolated (독립적)**
- **R – Repeatable (반복 가능)**
- **S – Self-validating (자가 검증)**
- **T – Timely (적시에)**

각 항목별 설명과 예시는 아래와 같습니다.

### 1.1 Fast – 빠르게

테스트는 **자주 돌릴 수 있을 만큼 빠르게** 실행되어야 합니다. 느린 테스트는 실행 빈도가 떨어지고, 결국 신뢰도도 떨어집니다.

- 실제 DB, 외부 API, 파일 IO, 네트워크 통신 등은 테스트를 느리게 만드는 주요 원인입니다.
- 가능한 한 **순수 도메인 로직은 유닛 테스트**로 분리하여 메모리 안에서만 검증합니다.

```java
// 순수 도메인 로직만 검증하는 빠른 유닛 테스트 예시
class PostTest {

    @Test
    void increaseLikeCount() {
        // given
        Post post = new Post("제목", "내용", 0);

        // when
        post.increaseLike();

        // then
        assertThat(post.getLikeCount()).isEqualTo(1);
    }
}
```

### 1.2 Independent – 독립적

테스트는 서로 **상호 의존하지 않고 독립적으로** 실행 가능해야 합니다.

- 실행 순서에 따라 성공/실패가 달라지면 안 됩니다.
- 한 테스트에서 만든 데이터를 다른 테스트가 재사용하지 않도록 합니다.

```java
// 각 테스트는 독립적으로 Arrange → Act → Assert 수행
class PostLikeServiceTest {

    @Test
    void likePost_shouldIncreaseLikeCount() {
        // given
        Post post = new Post("제목", "내용", 0);
        PostLikeService service = new PostLikeService();

        // when
        service.like(post);

        // then
        assertThat(post.getLikeCount()).isEqualTo(1);
    }

    @Test
    void likePost_twice_shouldIncreaseLikeCountByTwo() {
        // given
        Post post = new Post("제목", "내용", 0);
        PostLikeService service = new PostLikeService();

        // when
        service.like(post);
        service.like(post);

        // then
        assertThat(post.getLikeCount()).isEqualTo(2);
    }
}
```

### 1.3 Repeatable – 반복 가능

테스트는 **어떤 환경에서 실행하더라도 항상 같은 결과**를 내야 합니다.

- 시간(`LocalDateTime.now()`), 랜덤 값, 외부 환경 변수에 직접 의존하면 재현 어려운 테스트가 됩니다.
- 시간이 필요한 경우 `Clock` 또는 `TimeProvider` 같은 추상화를 주입 받아 테스트에서 고정된 값을 사용합니다.

```java
class FixedClockTimeProvider implements TimeProvider {
    private final LocalDateTime fixedTime;

    public FixedClockTimeProvider(LocalDateTime fixedTime) {
        this.fixedTime = fixedTime;
    }

    @Override
    public LocalDateTime now() {
        return fixedTime;
    }
}

class PostCreationPolicyTest {

    @Test
    void canCreatePost_onlyWithinAllowedTimeRange() {
        // given
        LocalDateTime nineAm = LocalDateTime.of(2024, 1, 1, 9, 0);
        TimeProvider timeProvider = new FixedClockTimeProvider(nineAm);
        PostCreationPolicy policy = new PostCreationPolicy(timeProvider);

        // when
        boolean result = policy.canCreatePost();

        // then
        assertThat(result).isTrue();
    }
}
```

### 1.4 Self-validating – 자가 검증

테스트는 **사람이 로그를 눈으로 확인하지 않아도** 통과/실패가 명확해야 합니다.

- `System.out.println` 으로 확인하는 것은 테스트가 아닙니다.
- 항상 `assertThat`, `assertEquals`, `assertThrows` 등을 사용해 **기대 결과를 명시적**으로 표현합니다.

```java
@Test
void titleShouldNotBeBlank() {
    assertThatThrownBy(() -> new Post("", "내용", 0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("title");
}
```

### 1.5 Timely – 적시에

테스트는 기능을 다 만든 뒤 나중에 한꺼번에 작성하는 것이 아니라, **설계/구현과 함께** 작성하는 것이 좋습니다.

- 테스트를 먼저(또는 함께) 작성하면 **테스트하기 쉬운 구조**를 강제하게 되어 설계 품질이 좋아집니다.
- 실무에서는 모든 것을 TDD로 하기는 어렵지만, 최소한 “중요한 도메인 규칙”은 테스트를 먼저 생각하고 구현하는 흐름을 권장합니다.

---

## 2. FIRST를 넘어: 좋은 테스트 코드의 추가 특징

FIRST는 테스트의 품질을 말해주지만, **읽기 쉽고 유지보수하기 좋은 테스트**를 위해서는 추가적인 기준이 필요합니다.

### 2.1 의도가 드러나는 테스트 이름

- 테스트 이름에는 **상황(when)과 기대 결과(then)** 가 드러나도록 작성합니다.
- 예시: `increaseLikeCount_whenCalled_thenIncreaseByOne()`

```java
@Test
void increaseLikeCount_whenCalled_thenIncreaseByOne() {
    // given
    Post post = new Post("제목", "내용", 0);

    // when
    post.increaseLike();

    // then
    assertThat(post.getLikeCount()).isEqualTo(1);
}
```

### 2.2 Given–When–Then / Arrange–Act–Assert 구조

- 코드 상에서 **준비 / 실행 / 검증** 단계를 눈에 보이게 나누면 테스트의 의도가 명확해집니다.
- 주석으로 섹션을 나누거나, 빈 줄로 구분하는 방식을 사용합니다.

### 2.3 중복 제거 vs 과도한 추상화 지양

- 테스트 코드도 리팩터링 대상이며, 반복되는 준비 로직은 **테스트 데이터 빌더, 픽스처, `@BeforeEach`** 등을 활용하여 정리합니다.
- 다만, 테스트 헬퍼가 너무 복잡해지면 **테스트를 읽기 어려워**지므로, “의도를 가리는 추상화”는 피합니다.

### 2.4 경계값과 예외 상황까지 포함

- 정상 케이스 하나만 테스트하는 것은 부족합니다.
  - 최소/최대 값
  - 빈 값, null
  - 중복, 권한 없음 등의 에러 상황
- 실제로 문제가 생기기 쉬운 부분이므로 **경계값/예외 케이스 테스트를 반드시 포함**합니다.

### 2.5 테스트도 함께 리팩터링

- 프로덕션 코드 구조가 바뀌면 테스트도 함께 리팩터링하여 **의도가 잘 드러나는 상태**를 유지합니다.
- 오래된 테스트를 “깨질까봐 무서워서” 방치하지 말고, 현재 설계를 잘 설명할 수 있도록 깔끔하게 유지하는 것이 중요합니다.

---

## 3. 테스트 커버리지에 대한 생각

### 3.1 커버리지의 종류와 의미

- **Line coverage**: 코드 라인이 한 번이라도 실행되었는지
- **Branch coverage**: if/else, switch 등 분기마다 테스트가 실행되었는지

커버리지는 “코드가 실행되었는지”를 보여줄 뿐, **버그가 없는지 보장하지는 않습니다.**

### 3.2 적절한 커버리지 목표

- 전체 프로젝트 기준으로는 **70~80% 이상**을 일반적인 목표로 삼을 수 있습니다.
- 더 중요한 것은 **어디를 커버하느냐** 입니다.
  - 핵심 도메인/복잡한 비즈니스 로직: 90% 이상 목표
  - 단순 DTO, Getter/Setter, 단순 위임 로직: 과도한 커버리지 추구는 비효율적

### 3.3 커버리지의 한계와 보완

- 커버리지가 높아도 assert가 부실하면 **실제 검증은 거의 없을 수 있습니다.**
- 보완 방법:
  - 복잡한 분기가 있는 메서드는 **브랜치/조건 커버리지**를 함께 확인
  - 가능하다면 **mutation testing**(예: PIT) 도입을 고려하여, 테스트가 실제로 버그를 잡아내는지 평가

### 3.4 전략

- 도메인/비즈니스 로직, 금전/보안/권한 관련 로직 → **높은 커버리지 최우선**
- 컨트롤러, 설정, 단순 위임 코드는 **핵심 플로우를 검증하는 통합/인수 테스트**로 간접 커버해도 충분한 경우가 많습니다.

---

## 4. 유닛 테스트와 통합 테스트

### 4.1 유닛 테스트(Unit Test)

- 하나의 단위(보통 클래스/메서드)를 **외부 의존성으로부터 분리**하여 검증합니다.
- DB, 외부 API, 메시지 큐 등은 목/스텁으로 대체합니다.

**장점**
- 빠르고, 실패 시 **원인 파악이 명확**합니다.
- 외부 의존성이 없기 때문에 설계를 **낮은 결합도로 개선**하는 데 도움을 줍니다.

**적용 대상 예시**
- 도메인 모델(`@Entity` 포함)의 비즈니스 메서드
- 복잡한 규칙/계산이 들어간 서비스 로직
- 순수 유틸, Validator, Policy 객체 등

### 4.2 통합 테스트(Integration Test)

- 여러 계층/컴포넌트를 **실제 환경에 가깝게 함께** 올려서 검증합니다.
- 예: Spring 컨텍스트 로딩 + JPA 리포지토리 + 실제 DB(H2) 조합

**장점**
- 설정 오류, Bean 주입 문제, 실제 DB 쿼리 문제 등을 조기에 발견합니다.

**단점**
- 유닛 테스트보다 느리고, 실패 시 **원인 파악에 시간이 더 걸릴 수** 있습니다.

**적용 대상 예시**
- `@DataJpaTest`로 JPA 매핑 및 쿼리 검증
- `@SpringBootTest` + `TestRestTemplate`/`MockMvc` 로 실제 REST API 인수 테스트
- 트랜잭션, 보안, AOP, 설정(`application-*.yml`) 등을 실제와 유사하게 검증해야 하는 시나리오

### 4.3 어디까지, 어디에 적용할 것인가

**유닛 테스트에 집중할 부분**
- 핵심 도메인 정책, 계산/검증 로직
- 비즈니스 규칙이 많은 서비스 계층
- 외부 시스템과의 통신을 제외한 순수 로직

**통합 테스트로 다룰 부분**
- DB와 실제로 연결되는 쿼리/트랜잭션
- 컨트롤러–서비스–리포지토리 전체 플로우
- 인증/인가, 필터, AOP, 설정 등 인프라/환경 의존 로직

### 4.4 테스트 피라미드

- **바닥층: 유닛 테스트(많고 빠르게)**  
  대부분의 비즈니스 로직을 빠른 유닛 테스트로 촘촘히 검증합니다.

- **중간층: 서비스/리포지토리 통합 테스트(필요한 만큼)**  
  DB, 트랜잭션, 설정과 관련된 통합 테스트를 적절한 수준으로 유지합니다.

- **상단층: 인수/E2E 테스트(적지만 핵심 플로우)**  
  실제 사용 시나리오(주요 API 플로우)를 검증하여 시스템 전체의 신뢰도를 확보합니다.

---

## 5. 정리

- FIRST(Fast, Independent, Repeatable, Self-validating, Timely)는 좋은 테스트 코드를 판단하는 기본 기준입니다.
- 그 위에 **의도가 드러나는 이름, 명확한 Given–When–Then 구조, 경계/예외 케이스 포함, 적절한 추상화 수준**이 더해져야 실제로 유지보수 가능한 테스트가 됩니다.
- 커버리지 지표는 참고 수단일 뿐이며, 특히 **핵심 도메인/복잡한 로직을 우선적으로 높은 커버리지로 가져가는 전략**이 중요합니다.
- 유닛 테스트는 빠르고 명확한 피드백과 설계 개선에, 통합 테스트는 실제 환경에서의 정합성 검증에 초점을 둡니다.
- 이상적인 구조는 **“유닛 테스트가 많고, 통합 테스트는 핵심 시나리오 위주”**인 테스트 피라미드 형태입니다.

