# Spring Boot 3.4 컨트롤러 테스트 통합 가이드

본 문서는 Spring Boot 3.4 환경에서의 **컨트롤러 테스트 전략**을 통합 정리한 것이다.  
기존 `@WebMvcTest` + `@MockBean` 기반 방식이 가지는 한계와, Spring Boot 3.4가 권장하는 **단위 테스트·슬라이스 테스트·통합 테스트 전략**을 모두 아우르는 실천적인 가이드를 제공한다.

---

## 1. Spring Boot 3.4 이후 테스트 환경 변화

Spring Boot 3.4는 테스트 생태계 전반에 다음과 같은 변화를 도입하였다.

- `@MockBean`이 **완전히 제거된 것은 아니지만**, AOT 환경 및 Native Image 대응 문제로 인해 **일부 상황에서 정상적으로 동작하지 않을 수 있음**
- 특히 `@WebMvcTest`와 결합할 경우 Proxy 생성 문제, Context 초기화 오류 등이 발생 가능
- Spring 팀은 `@MockBean` 기반 테스트보다는 **명시적 Stub Bean 등록**이나 **순수 단위 테스트 중심 구조**를 권장

따라서 기존처럼 아무 생각 없이 `@WebMvcTest` + `@MockBean` 조합을 쓰는 것은 더 이상 최선이 아니며,  
테스트의 목적과 범위에 맞는 새로운 전략이 필요하다.

---

## 2. 컨트롤러 테스트 관점에서의 큰 방향

컨트롤러 테스트는 크게 세 가지 관점으로 나눌 수 있다.

1. **컨트롤러 내부 로직만 빠르게 검증**하고 싶은 경우 → 단위 테스트(Standalone MockMvc)
2. **Spring MVC 인프라(바인딩/검증/예외 처리 등)를 실제와 유사하게 검증**하고 싶은 경우  
   → 슬라이스 테스트(`@WebMvcTest`) 또는 통합 테스트(`@SpringBootTest` + `@AutoConfigureMockMvc`)
3. **보안/필터/실제 Bean 구성까지 모두 포함한 흐름을 검증**하고 싶은 경우  
   → 통합 테스트(`@SpringBootTest` + `@AutoConfigureMockMvc`)

각 방식은 서로 대체재가 아니라, 테스트 목적에 따라 **서로 보완적으로 사용**해야 한다.

---

## 3. 주요 테스트 방식 비교

### 3.1 Standalone MockMvc 기반 단위 테스트

```java
MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller)
    .setCustomArgumentResolvers(...)
    .setControllerAdvice(...)
    .build();
```

**특징**

- Spring Context 로딩 없음 (순수 단위 테스트)
- Controller 객체만 로딩해 매우 빠르게 테스트 가능
- `HandlerMethodArgumentResolver`, `ControllerAdvice`, `MessageConverter` 등을 **직접 설정해야 함**

**장점**

- 가장 빠르고 가볍다.
- Spring 빈에 의존적인 구조에서 벗어나, 컨트롤러 로직 자체를 순수하게 검증 가능하다.
- Security filter chain, 복잡한 의존성으로부터 독립적이다.

**단점**

- Spring MVC 기반의 실제 동작(`@Valid`, `@ControllerAdvice`, JSON 직렬화/역직렬화 등)을 완전히 보장하지는 않는다.
- 운영 환경과 동일한 빈 구성을 검증하는 용도에는 적합하지 않다.

**언제 쓰는가**

- 복잡한 비즈니스 분기 로직을 컨트롤러 레벨에서 검증하고 싶을 때
- Service 협력 여부(호출 횟수, 파라미터)를 확인하고 싶을 때
- Security / Infrastructure에 의존하지 않는 단순 API일 때

---

### 3.2 @WebMvcTest 기반 MVC 슬라이스 테스트

```java
@WebMvcTest(MemberController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestStubConfig.class)
class MemberControllerSliceTest {
    // ...
}
```

**특징**

- Spring MVC 인프라(`MessageConverter`, Validation, `@ControllerAdvice` 등)를 실제와 유사하게 로딩한다.
- 기본적으로 WebMvc 관련 빈들만 로딩하며, 전체 애플리케이션 컨텍스트보다 가볍다.
- `@MockBean` 대신 **테스트용 Stub/Fake Bean을 명시적으로 등록**하는 방식이 권장된다.

**장점**

- 실제 Spring MVC 환경과 가장 유사한 테스트 작성이 가능하다.
- 매핑/바인딩/직렬화/예외 처리/유효성 검증 등을 자연스럽게 검증할 수 있다.

**단점**

- Standalone 대비 느리다.
- 잘못 사용하면 Security auto configuration까지 함께 올라가, 필터 체인 의존성 때문에 테스트가 불안정해질 수 있다.
- `@MockBean`을 남용하면 컨텍스트 초기화 비용 증가 및 예측 불가능한 부작용이 발생할 수 있다.

**언제 쓰는가**

- Request/Response 바인딩, JSON 직렬화/역직렬화, Validation, 예외 처리 흐름을 실제와 가깝게 검증하고 싶을 때
- 특정 컨트롤러(또는 소수의 컨트롤러)에 국한된 WebMvc 슬라이스를 테스트하고 싶을 때

---

### 3.3 @SpringBootTest + @AutoConfigureMockMvc 기반 통합 테스트

```java
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class MemberControllerIntegrationTest {

    @TestConfiguration
    static class StubConfig {
        @Bean
        MemberService memberService() {
            return Mockito.mock(MemberService.class);
        }
    }

    @Autowired
    MockMvc mockMvc;
}
```

**특징**

- 실제 애플리케이션에 가까운 전체 컨텍스트를 로딩한다.
- Security filter chain, DB, 메시지 브로커 등의 인프라도 함께 구성 가능하다(설정에 따라 선택).
- 테스트용 Stub/Fake Bean을 `@TestConfiguration`이나 테스트 전용 Config로 **명시적으로 등록**하는 패턴을 사용한다.

**장점**

- 운영 환경에 가장 가까운 형태로 전체 흐름을 검증할 수 있다.
- Controller ↔ Service ↔ Repository ↔ 외부 시스템까지 이어지는 end-to-end 시나리오 검증에 유리하다.

**단점**

- 가장 무겁고 느리다.
- 테스트 간 컨텍스트 캐시를 공유하지만, 부적절한 Bean 정의 변경은 캐시 무효화를 유발해 속도가 더 떨어질 수 있다.

**언제 쓰는가**

- 핵심 비즈니스 플로우를 통합적으로 검증하고 싶을 때
- Security/Filter/Interceptor 등 운영 환경과 동일한 흐름을 검증해야 할 때

---

## 4. @MockBean 사용을 지양하는 이유

Spring Boot 3.4 이상에서는 다음과 같은 이유로 `@MockBean` 의존을 줄이는 것이 좋다.

- `@MockBean`은 스프링 컨텍스트에 Fake 빈을 **덮어씌우는 방식**이라, 테스트마다 컨텍스트 재초기화를 유발하여 느리고 무겁다.
- AOT 및 Native Image 환경에서 Proxy 생성 실패, 빈 주입 순서 오류 등 예측하기 어려운 문제가 발생할 수 있다.
- Security filter chain처럼 복잡한 빈 구조에서 일부를 `@MockBean`으로 바꾸면, 다른 빈들이 함께 깨지는 등 부작용이 크다.
- 실제 운영과 다른 빈 구성이 만들어져, 테스트가 통과하는데 운영에서는 실패하는 상황이 생기기 쉽다.

따라서 다음과 같은 대안이 필요하다.

- **테스트 전용 Configuration**을 만들어 Stub/Fake Bean을 명시적으로 등록한다.
- 컨트롤러 단위의 테스트는 Standalone MockMvc로 분리하여, 스프링 컨텍스트와 무관하게 검증한다.
- 공통 테스트 인프라는 `java-test-fixtures` 등에 모아 재사용한다.

---

## 5. Security 및 의존성 최소화 전략

컨트롤러 테스트에서 가장 자주 부딪히는 문제는 **Security auto configuration**과의 충돌이다.

### 5.1 Security 빈이 테스트를 깨뜨리는 이유

- `@WebMvcTest`를 아무 설정 없이 사용하면 Spring Boot가 Security auto configuration까지 읽어, 필터 체인에 등록된 모든 보안 빈을 찾으려 한다.
- Custom 필터/LogoutHandler/JWT Provider 등 우리가 컨트롤러 테스트에서 굳이 필요하지 않은 빈까지 요구된다.
- 그 결과, 특정 Security Bean이 누락되거나 Mock으로 대체되면서 컨텍스트 초기화 실패·테스트 속도 저하가 발생한다.

### 5.2 Security 의존성 줄이기

1. **Security 필터 비활성화 또는 최소화**
   - `@AutoConfigureMockMvc(addFilters = false)`로 테스트 시 Security filter chain을 끈다.
   - 혹은 테스트 전용 `SecurityFilterChain` 빈을 정의하여, 모든 요청을 `permitAll`로 열어둔 단순 구성을 사용한다.

2. **MockMvc Standalone 사용**
   - `MockMvcBuilders.standaloneSetup(controller)`로 컨트롤러만 올린다.
   - 이 경우 Security filter chain은 아예 올라오지 않으므로, Security 의존성에서 완전히 자유롭다.

3. **SecurityMockMvcRequestPostProcessors 활용**
   - Spring Security의 `with(user("tester").roles("USER"))` 등의 도우미를 사용해 인증 컨텍스트를 흉내낸다.
   - 실제 필터 체인을 다 올리지 않고도, 인증/인가 관련 로직을 일정 수준까지 검증할 수 있다.

4. **테스트 전용 ArgumentResolver / Fake CurrentUser**
   - `@CurrentUser`와 같은 커스텀 어노테이션이 있다면, 테스트에서는 이를 해석하는 `HandlerMethodArgumentResolver`를 Fake로 등록한다.
   - Standalone MockMvc에서는 `setCustomArgumentResolvers`, `@WebMvcTest`/`@SpringBootTest`에서는 테스트 전용 Config를 통해 등록한다.

---

## 6. 테스트 코드 작성 컨벤션 (Controller 중심)

### 6.1 기본 원칙

1. **기본은 Standalone MockMvc**를 우선 고려한다.
   - 컨트롤러 내부 로직, Service 호출 여부, 응답 형태 등을 빠르고 독립적으로 검증한다.
2. **WebMvc 슬라이스/통합 테스트는 “인프라 검증”용**으로 사용한다.
   - Request/Response 바인딩, Validation, 예외 처리, Security 등 인프라 레벨을 함께 검증해야 할 때 선택한다.
3. **`@MockBean`은 가능하면 사용하지 않는다.**
   - 대신 테스트 전용 Config에서 Stub/Fake Bean을 정의하고 `@Import` 혹은 `@TestConfiguration`으로 주입한다.

---

### 6.2 Standalone 컨트롤러 테스트 패턴

- 컨트롤러는 테스트 대상이므로, Service는 `@Mock`으로 두고 `@InjectMocks` 또는 생성자 주입으로 묶는다.
- `MockMvc`는 `MockMvcBuilders.standaloneSetup(controller)`로 생성한다.
- 필요한 경우:
  - 커스텀 `HandlerMethodArgumentResolver`를 직접 구현하여 등록
  - `@RestControllerAdvice`에 해당하는 예외 처리기를 직접 등록

예시:

```java
@ExtendWith(MockitoExtension.class)
class MemberControllerTest {

    @InjectMocks
    private MemberController memberController;

    @Mock
    private MemberService memberService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(memberController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }
}
```

---

### 6.3 @WebMvcTest 슬라이스 테스트 패턴

- 여러 컨트롤러 테스트에서 공통으로 사용하는 Resolver, Advice, Fake Security 설정 등은 **테스트 전용 Config** 또는 **java-test-fixtures**로 분리해 재사용한다.
- `@AutoConfigureMockMvc(addFilters = false)`로 Security 필터를 끄거나, 테스트용 Security 구성을 명시적으로 등록한다.
- 반드시 필요한 경우에만 `@WebMvcTest`를 사용하고, 대상 컨트롤러를 명시한다.

예시:

```java
@WebMvcTest(MemberController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({TestMvcConfig.class, TestStubConfig.class})
class MemberControllerSliceTest {

    @Autowired
    MockMvc mockMvc;
}
```

`TestMvcConfig` 예시(개념):

- 공통 `ControllerAdvice`
- 공통 `HandlerMethodArgumentResolver`
- 필요 시 Jackson 설정

`TestStubConfig` 예시(개념):

- `MemberService` 등 도메인 Service에 대한 Stub/Fake Bean 정의

---

### 6.4 @SpringBootTest 통합 테스트 패턴

- 애플리케이션 전반의 흐름(보안, 인프라 포함)을 검증해야 하는 핵심 시나리오에 한해 사용한다.
- 외부 시스템(DB, 메시지 브로커, 외부 API 등)은 Testcontainer 또는 Fake Adapter로 대체할 수 있다.
- `@TestConfiguration` 또는 별도 테스트 전용 Config로 Stub/Fake Bean을 등록한다.

예시:

```java
@SpringBootTest
@AutoConfigureMockMvc
class MemberControllerIntegrationTest {

    @TestConfiguration
    static class StubConfig {
        @Bean
        MemberService memberService() {
            return Mockito.mock(MemberService.class);
        }
    }
}
```

---

## 7. API 응답 및 검증 컨벤션

컨트롤러 테스트에서는 **API 응답 형식**을 항상 일관되게 검증해야 한다.

- 공통 응답 래퍼(`ApiResponse.success(...)`, `ApiResponse.error(...)` 등)가 있다면,
  - `jsonPath("$.success").value(true)`  
  - `jsonPath("$.data.id").value(...)`  
  - `jsonPath("$.error.code").value("...")`  
  등으로 필수 필드를 검증한다.
- Validation 실패, 인증 실패, 권한 부족 등의 예외 응답도 **예상된 코드/메시지 구조**를 검증한다.

---

## 8. 요약: 우리가 지켜야 할 컨트롤러 테스트 전략

- **단위 테스트(Standalone MockMvc)** 를 기본으로 한다.
  - 빠르고 독립적인 컨트롤러 검증에 초점을 맞춘다.
- **슬라이스 테스트(@WebMvcTest)** 는 MVC 인프라(바인딩/Validation/Advice)를 검증해야 할 때만 사용한다.
  - `@AutoConfigureMockMvc(addFilters = false)` 또는 테스트용 Security Config로 Security 의존성을 최소화한다.
- **통합 테스트(@SpringBootTest + @AutoConfigureMockMvc)** 는 실제 운영에 가까운 전체 흐름을 검증하는 데만 사용한다.
- **`@MockBean` 남발을 지양**하고, 테스트 전용 Config/Fixture/Stub Bean을 활용한다.
- 공통 테스트 인프라(Resolver, Advice, Fake Security 등)는 **java-test-fixtures**나 전용 패키지로 추출하여 재사용한다.

이 가이드를 기준으로 컨트롤러 테스트를 구성하면, Spring Boot 3.4 환경에서도  
테스트가 빠르면서도 안정적이고, 향후 프레임워크 변화에도 유연하게 대응할 수 있다.

