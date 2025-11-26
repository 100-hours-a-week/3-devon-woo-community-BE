# 테스트 더블(Test Double) 가이드 – Mock, Stub, Fake, Spy, Dummy

> 이 문서는 docs/test 아래의 전략 문서들(테스트 베스트 프랙티스, Toss/카카오페이/조졸두 글 요약)을 바탕으로, 테스트 더블 개념을 정리하고 Spring 환경에서의 짧은 예시를 제공합니다.

---

## 1. 테스트 더블(Test Double)이란?

- 실제 협력 객체(리포지토리, 외부 API 클라이언트, 서비스 등)를 **테스트용 대체 객체**로 바꾸어 사용하는 모든 형태를 포괄하는 표현입니다.
- 목적:
  - 느린 의존성(DB/HTTP/파일)을 대체해 **테스트를 빠르고 결정적으로** 만들기
  - 아직 구현되지 않은 협력 객체를 대신하여, **대상 객체만 먼저 테스트**하기
  - 실패 상황/예외 상황 등 실제로 만들기 어려운 환경을 **인위적으로 재현**하기

주요 종류:
- Dummy
- Stub
- Fake
- Mock
- Spy

---

## 2. Dummy

- **사용되지 않는 파라미터를 채우기 위한 “자리 채우기 객체”**입니다.
- 테스트에서 실제로 그 객체를 사용하지 않으며, 보통 `null` 대신 타입을 맞추기 위해 생성합니다.

```java
// 예: 메서드 시그니처 때문에 어쩔 수 없이 넘겨야 하지만, 테스트에서는 사용하지 않는 인자
class NotificationService {
    void send(String message, EmailClient emailClient) {
        // 이번 테스트에서는 emailClient를 전혀 사용하지 않는 시나리오
    }
}

@Test
void send_whenMessageIsEmpty_shouldNotSend() {
    NotificationService service = new NotificationService();
    EmailClient dummyEmailClient = null; // 또는 new NoOpEmailClient();

    service.send("", dummyEmailClient);

    // emailClient에 대한 검증은 없음
}
```

실무에서는 의미 없는 Dummy 객체를 많이 만들기보다는, **테스트 설계를 단순하게 바꾸는 것이 우선**입니다.

---

## 3. Stub

- **미리 정해둔 값을 반환하는 “대답 전용” 테스트 더블**입니다.
- 호출 횟수나 순서에는 관심이 없고, “이 입력에 이런 결과를 돌려준다”에만 관심이 있습니다.

```java
// 서비스: 할인율을 외부 정책 서비스에서 받아온다고 가정
class DiscountService {
    private final DiscountPolicyClient client;

    DiscountService(DiscountPolicyClient client) {
        this.client = client;
    }

    int discountPrice(int original) {
        int rate = client.getDiscountRate(); // 외부 API
        return original - (original * rate / 100);
    }
}

@Test
void discountPrice_whenRateIs10_returns90Percent() {
    // Stub: 항상 10을 반환
    DiscountPolicyClient stubClient = () -> 10;
    DiscountService service = new DiscountService(stubClient);

    int result = service.discountPrice(10000);

    assertThat(result).isEqualTo(9000);
}
```

Spring에서는 인터페이스 기반 의존성 주입을 이용해, **테스트 전용 Stub 구현을 주입**하는 패턴이 많습니다.

---

## 4. Fake

- **간단한 인메모리 구현**으로 실제 객체를 흉내 내는 테스트 더블입니다.
- 실제와 같은 인터페이스를 가지지만, 구현은 단순화합니다.

```java
interface MemberRepository {
    Member save(Member member);
    Optional<Member> findById(Long id);
}

class InMemoryMemberRepository implements MemberRepository { // Fake
    private final Map<Long, Member> store = new HashMap<>();
    private long sequence = 0L;

    @Override
    public Member save(Member member) {
        ReflectionTestUtils.setField(member, "id", ++sequence);
        store.put(member.getId(), member);
        return member;
    }

    @Override
    public Optional<Member> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }
}

@Test
void signUp_withFakeRepository() {
    MemberRepository fakeRepository = new InMemoryMemberRepository();
    SignupService signupService = new SignupService(
            fakeRepository,
            new AuthValidator(fakeRepository),
            password -> "encoded-" + password // 간단한 PasswordEncoder fake
    );

    SignupResponse response = signupService.signup(
            new SignupRequest("user@test.com", "pass", "tester", null)
    );

    assertThat(response.userId()).isEqualTo(1L);
}
```

Fake는 **통합 테스트로 내릴 필요는 없지만, 유닛 테스트만으로는 부족**할 때 좋은 중간 지점입니다.

---

## 5. Mock

- **어떻게 호출되었는지(행위)를 검증하는 테스트 더블**입니다.
- 호출 횟수, 호출 인자, 호출 순서 등에 관심이 있습니다.

```java
@UnitTest
class SignupServiceTest {

    @Mock
    MemberRepository memberRepository; // Mock

    @Mock
    AuthValidator authValidator;

    @Mock
    PasswordEncoder passwordEncoder;

    @InjectMocks
    SignupService signupService;

    @Test
    void signup_invokesValidatorAndSavesMember() {
        SignupRequest request = new SignupRequest("user@test.com", "password", "tester", null);
        given(passwordEncoder.encode("password")).willReturn("encoded");
        given(memberRepository.save(any(Member.class))).willAnswer(invocation -> {
            Member member = invocation.getArgument(0);
            ReflectionTestUtils.setField(member, "id", 1L);
            return member;
        });

        SignupResponse response = signupService.signup(request);

        assertThat(response.userId()).isEqualTo(1L);
        verify(authValidator).validateSignup(request); // “어떻게 호출됐는지”에 관심
        verify(memberRepository).save(any(Member.class));
    }
}
```

주의: **내부 구현에 과도하게 결합된 Mock 검증**은 리팩터링을 어렵게 만듭니다. (조졸두 글에서 내부 구현 검증을 피하라고 강조)

---

## 6. Spy

- **실제 객체를 감싸 호출을 기록하는 테스트 더블**입니다.
- 보통 일부 메서드는 그대로 사용하고, 몇몇 메서드만 스텁하거나 호출 여부를 검증합니다.

```java
@Test
void spyExample() {
    List<String> realList = new ArrayList<>();
    List<String> spyList = Mockito.spy(realList); // Spy

    spyList.add("one");
    spyList.add("two");

    verify(spyList).add("one"); // 호출 여부 검증
    assertThat(spyList).containsExactly("one", "two"); // 실제 동작 결과도 검증
}
```

Spring에서 Spy를 사용할 때는 **불필요한 부분까지 기록/검증하는 테스트 스멜**이 생기지 않도록 주의해야 합니다.

---

## 7. 우리 프로젝트에서의 사용 권장 패턴

docs/test에 정리된 전략을 바탕으로:

1. **우선순위: 실제 객체 → Fake/Stub → Mock**
   - 도메인/서비스 로직: 가능하면 Fake/Stub 또는 인메모리 구현(Fake)으로 테스트.
   - 외부 시스템(HTTP, Redis, 외부 결제 등): Fake/Stub가 어려운 경우 Mock 사용.

2. **Mock은 “행위 검증이 필요한 곳에만”**
   - “정확히 이 인자로 한 번 호출되어야 한다”는 것이 비즈니스적으로 의미 있을 때만 사용.
   - 단순하게는 Stub/Fake로 충분한 경우 Mock 검증을 줄인다.

3. **테스트 더블을 위한 공용 Fixture/TestConfig**
   - `java-test-fixtures`를 활용해:
     - 인메모리 리포지토리(Fake)
     - 공통 Builder/헬퍼(Stub 데이터)
     - 컨트롤러 테스트용 ArgumentResolver, Security Fake
   - 를 한 곳에 모아서 여러 테스트에서 재사용한다.

4. **컨트롤러 테스트에서는 Security를 Mock이 아니라 Fake/Disable로 처리**
   - LogoutFilter/JwtFilter 등은 테스트 전용 SecurityConfig에서 disable 또는 Fake로 대체.
   - 컨트롤러 테스트에서는 서비스/검증 로직을 Mock/Stub로 처리하고, 보안 흐름은 인수/통합 테스트에서 별도로 검증.

---

## 8. 정리

- 테스트 더블은 단일 개념이 아니라, 목적에 따라 쓰이는 여러 패턴(Dummy/Stub/Fake/Mock/Spy)의 묶음입니다.
- 우리 프로젝트에서는:
  - 도메인/서비스: Stub/Fake 위주 (빠르고 결정적인 테스트)
  - 컨트롤러: Standalone/슬라이스 + 최소 Mock
  - 외부 연동: 필요시 Mock, 가능하면 Fake/Stub
  - 공통 Fixture는 testFixtures로 공유
  를 기본 원칙으로 삼습니다.

이렇게 하면 테스트가 **빠르고, 의도가 명확하며, 설계 변경에도 견고한 상태**를 유지할 수 있습니다.

