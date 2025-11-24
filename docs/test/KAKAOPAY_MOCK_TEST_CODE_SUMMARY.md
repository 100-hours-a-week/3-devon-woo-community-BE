# 카카오페이 기술 블로그 요약 – Mock 기반 테스트 코드 전략 정리

> 원문: [실무에서 적용하는 테스트 코드 작성 방법과 노하우 Part 1: Mock 관련 테스트 코드 작성 방법](https://tech.kakaopay.com/post/mock-test-code/)  
> 이 문서는 카카오페이 정산플랫폼팀 글을 기반으로, Mock·Mock Server·@MockBean·@TestConfiguration을 활용한 테스트 전략과 그 한계를 정리한 것입니다.

---

## 1. 글의 문제의식

- 서비스 초기에는 간단한 비즈니스 로직에 대해 **직접 DB에 저장하고 그 결과만 검증하는 테스트**로 충분합니다.
- 시간이 지나면서
  - 외부 파트너 API 연동
  - 복잡한 신규 가입/등록 플로우
  - 여러 계층(Controller, Service, Client) 의존 관계
  가 생기면서 **HTTP 호출을 Mocking해야 하는 상황**이 늘어납니다.
- 처음에는 Mock Server로도 괜찮지만, 점점 다음과 같은 문제가 발생합니다.
  1. Mock Server 기반 HTTP Mocking이 너무 많은 JSON·요청/응답 매핑을 요구해 생산성이 떨어진다.
  2. @MockBean을 남발하면 Application Context가 자주 재초기화되어 빌드 시간이 크게 늘어난다.
  3. 어떤 영역은 테스트를 쓰기 어렵거나 불가능해지고, 그 영향이 다른 코드로 전이되면서 “Black Box”가 커진다.

글은 이 문제들을 해결하기 위한 실무적인 패턴을 설명합니다.

---

## 2. 예시 도메인: 신규 가맹점 등록 플로우

### 2.1 초기 구조

- `ShopRegistrationService.register(brn, shopName)`  
  → 전달받은 사업자 번호와 가맹점명을 그대로 DB에 저장하는 단순한 구조.

- 테스트:
  - 입력한 `brn`, `shopName` 이 저장된 `Shop` 엔티티에 잘 반영되었는지만 검증하는 단순 유닛/서비스 테스트.

### 2.2 요구사항 확장

- 요구사항: “가맹점 등록 전, 파트너 시스템(Partner API)을 호출해 사업자 정보를 검증하고 이름을 가져온다.”
- 변경된 구조:
  - `ShopRegistrationService.register(brn)`는 더 이상 `shopName`을 직접 받지 않고,
  - `PartnerClient`를 통해 외부 API를 호출해 `PartnerResponse(brn, name)` 을 가져온 뒤, 그 값을 저장.

이 시점부터 **외부 HTTP 호출을 어떻게 테스트에서 다룰 것인가**가 핵심 과제가 됩니다.

---

## 3. Mock Server 기반 HTTP Mocking의 한계

### 3.1 장점

- `MockWebServer` / WireMock 등으로  
  - 요청 URL, Method, Header, Body에 따라
  - 정해진 Response를 반환하도록 설정해
  - 실제 네트워크 없이 HTTP 기반 연동 로직을 테스트할 수 있습니다.

### 3.2 단점 – 과도한 Mocking에 따른 생산성 저하

- 예를 들어 `PartnerClient`를 사용하는 곳이
  - A Controller, A Service 정도만 있을 때에는 Mock Server 설정도 감당할 만합니다.
- 하지만 시간이 지나면서
  - 여러 Service/Controller/Batch 등 다양한 곳에서 `PartnerClient`를 직·간접적으로 의존하면
  - 각 테스트마다 HTTP Mocking 설정(요청/응답 JSON, Header, Query 등)을 반복해서 작성해야 합니다.
- 실제 업무에서는 Request/Response Body가 수십~수백 줄이 되는 경우도 흔하므로,
  - Mock Server 기반 HTTP Mocking을 과도하게 사용하면
  - **테스트 코드가 복잡하고 길어지며, 개발 생산성이 눈에 띄게 떨어집니다.**

---

## 4. @MockBean 기반 객체 행위 Mocking

### 4.1 접근법

- Mock Server 대신, `PartnerClient` 자체를 **Mock 객체**로 만들어 Spring 컨텍스트에 주입합니다.
- 테스트에서는:

  - `@MockBean lateinit var partnerClient: PartnerClient`
  - `given(partnerClient.getPartner(brn)).willReturn(PartnerResponse(brn, name))`

  와 같이 Mockito(또는 다른 Mock 라이브러리)를 사용해 **객체 행위 자체를 Mocking** 합니다.

### 4.2 장점

- HTTP 레벨이 아닌 **메서드 호출 레벨**에서 Mocking하므로,
  - JSON/HTTP 세부 설정 없이도
  - 다양한 케이스(성공, 실패, 예외)를 간단히 구성할 수 있습니다.
- Mock Server 대비 훨씬 짧고 가독성 좋은 테스트 코드를 작성할 수 있습니다.

### 4.3 단점 – Application Context 재초기화 문제

- `@MockBean`은 기존 Bean을 대체하는 방식이라,  
  **테스트마다 다른 MockBean 구성이 있으면 Spring Application Context를 다시 구성해야 합니다.**
- 테스트 클래스가 많아질수록,
  - 각기 다른 @MockBean 설정 때문에
  - 컨텍스트 재초기화가 반복되고,
  - 테스트 전체 빌드 시간이 크게 늘어납니다.
- 멀티 모듈 환경, 다양한 테스트 조합이 있을 때 이 문제는 더욱 심각해집니다.

---

## 5. @TestConfiguration + @Primary를 통한 개선

### 5.1 아이디어

- `@MockBean` 대신 **테스트용 mock Bean을 직접 등록**해,  
  Application Context가 재사용되도록 합니다.

```kotlin
@TestConfiguration
class ClientTestConfiguration {

    @Bean
    @Primary
    fun mockPartnerClient() = mock(PartnerClient::class.java)!!
}
```

- 위 Bean은:
  - 실제 `PartnerClient`와 같은 타입이지만,
  - Mockito의 `mock()`으로 생성된 Mock 객체입니다.
  - `@Primary`로 우선순위를 높여 실제 Bean 대신 이 Mock이 주입되게 합니다.
  - `test` 소스셋에만 존재하고 `@TestConfiguration` 이므로 운영 코드에는 영향을 주지 않습니다.

### 5.2 사용 방식

- 테스트 클래스에서는 `@MockBean` 대신 **일반 의존성 주입**을 사용합니다.

```kotlin
class ShopRegistrationServiceMockBeanTest(
    private val shopRegistrationService: ShopRegistrationService,
    private val partnerClient: PartnerClient,   // @MockBean이 아닌, TestConfiguration에서 등록된 Bean
) : TestSupport() {

    @Test
    fun `register mock bean test`() {
        // given
        val brn = "000-00-0000"
        val name = "주식회사 XXX"
        given(partnerClient.getPartner(brn))
            .willReturn(PartnerResponse(brn, name))

        // when
        val shop = shopRegistrationService.register(brn)

        // then
        then(shop.name).isEqualTo(name)
        then(shop.brn).isEqualTo(brn)
    }
}
```

### 5.3 효과

- `@MockBean`을 제거하고 **컨텍스트에 항상 같은 Mock Bean**을 등록하므로,
  - Application Context를 재사용할 수 있고
  - 테스트 속도와 빌드 시간이 개선됩니다.
- 동시에 Mock Server 대비 더 간결한 Mocking 구성이 가능합니다.

---

## 6. Black Box 영역과 그 전이(Propagation)를 막기

### 6.1 Black Box 영역이란

- 글에서는 **테스트 코드 작성이 어려운 영역**을 “Black Box 영역”이라고 부릅니다.
  - 외부 시스템에 강하게 의존하는 부분
  - 복잡한 인프라 설정이 필요한 부분
  - 환경상 테스트 환경 구성이 힘든 부분 등.

### 6.2 문제점 – Black Box의 전이

- Black Box 영역을 직접/간접적으로 의존하는 코드들은,
  - 함께 테스트하기 어려워지고
  - 결국 해당 영역 전체가 **테스트 불가능한 덩어리**가 됩니다.
- 제대로 격리하지 않으면 시간이 지날수록
  - Black Box 영역이 점점 커지고
  - 시스템 전반에 걸쳐 테스트가 어려운 구간이 확대됩니다.

### 6.3 해결 방향 – 격리와 경계 설정

- 핵심 메시지:

> Black Box 영역을 완전히 테스트하지 못하더라도,  
> 그 영향이 다른 영역으로 전이되지 않도록 **격리**해야 한다.

- 방법 예시:
  - 외부 시스템 의존부를 **Interface + Adapter**로 감싸고,  
    내부 도메인 로직은 인터페이스만 바라보도록 설계.
  - 그 인터페이스 구현체를 Mock/Stub/테스트용 구현으로 대체해,  
    도메인/서비스 로직은 최대한 테스트할 수 있도록 합니다.
- Mock는 이 격리를 위해 매우 유용한 도구이지만,
  - “Mock를 잘 쓰는 것” 자체가 목표가 아니라,
  - **Mock를 활용해 테스트 불가능한 영역을 잘라내고 나머지를 테스트 가능하게 만드는 것**이 목표입니다.

---

## 7. 정리 – 우리에게 주는 시사점

카카오페이 글이 전달하는 핵심을 요약하면 다음과 같습니다.

1. **Mock Server만으로 모든 HTTP 연동을 테스트하려 들면 생산성이 급격히 떨어진다.**
2. **@MockBean 남발은 Application Context 재초기화를 유발해 테스트 속도를 크게 저하시킨다.**
3. **@TestConfiguration + @Primary Mock Bean**으로 테스트 전용 Mock를 컨텍스트에 등록하면,
   - 실제 Bean처럼 재사용되면서도
   - 테스트에서 유연하게 Mocking할 수 있다.
4. 테스트하기 어려운 **Black Box 영역이 전이되지 않도록 경계를 명확히 하고 격리해야 한다.**
5. 테스트 코드는 운영 코드보다 관심도가 낮게 취급되기 쉽지만,
   - 운영 코드의 안정성과 효율성을 위해
   - 구조 변화에 맞춰 **테스트 구조도 함께 리팩터링**해야 한다.

우리 프로젝트에 적용한다면:

- 외부 API/클라이언트를 사용하는 코드에 대해
  - HTTP Mock Server보다 **Client 인터페이스 + TestConfiguration 기반 Mock Bean** 전략을 우선 고려합니다.
- 테스트하기 어려운 인프라·외부 의존부는
  - 명확한 인터페이스 경계를 두고,
  - 그 경계 바깥을 Mock/Stub으로 잘라,
  - 도메인/서비스 로직은 최대한 테스트 가능한 상태로 유지합니다.  

