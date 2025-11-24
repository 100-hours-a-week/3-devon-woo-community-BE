# Spring 테스트 어노테이션 완전 정리 문서

본 문서는 Spring Boot, Spring Test, JUnit5, Mockito 환경에서 사용되는 테스트 관련 어노테이션을 범주별로 정리한 참고용 자료이다. 모든 테스트 계층(단위·슬라이스·통합)에서 어떤 어노테이션이 어떤 역할을 수행하는지를 명확히 파악할 수 있도록 구성하였다.

---

# 1. JUnit5 기본 테스트 어노테이션

테스트 코드 기본 구조를 구성하는 가장 기초적인 어노테이션들.

## @Test

* 테스트 메서드를 정의하는 기본 어노테이션.
* 반환값 없음, 예외 던지기 허용.

## @BeforeEach / @AfterEach

* 각 테스트 실행 전/후 실행.
* 테스트 초기화, Mock 초기화 등에 사용.

## @BeforeAll / @AfterAll

* 테스트 클래스 전체 시작 전/후 단 한 번 실행.
* 정적(static) 메서드 필요.

## @DisplayName

* 테스트 이름을 사람이 읽기 좋은 형태로 제공.
* 테스트 리포트 가독성 향상.

## @Nested

* 테스트를 논리적 그룹으로 묶는 용도.
* 구조적인 테스트 케이스 작성 가능.

## @Disabled

* 특정 테스트 또는 테스트 클래스 전체 비활성화.

---

# 2. Mockito(Mock) 관련 어노테이션

Spring 없이 순수 단위 테스트 또는 Spring 테스트에서도 Mock 활용 시 사용.

## @Mock

* Mockito Mock 객체를 생성.
* Spring과 무관하며 가볍다.

## @Spy

* 실제 객체를 감싸 일부만 Mocking.

## @InjectMocks

* @Mock 또는 @Spy 객체를 자동으로 주입받는 실제 객체 생성.
* DI 기반 테스트에 적합.

## @Captor

* ArgumentCaptor를 선언할 때 사용.

## @ExtendWith(MockitoExtension.class)

* Mockito 확장을 JUnit5에서 사용.
* @Mock 등을 자동 초기화.

---

# 3. Spring Test 컨텍스트 관련 어노테이션

Spring TestContext Framework를 활성화하거나 구성 방식 조절.

## @ExtendWith(SpringExtension.class)

* Spring TestContext 연동.
* Spring ApplicationContext 로딩 가능.

## @ContextConfiguration

* 특정 Configuration 클래스나 XML 설정을 테스트 컨텍스트로 로딩.

## @DirtiesContext

* 테스트 실행 후 컨텍스트가 변경되었으므로 재생성 필요함을 표시.
* 무거운 작업이므로 최소 사용 권장.

## @ActiveProfiles("test")

* 테스트 실행 시 사용할 Spring profile 지정.
* application-test.yaml 등을 로딩할 때 사용.

---

# 4. Spring Boot 테스트 어노테이션 (통합 + 슬라이스 테스트)

Spring Boot에서 계층별 테스트를 손쉽게 수행하기 위한 어노테이션.

## @SpringBootTest

* 전체 애플리케이션 컨텍스트 로딩.
* Service, Repository, Security, Filter 등 모든 Bean 활성화.
* 통합 테스트에 적합.

---

## 슬라이스 테스트(Slice Test)

애플리케이션의 특정 계층만 로드하여 빠른 테스트 수행.

### @WebMvcTest

* Controller + MVC 컴포넌트만 로드.
* Service, Repository 자동 로딩되지 않음.
* API 스펙 테스트(입출력, Validation)에 적합.

### @DataJpaTest

* JPA Repository 테스트 전용.
* Embedded DB + Hibernate + 트랜잭션 자동 롤백.

### @JsonTest

* Jackson 기반 직렬화/역직렬화 테스트.

### @RestClientTest

* RestTemplate 기반 외부 API 호출 테스트.

---

# 5. Bean Mocking/교체 관련 어노테이션

Spring 테스트 컨텍스트 환경에서 Mock 객체를 Bean으로 대체하는 용도.

## @MockBean

* Spring Context에서 기존 Bean을 Mock으로 교체.
* SpringBootTest, WebMvcTest에서 사용.
* Spring Boot 3.4+에서는 일부 환경에서 비권장.

## @SpyBean

* 기존 Spring Bean을 Spy 객체로 교체.

## @SpringMockBean / @SpringSpyBean

* Native Image, AOT 빌드 대응 버전.

---

# 6. MockMvc/웹 테스트 관련 어노테이션

Spring MVC API 테스트 자동 구성.

## @AutoConfigureMockMvc

* MockMvc 자동 구성 및 주입 지원.
* SpringBootTest 또는 WebMvcTest와 함께 사용.
* addFilters=false 옵션으로 Security 필터 비활성화 가능.

## @AutoConfigureRestDocs

* Spring REST Docs 자동 설정.

---

# 7. DB/트랜잭션 테스트 관련 어노테이션

데이터 영속성 테스트에서 사용.

## @Transactional

* 테스트 종료 후 자동 롤백.
* JPA 테스트 시 매우 자주 사용.

## @Rollback

* 트랜잭션 롤백 여부 지정.

## @Sql

* 테스트 전후로 SQL 스크립트 실행.
* 테스트 데이터 초기화에 사용.

## @SqlGroup

* 여러 @Sql 묶음.

---

# 8. 테스트 전용 구성/설정 어노테이션

테스트 환경을 현실적으로 구성하기 위해 활용.

## @TestConfiguration

* 테스트 전용 Bean을 정의하는 Configuration.
* @MockBean 대신 직접 Stub Bean을 등록할 때 사용.
* Spring Boot 3.4 기준으로 권장되는 Mock 대체 패턴.

## @Import

* 테스트 컨텍스트에 특정 설정 클래스/Bean을 추가.

---

# 9. 파라미터 기반 테스트 어노테이션

JUnit5의 반복 테스트 기능.

## @ParameterizedTest

* 여러 입력값으로 반복 테스트.

## @ValueSource, @CsvSource, @MethodSource

* ParameterizedTest 입력 데이터 제공.

---

# 10. 전체 총정리 (암기용)

테스트 어노테이션은 다음 6개 축으로 정리된다.

### 1) 컨텍스트 로딩

* @SpringBootTest, @WebMvcTest, @DataJpaTest, @ActiveProfiles

### 2) Mocking

* @Mock, @Spy, @InjectMocks, @MockBean, @SpyBean

### 3) MVC 테스트

* @AutoConfigureMockMvc, @WebMvcTest

### 4) DB 테스트

* @Transactional, @Sql

### 5) 구성/설정

* @TestConfiguration, @Import, @DirtiesContext

### 6) JUnit 기본

* @Test, @BeforeEach, @DisplayName, @Nested, @ParameterizedTest

---

이 문서는 Spring 테스트 환경을 구성하는 어노테이션의 전체 체계를 이해하기 위한 참고 자료이며, 각 어노테이션의 실제 사용 예시나 조합을 포함한 확장 문서도 필요 시 추가할 수 있다.
