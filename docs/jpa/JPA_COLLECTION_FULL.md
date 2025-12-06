# ElementCollection vs Embedded vs OneToMany — 완전 정리 문서

본 문서는 JPA에서 자주 혼동되는 세 가지 매핑 방식  
**@ElementCollection**, **@Embedded**, **@OneToMany**  
에 대해 실무 기준으로 가장 명확하고 흐름 있게 정리한 기술 문서이다.

---

# 1. 전체 개념 비교표

| 항목 | **@ElementCollection** | **@Embedded / @Embeddable** | **@OneToMany** |
|------|--------------------------|------------------------------|------------------|
| 저장 대상 | 값 타입 컬렉션 | 단일 값 타입 객체 | 엔티티 컬렉션 |
| 별도 테이블 생성 | O | X (부모 테이블에 포함) | O |
| PK 존재 여부 | 없음 | 없음 | 있음 |
| 독립적 생명주기 | 없음 | 없음 | 있음 |
| 변경 시 동작 | 전체 삭제 후 재삽입 | 부모만 update | 엔티티 단위 update |
| 로딩 전략 | Lazy (문제 많음) | 즉시 포함 | Lazy |
| N+1 위험 | 있음 | 없음 | 있음 |
| 검색 조건 사용 | 매우 제한적 | 불가능 | 매우 강력 |
| DTO Projection / QueryDSL | 어려움 | 불필요 | 가능 |
| 사용 시나리오 | 단순 값 목록, 변경 적음 | 구성 값 묶음 | 대부분의 컬렉션 |
| 실무 선호도 | 낮음 | 중간 | 매우 높음 |

---

# 2. 세 방식의 개념적 차이

## 2.1 @Embedded / @Embeddable
**단일 객체가 여러 필드를 묶는 경우** 사용한다.

예:
- 주소(Address: zipcode, street, detail)
- 좌표(Coordinate: lat, lng)
- 기간(Period: startDate, endDate)

특징:
- 부모 엔티티 테이블에 바로 포함되는 구조
- Lazy 로딩 없음 → JSON 변환 시 문제 無
- 컬렉션 불가능 (Embedded는 단일 객체 표현)

사용 예시:

```java
@Embeddable
public class Address {
    private String zipcode;
    private String street;
    private String detail;
}

@Entity
public class Member {
    @Embedded
    private Address address;
}
```

**언제 쓰면 좋은가?**
- 하나의 개념을 하나의 값 객체로 묶고 싶을 때
- 재사용되는 값 객체가 있을 때
- 별도 테이블을 만들 필요가 없을 때

---

## 2.2 @ElementCollection
**엔티티가 아닌 값들의 리스트 컬렉션을 별도 테이블에 저장하고 싶을 때** 사용한다.

예:
- 유저 관심사 목록(List<String>)
- 상품 색상 목록(Set<String>)
- 변경이 거의 없는 단순 문자열 목록

구조 예시:

```java
@ElementCollection
@CollectionTable(
    name = "post_tag",
    joinColumns = @JoinColumn(name = "post_id")
)
@Column(name = "tag")
private List<String> tags;
```

ElementCollection은 테이블을 자동 생성하여:

```
post_tag
---------
post_id (FK)
tag
```

이렇게 매핑된다.

### 중요한 특징
- **엔티티가 아니다 → ID 없음**
- **부분 수정 불가능 → 전체 삭제 후 재삽입**
- Lazy 로딩 문제 많음 → LazyInitializationException 빈번
- 검색 성능이 매우 떨어짐
- JOIN 최적화 거의 불가

**언제만 쓰면 좋은가?**
- 단순 값이고
- 변경이 거의 없고
- 검색 조건이 필요 없는 경우

대부분의 실무 API에서는 적합하지 않으므로  
**신중하게 선택해야 할 기능**이다.

---

## 2.3 @OneToMany
가장 일반적인 “컬렉션 엔티티” 매핑.

예:
- 게시글(Post) — 댓글(Comment)
- 주문(Order) — 주문상품(OrderItem)
- 게시글(Post) — 태그(PostTag)
- 사용자(User) — 알림(Notification)

특징:
- 엔티티이므로 ID 존재
- 변경 시 부분 업데이트 가능
- QueryDSL / fetch join / EntityGraph 등 최적화 가능
- 대규모 서비스에서 가장 선호됨

예시:

```java
@OneToMany(mappedBy = "post", cascade = CascadeType.ALL)
private List<PostTag> tags = new ArrayList<>();
```

실무에서는 컬렉션이 조금만 복잡하면  
**99% OneToMany 또는 별도 엔티티 설계가 정답**이다.

---

# 3. 실무 기준 선택 기준

## 3.1 @Embedded를 선택해야 하는 경우
- 하나의 값 객체가 여러 필드를 의미적 단위로 묶는 경우  
(예: 주소, GPS 좌표)
- 컬렉션이 아니라 단일 데이터일 때
- 부모 엔티티에 속하는 단순 구성요소일 때

즉, **상태를 묶어 표현하는 Value Object**를 다룰 때.

---

## 3.2 @ElementCollection을 선택해야 하는 경우
- 엔티티로 만들 만큼 무겁지 않은 단순 값 목록
- 변경이 거의 없고 조회만 필요한 경우
- 서로 관계가 필요 없고 검색 조건도 거의 없는 경우

예:
- 단순 문자열 태그 목록 (단 변경이 없을 때만)
- 단순 옵션값
- 거의 변하지 않는 설정값 리스트

---

## 3.3 @OneToMany를 선택해야 하는 경우 (대부분)
- 컬렉션의 변경이 잦다
- 검색 조건이 필요하다
- 다른 엔티티와 연결할 수도 있다
- 중복을 관리해야 한다
- 대규모 트래픽에서 성능이 중요하다
- 조회 최적화(fetch join, projection 등)가 필요하다

실무에서는 **대부분 @OneToMany를 선택한다.**

---

# 4. 의사결정 흐름도 (실무용)

## Step 1. 값인가 엔티티인가?
- 검색해야 한다 → 엔티티 → OneToMany  
- 변경이 잦다 → 엔티티 → OneToMany  
- 외부 키 필요, 중복 제거 필요 → 엔티티 → OneToMany  
- 별도 생명주기 필요 → 엔티티 → OneToMany  
- 그렇지 않다면 → 값 타입

## Step 2. 값 타입이면?
- 단일 객체 → Embedded  
- 값 목록 → ElementCollection (단, 변경 적을 때만)

## Step 3. 조회 최적화 필요?
→ 무조건 OneToMany

---

# 5. 잘못된 실무 예시와 수정 방향

## 잘못된 사례
“게시글-태그(Post.tags)를 ElementCollection으로 구현”

문제:
- 태그 변경 시 전체 삭제 후 재삽입
- LazyInitializationException 빈번
- 태그 검색(join) 불리
- 대규모 서비스에서 N+1 폭발

## 실무 정답: 별도 엔티티(PostTag)로 분리

```java
@Entity
public class PostTag {
    @Id @GeneratedValue
    private Long id;

    @ManyToOne(fetch = LAZY)
    private Post post;

    @ManyToOne(fetch = LAZY)
    private Tag tag;
}
```

장점:
- 검색 최적화 가능
- 변경 비용 저렴
- QueryDSL 활용 가능
- 대규모 트래픽 환경에서 안정적

---

# 6. 최종 실무 요약

| 상황 | 추천 방식 |
|------|------------|
| 단일 객체(주소, 좌표 등) | **@Embedded** |
| 단순 값 리스트, 변경 거의 없음 | **@ElementCollection** |
| 컬렉션 + 검색 + 변경 잦음 | **@OneToMany** |
| API 응답 최적화 필요 | **@OneToMany + fetch join 또는 Projection** |
| 대규모 서비스 | **거의 항상 @OneToMany** |

즉, 실무에서의 정답은 다음과 같다:

**"구체적이고 복잡한 컬렉션은 무조건 OneToMany,  
단순하고 변경 거의 없고 값만 필요하면 ElementCollection,  
단일 값 묶음은 Embedded."**

---

# 7. 부록: 세 방식 비교 시각화 요약

- Embedded = 객체 묶음(컬럼 합체)
- ElementCollection = 값들의 리스트(별도 테이블, 값 타입)
- OneToMany = 엔티티 컬렉션(가장 유연하고 강력)

---

문서 끝.
