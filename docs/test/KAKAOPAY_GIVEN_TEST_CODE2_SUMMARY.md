# 카카오페이 기술 블로그 요약 – Given 지옥에서 벗어나기 (Part 2)

> 원문: [실무에서 적용하는 테스트 코드 작성 방법과 노하우 – Given 편 Part 2](https://tech.kakaopay.com/post/given-test-code-2/)  
> 이 문서는 카카오페이 글을 바탕으로, Given 절에서 발생하는 여러 “지옥”과 이를 해결하는 방법을 정리한 것입니다.

---

## 1. Given 지옥: 문제 인식

글은 테스트 코드의 **Given 절**에서 반복적으로 겪게 되는 문제들을 “지옥”이라는 표현으로 묶어 설명합니다.

- Given에는 다양한 데이터 셋업 코드가 필요합니다.
- 이 셋업 코드 대부분은 테스트의 핵심 관심사(검증하고 싶은 규칙/행동)가 아니라 **부수적인 준비 작업**입니다.
- 그 결과:
  - 테스트 코드가 길고 복잡해지고
  - 가독성이 떨어지며
  - 추가 테스트 케이스를 쓰기 귀찮아져, 최소한의 케이스만 남게 됩니다.

대표적인 하위 문제로
- 파라미터 지옥
- 멀티 모듈 환경에서의 테스트 코드 재사용 지옥
- 외부 인프라 의존으로 인한 Mocking 지옥
을 소개합니다.

---

## 2. 파라미터 지옥

### 2.1 상황

- 예시: 상품 변경 이력을 저장하는 `ProductHistory` 엔티티와 그에 따른 테스트.
- 생성자에 많은 필드가 있어, 테스트에서 매번 모든 필드를 다 채워 넣어야 하는 상황입니다.

```kotlin
val productHistory = ProductHistory(
    effectiveStartDate = LocalDate.of(2024, 1, 1),
    effectiveEndDate = LocalDate.of(2025, 1, 1),
    productId = 1L,
    productName = "Sample Product",
    category = "Electronics",
    // ... 기타 필드 다수
)
```

- 이런 코드가 테스트마다 반복되면:
  - Given 절이 매우 길어지고
  - 테스트 핵심이 무엇인지 파악하기 어렵습니다.

### 2.2 개선: 테스트 데이터 빌더/헬퍼 도입

- 필수 필드만 입력하고 나머지는 기본값으로 채워주는 **Given 헬퍼 함수**를 정의합니다.

```kotlin
fun givenProductHistory(
    effectiveStartDate: LocalDate,
    effectiveEndDate: LocalDate,
): ProductHistory = ProductHistory(
    effectiveStartDate = effectiveStartDate,
    effectiveEndDate = effectiveEndDate,
    productId = 1L,
    productName = "Sample Product",
    category = "Electronics",
    // ... 나머지는 기본값
)
```

- 테스트에서는 핵심 차이만 명시하고, 나머지는 기본값에 맡깁니다.

```kotlin
val productHistory = givenProductHistory(
    effectiveStartDate = LocalDate.of(2024, 1, 1),
    effectiveEndDate = LocalDate.of(2025, 1, 1),
)
```

**효과**
- Given 절이 짧고 의도가 명확해집니다.
- 필드가 추가/변경되어도 헬퍼 한 곳만 수정하면 됩니다.

---

## 3. 멀티 모듈 환경에서의 테스트 코드 재사용 지옥

### 3.1 문제

- Gradle 멀티 모듈 구조에서, 각 모듈(`domain`, `api`, `batch` 등)의 `src/test`는 서로 참조할 수 없습니다.
- 따라서 `domain` 모듈의 테스트에 정의한 `givenProductHistory` 같은 헬퍼를
  - API, 서비스, 외부 통신, 배치 모듈 테스트에서 재사용할 수 없습니다.
- 결국 **동일한 Given 헬퍼/픽스처 코드가 모듈마다 복사-붙여넣기**되며:
  - 중복 증가
  - 수정 시 일관성 깨짐
  - 유지보수 난이도 상승

글에서는 이 상황을 **“멀티 모듈 환경에서의 테스트 코드 재사용 지옥”**이라고 부릅니다.

### 3.2 해결: `java-test-fixtures` 를 통한 공유

- Gradle의 `java-test-fixtures` 플러그인을 사용해, **테스트용 공용 리소스 모듈**을 만듭니다.
- 예:
  - `domain` 모듈에 `testFixtures` 소스셋을 추가하고, 그 안에 `givenProductHistory` 등을 정의.
  - 다른 모듈에서는:

```kotlin
dependencies {
    testImplementation(testFixtures(project(":domain")))
}
```

- 이렇게 하면:
  - 여러 모듈이 공통 테스트 헬퍼/픽스처를 **중복 없이** 사용할 수 있습니다.
  - Given 코드가 한 곳에서 관리되어 재사용성과 일관성이 크게 좋아집니다.

---

## 4. 외부 인프라 의존으로 인한 Mocking 지옥

### 4.1 문제

- 예시: 상품 적용일이 시작되면 담당자에게 이메일을 보내는 기능.
  - 외부 이메일 서버에 HTTP POST 요청을 보내고,
  - Request Body로 `ProductHistory`와 필드가 12개 정도 겹치는 15개 이상의 필드를 가진
    `ProductChangeNotificationRequest` 를 전송한다고 가정합니다.

- 테스트에서 이 외부 호출을 Mocking하려면:
  - 긴 Request 객체를 매번 생성해야 하고,
  - `ProductHistory`와 `ProductChangeNotificationRequest` 사이의 필드 매핑도 일일이 맞춰야 합니다.

```kotlin
given(
    emailSender.sendProductChangeNotificationEmail(
        ProductChangeNotificationRequest(
            effectiveStartDate = LocalDate.of(2024, 5, 1),
            effectiveEndDate = LocalDate.of(2025, 1, 1),
            productId = productHistory.productId,
            productName = productHistory.productName,
            category = productHistory.category,
            // ... 나머지 여러 필드들
        )
    )
)
```

- 이런 코드가 테스트마다 등장하면:
  - Mocking 관련 코드량이 테스트 본문을 압도하고
  - 필드 매핑 실수 가능성도 높아집니다.

### 4.2 개선 방향 (요지)

- 외부 인프라에 대한 Mocking도 **테스트 헬퍼/팩토리**로 감춥니다.
  - 예: `givenNotificationRequest(productHistory, ...)` 같은 함수를 통해
    - 중복 필드 매핑·기본값을 한 곳에서 처리.
- 또는 이메일 전송 책임을 별도 객체로 분리하여
  - 테스트에서는 더 작은 인터페이스·단순한 파라미터만 Mocking하도록 설계를 정리합니다.

핵심은:
- Given 절에서 외부 인프라와 직접 맞붙기보다,
  - **도메인 모델/값 객체/헬퍼로 한 번 더 감싸서** 테스트를 간결하게 유지하는 것입니다.

---

## 5. Given 지옥에서 벗어나는 전략 요약

글에서 제안하는 핵심 전략은 다음 네 가지입니다.

1. **테스트 데이터 빌더/헬퍼 (`given*` 함수)**
   - 필수 차이점만 인자로 받고, 나머지는 일관된 기본값으로 채워주는 헬퍼 도입.

2. **Gradle `java-test-fixtures` 로 공용 테스트 리소스 모듈 구성**
   - 멀티 모듈 환경에서 테스트 헬퍼/픽스처를 중복 없이 재사용.

3. **외부 인프라 의존도 캡슐화**
   - 외부 시스템 Request/Response와의 직접적인 매핑 코드를 헬퍼/Adapter로 감추고,
   - 테스트에서는 더 작은 인터페이스와 값 객체를 다루도록 설계.

4. **Given 작성 편의성을 설계 피드백으로 활용**
   - Given 절이 지나치게 길고 복잡하다면,
   - “테스트 도구 부족 + 설계 개선 필요”의 신호로 받아들이고,
   - 테스트 헬퍼 도입/모듈 구조 정리/책임 분산 등을 검토합니다.

---

## 6. Given 작성 편의가 주는 이점과 마무리

글의 결론은, **Given 작성이 편해지면 테스트 코드 전체의 생태계가 좋아진다**는 것입니다.

- Given이 간결하고 재사용 가능해지면:
  - 새로운 테스트 케이스를 추가하는 부담이 줄고
  - 더 많은 시나리오를 자연스럽게 커버하게 됩니다.
  - 테스트 코드가 “투자 대비 가치 있는 자산”으로 성장합니다.

- 작성자는 이를 **“스노우 볼을 굴린다”** 고 표현합니다.
  - 작은 개선(헬퍼 하나, fixture 모듈 하나)에서 시작해도,
  - 시간이 지날수록 테스트 작성이 쉬워지고,
  - 더 많은 테스트가 설계·품질 향상에 기여하게 됩니다.

우리 프로젝트에도:
- 공통 `given*` 헬퍼 도입
- 테스트용 fixture 모듈 분리
- 외부 인프라 의존 코드의 캡슐화
를 차근차근 적용해, Given 지옥을 줄이고 “테스트 친화적인” 구조를 만들어갈 수 있습니다.

