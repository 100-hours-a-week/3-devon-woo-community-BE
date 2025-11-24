# 토스 기술 블로그 요약 – Gradle에서 테스트 의존성 관리하기

> 원문: [테스트 의존성 관리로 높은 품질의 테스트 코드 유지하기](https://toss.tech/article/how-to-manage-test-dependency-in-gradle)  
> 이 문서는 토스페이먼츠 글을 바탕으로, 멀티 모듈 Gradle 환경에서 테스트용 Builder/Helper와 테스트 전용 의존성을 관리하는 방법을 정리한 것입니다.

---

## 1. 문제의식: 테스트 코드도 “의존성 지옥”에 빠진다

- 테스트 코드 품질이 낮으면:
  - 이해하기 어렵고 여기저기 깨진 테스트가 늘어나며
  - 결국 기술 부채 못지않은 “테스트 부채”가 됩니다.
- 이를 막기 위해 다양한 **Builder, Helper, 테스트 전용 의존성**을 도입하지만,
  - 이 도구들 자체도 관리 대상입니다.
  - 제대로 관리하지 않으면 **중복 코드, 의존성 지옥**으로 이어집니다.

글의 목표:
- Gradle의 `java-test-fixtures` 플러그인을 활용해
  - 테스트 전용 코드(Builder/Helper)를 모듈 간에 안전하게 공유하고,
  - 테스트 전용 의존성(H2 등)을 다른 모듈로 전달하는 방법을 설명합니다.

---

## 2. 예시 프로젝트 구조 (멀티 모듈)

- **`domain` 모듈**
  - 핵심 비즈니스 로직만 포함.
  - 외부 시스템(DB, HTTP, 서드파티 라이브러리)에 의존하지 않음.
  - 다른 모듈에서 `implementation(project(":domain"))` 으로 의존.

- **`db` 모듈**
  - 데이터의 CRUD에만 관심.
  - 비즈니스 규칙을 위해 `domain` 에 의존 (`implementation(project(":domain"))`).

- **`application` 모듈**
  - 클라이언트 요청을 처리하는 진입점.
  - 비즈니스를 위해 `domain` 에 의존 (`implementation`),
  - 데이터 접근을 위해 `db` 에 런타임 의존 (`runtimeOnly(project(":db"))`).

이 구조 위에서 테스트용 Builder/Helper와 테스트 전용 의존성을 어떻게 공유·전파할지 다룹니다.

---

## 3. 테스트 전용 Builder/Helper를 다른 모듈에 노출하기

### 3.1 기본 문제

- `domain` 모듈에 아래와 같은 엔티티가 있다고 가정합니다.

```kotlin
class Order(
    val id: String,
    val description: String,
    val amount: Long,
)
```

- 필드가 많을수록 테스트에서 매번 객체를 생성하기 번거롭고,
  - 필수값을 기본값으로 처리하면 프로덕션에서 잘못 사용될 위험이 있습니다.
- 그래서 테스트용으로만 사용할 `OrderBuilder` 같은 Builder를 만듭니다.
  - 이 Builder는 당연히 **`domain/src/test`** 아래에 둡니다.
  - 프로덕션 코드에서 사용되면 안 되기 때문입니다.

문제:
- `application`, `db` 모듈은 `domain` 에 의존하지만,
  - `domain` 의 `src/test` 에 있는 `OrderBuilder` 는 빌드 결과(jar)에 포함되지 않으므로
  - 다른 모듈 테스트에서 `OrderBuilder` 를 import 할 수 없습니다.

### 3.2 단순(하지만 나쁜) 해결책들

1. 각 모듈 `src/test` 에 Builder를 복붙 → **중복/관리 지옥**.
2. `test-data` 같은 별도 테스트 전용 모듈을 만들고 여기에 Builder/Helper를 모은다.
   - 각 모듈이 `testImplementation(project(":test-data"))` 로 의존.
   - 하지만:
     - 실제 소스(`Order`)와 테스트 도우미(`OrderBuilder`)가 모듈로 분리되어 응집도가 떨어지고,
     - `test-data` 의 클래스는 `main` 디렉토리에 둬야 하므로
       “프로덕션에서 쓰는 것처럼 보이는” 혼란을 야기합니다.

둘 다 만족스럽지 않습니다.

---

## 4. 구세주: `java-test-fixtures` 플러그인

### 4.1 Builder/Helper 공유

1. `domain` 모듈에 `java-test-fixtures` 플러그인 추가

```kotlin
// domain/build.gradle.kts
plugins {
    // ...
    `java-test-fixtures`
}
```

2. `testFixtures` 디렉토리 생성 후 테스트 전용 Builder/Helper 이동
   - IntelliJ에서 `java-test-fixtures` 적용 후 디렉토리 생성 시 `testFixtures` 가 자동 제안됩니다.
   - `OrderBuilder` 를 `src/testFixtures` 로 옮깁니다.

3. 빌드 시 `testFixturesJar` 생성
   - `gradlew :domain:build` 를 수행하면
     - `testFixturesJar` 가 만들어지고,
     - 그 안에 `OrderBuilder` 가 포함됩니다.
   - `plain.jar` (main)에는 프로덕션 코드만,
     `test-fixtures.jar` 에는 Fixture 코드만 포함됩니다.

4. 다른 모듈에서 `testFixtures` 의존 추가

```kotlin
// application/db 모듈의 build.gradle.kts
dependencies {
    implementation(project(":domain"))
    testImplementation(testFixtures(project(":domain")))
}
```

이제:
- `application` / `db` 모듈의 테스트 코드에서
  - `domain` 의 `src/testFixtures` 안에 있는 `OrderBuilder` 등을 그대로 사용할 수 있습니다.
- 장점:
  - 테스트 전용 클래스만 노출되고,
  - `@Test` 붙은 테스트 본문 등은 외부에 노출되지 않습니다.

---

## 5. 테스트 전용 의존성을 다른 모듈에 전파하기

### 5.1 문제: H2를 `db` 에만 추가하면?

- `db` 모듈에서 통합 테스트를 위해 H2를 **테스트 전용**으로 추가했다고 가정합니다.

```kotlin
// db/build.gradle.kts
dependencies {
    // ...
    testRuntimeOnly("com.h2database:h2")
}
```

- 이 상태에서 `db` 모듈의 통합 테스트는 H2를 잘 사용합니다.
- 하지만 `application` 모듈은 다음과 같이 `db` 에 의존합니다.

```kotlin
// application/build.gradle.kts
dependencies {
    runtimeOnly(project(":db"))
}
```

- 기대: `application` 통합 테스트에서도 H2가 사용되길 희망.
- 현실: `application` 의 `testRuntimeClasspath` 에 H2가 포함되지 않아, H2 없이 테스트가 실행됩니다.

이유:
- `db` 의 `testRuntimeClasspath` 에 있는 H2는
  - `application` 의 `testRuntimeClasspath` 로 **전파되지 않기 때문**입니다.
- `runtimeOnly(project(":db"))` 는
  - `application` 의 `runtimeClasspath` 와 `testRuntimeClasspath` 에 `db` 를 추가하지만,
  - `db` 의 **테스트 전용 의존성**까지 자동으로 전파되지는 않습니다.

### 5.2 해결: `testFixturesRuntimeOnly` + `testRuntimeOnly(testFixtures(...))`

1. `db` 모듈에 `java-test-fixtures` 적용 + H2를 `testFixturesRuntimeOnly` 로 변경

```kotlin
// db/build.gradle.kts
plugins {
    // ...
    `java-test-fixtures`
}

dependencies {
    // ...
    testFixturesRuntimeOnly("com.h2database:h2")
}
```

- 이렇게 하면:
  - `testFixturesCompileClasspath`, `testFixturesRuntimeClasspath` 가 생기고,
  - `testFixturesRuntimeOnly` 로 추가한 H2는
    - `testFixturesRuntimeClasspath` 와
    - `testRuntimeClasspath` 둘 다에서 보이게 됩니다.

2. `application` 에서 `db` 의 test fixtures 의존성 전파

```kotlin
// application/build.gradle.kts
dependencies {
    runtimeOnly(project(":db"))
    testRuntimeOnly(testFixtures(project(":db")))
}
```

- `testRuntimeOnly(testFixtures(project(":db")))` 를 통해
  - `db` 모듈의 `testFixturesRuntimeClasspath` 에 있는 H2 의존성이
  - `application` 의 `testRuntimeClasspath` 로 전파됩니다.

결과:
- `application` 모듈의 통합 테스트에서도 H2를 사용할 수 있게 됩니다.
- `application` 은 여전히 “어떤 DB를 쓰는지”에는 구체적으로 관여하지 않고,
  - 관심사 분리를 유지한 채
  - 테스트 전용 의존성만 전달받습니다.

---

## 6. 정리

- 테스트 코드도 소프트웨어로서 품질 관리가 필요하며,
  - Builder/Helper, 테스트 전용 의존성 자체도 **구조적으로 관리**해야 합니다.
- Gradle `java-test-fixtures` 플러그인을 활용하면:
  1. 테스트용 Builder/Helper 클래스를  
     - `src/testFixtures` 에 두고
     - `testImplementation(testFixtures(project(":module")))` 으로 다른 모듈 테스트와 공유할 수 있습니다.
  2. 테스트 전용 의존성(H2 등)을  
     - `testFixturesRuntimeOnly` 로 추가하고
     - 다른 모듈에서 `testRuntimeOnly(testFixtures(project(":module")))` 로 전파할 수 있습니다.
- 이렇게 하면:
  - 중복 테스트 코드/의존성 추가를 줄이고,
  - 모듈 간 관심사 분리를 유지하면서
  - 멀티 모듈 환경에서도 높은 품질의 테스트 코드를 유지할 수 있습니다.

