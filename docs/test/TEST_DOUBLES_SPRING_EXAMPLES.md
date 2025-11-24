# Spring Test에서의 테스트 더블 실무 사용 예시

> 이 문서는 `TEST_DOUBLES_GUIDE.md`에서 정리한 개념을, Spring + JUnit 5 + Mockito 조합에서 실제로 어떻게 사용하는지 예시 위주로 정리한 것입니다.

---

## 공통 세팅: JUnit 5 + Mockito

### 1) 순수 유닛 테스트 (스프링 컨텍스트 없음)

```java
@ExtendWith(MockitoExtension.class)
class ExampleServiceTest {

    @Mock
    private Dependency dependency;   // Mock

    @InjectMocks
    private ExampleService exampleService; // 테스트 대상

    @Test
    void example() {
        given(dependency.call()).willReturn("value");

        String result = exampleService.doSomething();

        assertThat(result).isEqualTo("value");
        verify(dependency).call();
    }
}
```

- 특징: 스프링 컨텍스트를 띄우지 않고, 순수 자바 객체만 테스트.
- 어디에 사용? 서비스/도메인 유닛 테스트에 가장 적합.

### 2) Spring Test 슬라이스에서 Mockito 사용

```java
@WebMvcTest(SomeController.class)
@AutoConfigureMockMvc(addFilters = false)
class SomeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SomeService someService; // Spring 컨텍스트에 Mock을 등록

    @Test
    void getExample() throws Exception {
        given(someService.getData()).willReturn("hello");

        mockMvc.perform(get("/example"))
               .andExpect(status().isOk())
               .andExpect(content().string("hello"));
    }
}
```

- 특징: WebMvc 슬라이스만 로딩, Service는 `@MockBean`으로 대체.
- 어디에 사용? 컨트롤러 레벨 테스트(HTTP ↔ 서비스 경계)에서 많이 사용.

---

## 1. Dummy 사용 예시

실제 테스트에서는 Dummy를 많이 드러내놓고 쓰기보다는, “이 인자는 이번 테스트에서 쓰이지 않는다”는 사실을 드러낼 때 사용합니다.

```java
class EmailService {
    void send(String message, String address, EmailClient client) {
        if (message == null || message.isBlank()) return;
        client.send(address, message);
    }
}

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    EmailClient emailClient; // 실제로는 Dummy처럼 취급

    @Test
    void messageIsBlank_doNothing() {
        EmailService service = new EmailService();

        service.send(" ", "user@test.com", emailClient);

        // 어떤 호출도 일어나지 않음 → dummy client
        verifyNoInteractions(emailClient);
    }
}
```

- Dummy는 “테스트의 관심사가 아님”을 표현하는 수단으로 활용.

---

## 2. Stub 사용 예시

### 2-1) 인터페이스 직접 구현 (Fake에 가까운 Stub)

```java
interface RateClient {
    int getRate();
}

class OrderService {
    private final RateClient client;
    OrderService(RateClient client) { this.client = client; }

    int discounted(int price) {
        return price - price * client.getRate() / 100;
    }
}

@Test
void discounted_withStubClient() {
    RateClient stubClient = () -> 20; // 항상 20% 반환
    OrderService service = new OrderService(stubClient);

    int result = service.discounted(10000);

    assertThat(result).isEqualTo(8000);
}
```

### 2-2) Mockito로 Stub 구성

```java
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    RateClient rateClient; // Stub으로 활용

    @InjectMocks
    OrderService orderService;

    @Test
    void discounted_withMockitoStub() {
        given(rateClient.getRate()).willReturn(15);

        int result = orderService.discounted(10000);

        assertThat(result).isEqualTo(8500);
        // 호출 횟수에는 관심 없으므로 별도 verify 없음
    }
}
```

- 실무에서는 인터페이스가 있을 때 “간단한 Stub 구현”을 만들어두거나, Mockito의 `given()` 을 이용해 Stub처럼 사용합니다.

---

## 3. Fake 사용 예시 (인메모리 구현)

### 3-1) Repository Fake

```java
interface MemberRepository {
    Member save(Member member);
    Optional<Member> findByEmail(String email);
}

class InMemoryMemberRepository implements MemberRepository {
    private final Map<String, Member> store = new HashMap<>();
    private long idSequence = 0L;

    @Override
    public Member save(Member member) {
        ReflectionTestUtils.setField(member, "id", ++idSequence);
        store.put(member.getEmail(), member);
        return member;
    }

    @Override
    public Optional<Member> findByEmail(String email) {
        return Optional.ofNullable(store.get(email));
    }
}

class SignupService {
    private final MemberRepository memberRepository;
    SignupService(MemberRepository memberRepository) { this.memberRepository = memberRepository; }
    // ...
}

@Test
void signup_withFakeRepository() {
    MemberRepository fakeRepo = new InMemoryMemberRepository();
    SignupService service = new SignupService(fakeRepo);

    // 행위 테스트
}
```

### 3-2) Spring Test에서 Fake Bean 등록 (@TestConfiguration)

```java
@SpringBootTest
@ActiveProfiles("test")
class SignupIntegrationTest {

    @TestConfiguration
    static class FakeConfig {
        @Bean
        @Primary
        MemberRepository memberRepository() {
            return new InMemoryMemberRepository(); // Fake 대신 실제 DB를 쓰지 않음
        }
    }

    @Autowired
    SignupService signupService;

    @Test
    void signup_withFakeRepositoryBean() {
        // ...
    }
}
```

- 실무에서 Fake는 “DB/외부 시스템이 없어도 테스트할 수 있는 중간 단계”로 자주 활용됩니다.

---

## 4. Mock 사용 예시 (행위 검증)

### 4-1) 서비스 계층에서 Validator/Repository Mock

```java
@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private MemberValidator memberValidator;

    @InjectMocks
    private MemberService memberService;

    @Test
    void updateMember_callsValidatorAndRepository() {
        Member member = Member.create("user@test.com", "pass", "old");
        ReflectionTestUtils.setField(member, "id", 1L);
        MemberUpdateRequest request = new MemberUpdateRequest("newNick", null);

        given(memberRepository.findByIdAndStatus(1L, MemberStatus.ACTIVE))
                .willReturn(Optional.of(member));
        given(memberRepository.save(member)).willReturn(member);

        memberService.updateMember(1L, request);

        // 행위 검증
        verify(memberValidator).validateNicknameNotDuplicated("newNick", member);
        verify(memberRepository).save(member);
    }
}
```

### 4-2) 컨트롤러 슬라이스에서 서비스 Mock

```java
@WebMvcTest(SignupController.class)
@AutoConfigureMockMvc(addFilters = false)
class SignupControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    SignupService signupService; // Controller의 협력자

    @Test
    void signUp_returnsCreated() throws Exception {
        SignupRequest request = new SignupRequest("user@test.com", "password", "tester", null);
        given(signupService.signup(any(SignupRequest.class)))
                .willReturn(new SignupResponse(1L));

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.userId").value(1L));

        verify(signupService).signup(any(SignupRequest.class));
    }
}
```

- 실무에서 Mock은 **“이 협력자가 꼭 호출되어야 한다”**는 계약을 표현할 때 사용합니다.

---

## 5. Spy 사용 예시

### 5-1) 부분 Stub + 호출 기록

```java
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Spy
    EmailSender emailSender; // 실제 구현을 Spy로 감쌈

    @InjectMocks
    NotificationService notificationService;

    @Test
    void sendEmail_sendsOnlyWhenEnabled() {
        // 일부 메서드는 Stub
        doReturn(true).when(emailSender).isEnabled();

        notificationService.send("msg", "user@test.com");

        verify(emailSender).send("user@test.com", "msg");
    }
}
```

- Spy는 실제 구현을 많이 건드리지 않고 호출 기록만 확인하고 싶을 때 사용하지만, **내부 구현에 결합되기 쉬워서 남용은 지양**합니다.

### 5-2) Spring에서 @SpyBean

```java
@SpringBootTest
class SomeIntegrationTest {

    @SpyBean
    SomeService someService; // 실제 Bean을 Spy로 감싸 로그/호출 여부를 확인

    @Test
    void verifyServiceIsCalled() {
        // ... API 호출 등
        verify(someService).doWork();
    }
}
```

---

## 6. Dummy/Stub/Fake/Mock/Spy를 고르는 기준 (실무 관점 요약)

1. **DB/외부 시스템 없이 서비스/도메인 로직을 보고 싶다**  
   → 인메모리 Fake (Repository, Client) 또는 인터페이스 기반 Stub.

2. **“이 메서드가 꼭 호출되었는지”를 확인하고 싶다**  
   → Mock/Spy (`verify`) 사용. 단, 비즈니스상 의미 있을 때만.

3. **컨트롤러 테스트에서 서비스만 대체하고 싶다**  
   → `@WebMvcTest` + `@MockBean Service` 또는 Standalone + `@Mock Service`.

4. **슬라이스/통합 테스트에서 특정 Bean만 대체하고 싶다**  
   → `@TestConfiguration` + `@Primary` Fake Bean, 필요시 `@SpyBean`/`@MockBean` 최소 활용.

5. **쓰이지 않는 인자를 그냥 채우고 싶다**  
   → Dummy (null 또는 No-Op 구현) 사용, 단 설계를 다시 생각해볼 신호일 수 있음.

실무에서는 “테스트 속도/안정성/의도의 분명함”을 기준으로, **가능한 한 Fake/Stub → 필요할 때만 Mock/Spy** 순으로 선택하는 것이 유지보수에 유리합니다.  
우리 프로젝트의 테스트 전략(유닛은 빠르고 많게, 통합은 핵심만, 인수는 플로우 위주)에도 이 우선순위를 그대로 적용합니다.

