# 테스트 픽스처 컨벤션

이 문서는 자바 기반 프로젝트에서 테스트 픽스처를 사용할 때 따라야 하는 공통 컨벤션을 정의합니다.  

---

## 1. 적용 범위

- 대상
  - 도메인 엔티티, 값 객체
  - 요청/응답/조회 DTO
  - 테스트 전용 인프라(Fake/Stub, 공통 설정 등)
- 목적
  - 테스트에서 반복적으로 사용하는 “유효한 기본 상태”와 대표 시나리오를 캡슐화한다.

---

## 2. Gradle 및 소스셋

- 기본 원칙
  - 공용 테스트 코드는 Gradle `java-test-fixtures` 소스셋에 둔다.
  - 모듈 간에 공유할 픽스처는 test-fixtures JAR를 통해 의존한다.

- Gradle 설정 예시

```groovy
// 공용 도메인 모듈
plugins {
    id 'java'
    id 'java-test-fixtures'
}

// 다른 모듈에서 공용 픽스처 의존
dependencies {
    testImplementation(testFixtures(project(":domain")))
}
```

- 테스트 전용 의존성
  - 픽스처에서 사용하는 라이브러리(H2, Testcontainers 등)는 `testFixturesImplementation` / `testFixturesRuntimeOnly`로 선언한다.
  - 픽스처를 사용하는 모듈에서는 `testImplementation(testFixtures(project(":xxx")))`, `testRuntimeOnly`로만 참조한다.

---

## 3. Fixture 클래스 규칙

- 네이밍
  - 규칙: **대상 + Fixture**
    - 예: `OrderFixture`, `OrderRequestFixture`, `OrderQueryDtoFixture`
- 구조
  - 클래스는 `final`로 선언한다.
  - 기본 생성자는 `private`으로 막는다.
  - 인스턴스 상태를 가지지 않고, `static` 메서드와 `static final` 상수만 제공한다.
- 상수
  - 유효한 기본값: `DEFAULT_` 접두어 사용
    - 예: `OrderFixture.DEFAULT_ORDER_ID`, `DEFAULT_TOTAL_AMOUNT`
  - 대표적인 변경 값: `UPDATED_` 접두어 사용
    - 예: `OrderFixture.UPDATED_TOTAL_AMOUNT`

---

## 4. Fixture 메서드 규칙

- 엔티티용
  - `create()`
    - 유효한 기본값을 가진 새 엔티티를 생성한다.
    - 예: `OrderFixture.create()`
  - `createWithId()`
    - ID가 채워진 엔티티를 생성한다(내부에서 Reflection 사용 가능).
    - 예: `OrderFixture.createWithId()`
  - 의미 있는 변형은 이름에 명시한다.
    - 예: `OrderFixture.createCanceled()`, `createCompleted()`

- 요청 DTO용
  - `createRequest()`
    - 정상 시나리오에 사용하는 기본 요청 DTO를 생성한다.
    - 예: `OrderRequestFixture.createRequest()`
  - `updateRequest()`
    - 수정 시나리오에 사용하는 요청 DTO를 생성한다.
  - 실패/경계 케이스는 메서드 이름으로 구분한다.
    - 예: `OrderRequestFixture.createRequestWithoutItems()`, `createRequestWithTooManyItems()`

- 응답/조회 DTO용
  - 여러 테스트에서 동일한 형태의 조회 결과를 사용할 때만 별도 Fixture를 둔다.
  - `create()`: 기본값으로 DTO를 생성한다.
  - `createWithAllFields(...)`: 모든 필드를 받아 오버라이드할 수 있도록 제공한다.

---

## 5. 테스트에서의 사용 규칙

- 공통
  - 가능하면 생성자를 직접 호출하지 않고 Fixture 메서드를 사용한다.
  - Reflection 기반 유틸리티는 Fixture 내부에서만 사용하고, 테스트 본문에는 드러내지 않는다.

- 테스트 종류별
  - 도메인 테스트
    - 연관 객체/부수적인 도메인 객체 생성에 Fixture를 사용한다.
    - 저장 여부가 중요하지 않으면 `create()`, 저장 상태가 필요하면 `createWithId()`를 사용한다.
  - 서비스/리포지토리 테스트
    - 도메인 Fixture와 DTO Fixture를 함께 사용해 시나리오를 구성한다.
  - 컨트롤러/통합 테스트
    - 요청 DTO Fixture를 우선 사용한다.
    - 동일한 시나리오를 여러 레이어에서 검증할 경우, 가능한 한 동일한 Fixture 메서드를 공유한다.

---

## 6. 생성/수정 시 체크리스트

- 새 도메인 또는 API 추가 시
  - 엔티티 정의와 함께 엔티티 Fixture를 추가한다.
  - 요청 DTO 정의와 함께 요청 DTO Fixture를 추가한다.
  - 반복 사용되는 조회/응답 DTO가 있다면 별도 Fixture를 고려한다.

- 기존 도메인 변경 시
  - 필드 추가/변경 시 관련 Fixture의 기본값 상수와 `create*` 메서드를 함께 갱신한다.
  - 변경된 필드를 사용하는 테스트가 모두 Fixture를 통해 값을 주입하는지 확인한다.

