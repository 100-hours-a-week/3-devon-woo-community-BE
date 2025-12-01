# Post 삭제 시 PostTag 가 삭제되지 않는 문제 분석 및 해결 문서

**환경:** Spring Boot 3.x / Hibernate 6.x / H2 / DDL `create-drop`\
**문제:** Post 삭제 후에도 PostTag(조인 테이블) 레코드가 삭제되지 않음

------------------------------------------------------------------------

# 1. 문제 요약

테스트 코드에서는 `@OnDelete(action = CASCADE)` 와\
`ddl-auto=create-drop` 설정을 사용하고 있음에도 불구하고,\
`Post` 엔티티를 삭제하면 관련된 `PostTag` 가 자동으로 삭제되지 않았다.

테스트 로그에서는 다음과 같은 Assertion 실패가 발생했다.

    Expecting empty but was: [PostTag@xxxx, PostTag@yyyy]

의도는 다음과 같다:

-   **Post 삭제 → DB 레벨에서 PostTag 자동 삭제 (ON DELETE CASCADE)**\
-   그러나 실제로는 PostTag 가 그대로 남아있어 테스트 실패

------------------------------------------------------------------------

# 2. 우리가 기대했던 동작

### @OnDelete(CASCADE) 기대 효과

Hibernate 문서상 `@OnDelete(action = OnDeleteAction.CASCADE)` 는 다음을
의미한다.

> 엔티티 삭제 시 Hibernate 가 생성하는 DDL 에\
> 외래키에 `ON DELETE CASCADE` 옵션을 추가해\
> **DB 레벨에서 자동 삭제**가 이루어지도록 함.

즉, Post 삭제 시 DB가 PostTag를 자동으로 삭제해야 한다.

테스트 환경도 다음과 같아,\
DDL 생성이 정상적으로 이루어질 것으로 예상했다:

``` yaml
spring:
  jpa:
    hibernate:
      ddl-auto: create-drop
    database-platform: org.hibernate.dialect.H2Dialect
```

------------------------------------------------------------------------

# 3. 실제 발생한 문제

### 결론:

> **Hibernate 는 "@EmbeddedId + @MapsId 복합키 구조" 아래에서는\
> 스키마 생성 시 `ON DELETE CASCADE` 를 FK 에 넣지 못한다.\
> 따라서 DDL-auto=create-drop 이어도 FK에서 CASCADE 삭제는 생성되지
> 않는다.**

결과적으로 FK가 다음처럼 생성됨:

    foreign key (post_id) references post(id)

즉, `ON DELETE CASCADE` 없음 → DB cascade 동작 없음.

------------------------------------------------------------------------

# 4. 문제 원인 상세 분석

## 4.1 Hibernate 의 구조적 제약

PostTag 엔티티는 다음과 같은 복합키 구조를 가진다:

``` java
@EmbeddedId
private PostTagId id;

@MapsId("postId")
@ManyToOne
@JoinColumn(name = "post_id")
@OnDelete(action = OnDeleteAction.CASCADE)
private Post post;
```

### 이 구조의 문제:

-   `@EmbeddedId` + `@MapsId` 는 Hibernate 의 DDL 생성 과정에서\
    **FK 생성 로직이 단순하지 않음**
-   Hibernate 는 복합키 기반의 FK 생성 시\
    `@OnDelete` 메타데이터를 읽어 FK 옵션을 추가하는 과정이 누락됨\
-   이는 Hibernate issue tracker에도 명시된 기존 제약 사항

즉, Hibernate 자체가 이 패턴에서는 `ON DELETE CASCADE` 를 출력하지
못한다.

------------------------------------------------------------------------

## 4.2 H2Dialect 와의 조합

H2 는 ON DELETE CASCADE 를 지원하지만,\
Hibernate 가 DDL 에 CASCADE 를 넣지 않으면 당연히 적용되지 않는다.

→ 즉 H2의 문제라기보단 **Hibernate가 CASCADE 띄우지 않음**이 원인.

------------------------------------------------------------------------

## 4.3 ddl-auto=create-drop 은 문제를 해결해주지 않는다

많은 개발자들이 착각하는 부분:

> "create-drop이면 스키마 새로 생성하는데 @OnDelete 가 적용되지 않을
> 이유가 없는 것 아닌가?"

그러나 ddl-auto 옵션은 "DDL 생성 로직 실행 여부"만 결정한다.

**DDL 생성 로직 자체가 CASCADE를 누락하기 때문에 create-drop은 의미가
없다.**

------------------------------------------------------------------------

# 5. 해결 방법

## 해결책 1. DB 레벨 FK를 직접 정의 (가장 확실, 권장)

테스트 및 운영 환경 모두에서 FK를 명시적으로 설정한다.

`src/test/resources/schema.sql` 또는 DB에서 직접 다음 실행:

``` sql
ALTER TABLE post_tag DROP CONSTRAINT IF EXISTS fk_post_tag_post;

ALTER TABLE post_tag
ADD CONSTRAINT fk_post_tag_post
    FOREIGN KEY (post_id)
    REFERENCES post(id)
    ON DELETE CASCADE;
```

이렇게 하면 Post 삭제 시 PostTag가 DB에서 자동으로 삭제된다.

### 이 방식의 장점

-   Hibernate 버그/제약에 영향받지 않음
-   운영 환경에서 가장 안정적
-   PostgreSQL / MySQL / H2 모두 동일하게 동작

------------------------------------------------------------------------

## 해결책 2. 복합키를 제거하고 단일 surrogate key 사용

조인 테이블(PostTag)에 다음과 같이 단일 PK를 두면 Hibernate는 정상적으로
CASCADE DDL을 생성한다.

``` java
@Id
@GeneratedValue
private Long id;
```

일반적으로 대규모 시스템(Google/Meta/Netflix 포함)에서도\
복합키 기반의 조인 테이블은 지양하는 패턴이다.

### 단일 ID 기반 구조 예시

``` java
@Entity
class PostTag {
    @Id @GeneratedValue
    private Long id;

    @ManyToOne
    @JoinColumn(name="post_id", onDelete = CASCADE)
    private Post post;

    @ManyToOne
    @JoinColumn(name="tag_id")
    private Tag tag;
}
```

Hibernate 의 FK 생성이 단순해져\
`ON DELETE CASCADE` 적용이 잘 된다.

------------------------------------------------------------------------

## 해결책 3. JPA Cascade 로 삭제 처리

DB cascade 대신 JPA cascade 를 사용하려면:

``` java
post.addPostTag(postTag);
```

Post 엔티티의 postTags 컬렉션에 반드시 자식을 추가해야 한다.\
그러나 이 방식은 영속성 컨텍스트에 의존하며,\
대량 삭제나 성능 측면에서는 DB cascade 보다 떨어진다.

실무에서는 **DB cascade가 권장**된다.

------------------------------------------------------------------------

# 6. 최종 결론

  -----------------------------------------------------------------------
  항목                                내용
  ----------------------------------- -----------------------------------
  문제 원인                           **Hibernate 는 EmbeddedId + MapsId
                                      복합키 구조에서 @OnDelete(CASCADE)
                                      를 반영한 FK 생성이 불가능함**

  결과                                테스트 환경(create-drop)에서도 FK에
                                      ON DELETE CASCADE 가 포함되지 않음

  Post 삭제 시                        DB cascade 동작 안 함 → PostTag
                                      남음

  해결 방법                           **FK 직접 생성** 또는 **복합키
                                      제거(단일 surrogate key)**
  -----------------------------------------------------------------------

------------------------------------------------------------------------

# 7. 핵심 요약

-   @OnDelete(CASCADE) 는 "DDL 생성 시"만 효과가 있다\
-   **복합키 + @MapsId 구조에서는 Hibernate가 ON DELETE CASCADE를
    생성하지 못한다**\
-   ddl-auto=create-drop 이어도 해결되지 않는다\
-   실무에서는 FK를 직접 설정하거나 단일 PK 방식으로 전환하는 것이
    베스트

------------------------------------------------------------------------

# 8. 부록: Hibernate Issue 참고

Hibernate 공식적으로 다음 사항이 언급되어 있다:

-   복합키(Composite key) 관계에서 @OnDelete 적용이 제한됨
-   ManyToOne + @MapsId 조합에서는 FK 생성 옵션을 완전하게 제어할 수
    없음
-   해결책은 "DDL을 직접 작성하거나 surrogate key 사용"
