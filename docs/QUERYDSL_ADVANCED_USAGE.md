# QueryDSL 고급 사용법

## 1. 동적 정렬 (Dynamic Sorting)

### 개요
클라이언트가 API 호출 시 정렬 조건을 동적으로 지정할 수 있도록 지원합니다.

### 화이트리스트 방식 정렬

```java
// PostRepositoryImpl.java
private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
    "id",
    "title",
    "viewsCount",
    "likeCount",
    "createdAt",
    "updatedAt"
);

@Override
public Page<Post> findAllWithMember(Pageable pageable) {
    // 동적 정렬 적용 (기본값: createdAt desc)
    OrderSpecifier<?>[] orders = QueryDslOrderUtil.getOrderSpecifiersWithDefault(
            pageable,
            post,
            ALLOWED_SORT_FIELDS,
            post.createdAt.desc()  // 기본 정렬
    );

    List<Post> content = queryFactory
            .selectFrom(post)
            .join(post.member, member).fetchJoin()
            .orderBy(orders)  // 동적 정렬 적용
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();

    JPAQuery<Long> countQuery = queryFactory
            .select(post.count())
            .from(post);

    return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
}
```

### API 사용 예시

```bash
# 최신순 정렬 (기본값)
GET /posts?page=0&size=10

# 좋아요 많은 순
GET /posts?page=0&size=10&sort=likeCount,desc

# 조회수 많은 순
GET /posts?page=0&size=10&sort=viewsCount,desc

# 다중 정렬: 좋아요 많은 순 -> 최신순
GET /posts?page=0&size=10&sort=likeCount,desc&sort=createdAt,desc

# 제목 가나다순
GET /posts?page=0&size=10&sort=title,asc
```

### Controller 예시

```java
@GetMapping
public ResponseEntity<PostListResponse> getPosts(
        @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
        Pageable pageable
) {
    // Pageable에 정렬 정보가 포함되어 있음
    // Repository에서 자동으로 처리
    Page<Post> posts = postRepository.findAllActiveWithMember(pageable);
    return ResponseEntity.ok(PostListResponse.from(posts));
}
```

### 보안 고려사항

**화이트리스트를 사용하는 이유:**

1. **악의적인 필드 접근 방지**
   ```bash
   # 차단됨: member.password 같은 민감한 필드
   GET /posts?sort=member.password,desc
   ```

2. **존재하지 않는 필드 접근 방지**
   ```bash
   # 차단됨: 존재하지 않는 필드
   GET /posts?sort=nonExistentField,desc
   ```

3. **성능 저하 방지**
   - 인덱스가 없는 필드로 정렬 방지
   - TEXT 타입 같은 대용량 필드 정렬 방지

## 2. 필드명 매핑 (Field Mapping)

API 필드명과 엔티티 필드명이 다른 경우:

```java
// 필드명 매핑 테이블 정의
private static final Map<String, String> SORT_FIELD_MAPPING = Map.of(
    "likes", "likeCount",           // API: likes -> Entity: likeCount
    "views", "viewsCount",          // API: views -> Entity: viewsCount
    "created", "createdAt",         // API: created -> Entity: createdAt
    "author", "member.nickname"     // API: author -> Entity: member.nickname
);

// 사용
OrderSpecifier<?>[] orders = QueryDslOrderUtil.getOrderSpecifiers(
        pageable,
        post,
        SORT_FIELD_MAPPING
);
```

**사용 예시:**
```bash
# API 필드명으로 정렬 가능
GET /posts?sort=likes,desc    # 내부적으로 likeCount로 변환
GET /posts?sort=views,desc    # 내부적으로 viewsCount로 변환
```

## 3. 공통 유틸리티 활용

### Null-safe BooleanExpression

```java
// QueryDslSupport의 nullSafeBuilder 사용
protected BooleanExpression buildSearchCondition(
        String title,
        String content,
        Long memberId
) {
    return nullSafeBuilder(
        titleContains(title),        // null이면 무시
        contentContains(content),    // null이면 무시
        memberIdEq(memberId)         // null이면 무시
    );
}

// 사용 예시
public Page<Post> searchPosts(
        String title,
        String content,
        Long memberId,
        Pageable pageable
) {
    BooleanExpression condition = buildSearchCondition(title, content, memberId);

    return queryFactory
            .selectFrom(post)
            .where(condition)  // null-safe 조건
            .fetch();
}
```

### 빈 문자열 처리

```java
// 빈 문자열을 null로 변환
String normalizedKeyword = emptyToNull(keyword);

private BooleanExpression titleContains(String keyword) {
    keyword = emptyToNull(keyword);  // "" -> null 변환
    return keyword != null ? post.title.containsIgnoreCase(keyword) : null;
}
```

## 4. 실전 예제

### 복잡한 검색 쿼리

```java
public Page<Post> searchPostsAdvanced(
        String keyword,
        Long memberId,
        Integer minLikes,
        LocalDateTime startDate,
        LocalDateTime endDate,
        Pageable pageable
) {
    // 동적 정렬
    OrderSpecifier<?>[] orders = QueryDslOrderUtil.getOrderSpecifiersWithDefault(
            pageable,
            post,
            ALLOWED_SORT_FIELDS,
            post.createdAt.desc()
    );

    // 동적 조건
    BooleanExpression condition = nullSafeBuilder(
        searchKeyword(keyword),
        memberIdEq(memberId),
        likeCountGoe(minLikes),
        createdAtBetween(startDate, endDate),
        post.isDeleted.eq(false)  // 항상 적용되는 조건
    );

    List<Post> content = queryFactory
            .selectFrom(post)
            .join(post.member, member).fetchJoin()
            .where(condition)
            .orderBy(orders)
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();

    JPAQuery<Long> countQuery = queryFactory
            .select(post.count())
            .from(post)
            .where(condition);

    return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
}

// 조건 메서드들
private BooleanExpression searchKeyword(String keyword) {
    keyword = emptyToNull(keyword);
    return keyword != null
        ? post.title.containsIgnoreCase(keyword)
            .or(post.content.containsIgnoreCase(keyword))
        : null;
}

private BooleanExpression memberIdEq(Long memberId) {
    return memberId != null ? post.member.id.eq(memberId) : null;
}

private BooleanExpression likeCountGoe(Integer minLikes) {
    return minLikes != null ? post.likeCount.goe(minLikes.longValue()) : null;
}

private BooleanExpression createdAtBetween(LocalDateTime start, LocalDateTime end) {
    if (start != null && end != null) {
        return post.createdAt.between(start, end);
    } else if (start != null) {
        return post.createdAt.goe(start);
    } else if (end != null) {
        return post.createdAt.loe(end);
    }
    return null;
}
```

### API 호출 예시

```bash
# 모든 조건 적용
GET /posts/search?keyword=spring&memberId=1&minLikes=10&startDate=2024-01-01T00:00:00&endDate=2024-12-31T23:59:59&sort=likeCount,desc

# 키워드만 검색
GET /posts/search?keyword=spring

# 특정 회원의 게시글 + 인기순 정렬
GET /posts/search?memberId=1&sort=likeCount,desc

# 기간 검색 + 최신순
GET /posts/search?startDate=2024-10-01T00:00:00&sort=createdAt,desc
```

## 5. 성능 최적화 팁

### 1. Count 쿼리 최적화

```java
// Bad: Join을 포함한 count 쿼리
JPAQuery<Long> countQuery = queryFactory
    .select(post.count())
    .from(post)
    .join(post.member, member)  // 불필요한 join
    .where(condition);

// Good: Join 제거
JPAQuery<Long> countQuery = queryFactory
    .select(post.count())
    .from(post)
    .where(condition);  // join 없이 count만 수행
```

### 2. Fetch Join 위치

```java
// Content 쿼리에만 fetch join 적용
List<Post> content = queryFactory
    .selectFrom(post)
    .join(post.member, member).fetchJoin()  // 여기만 필요
    .where(condition)
    .orderBy(orders)
    .fetch();

// Count 쿼리는 join 불필요
JPAQuery<Long> countQuery = queryFactory
    .select(post.count())
    .from(post)
    .where(condition);  // join 없음
```

### 3. 커버링 인덱스 활용

```java
// 필요한 필드만 조회
List<PostSummaryDto> results = queryFactory
    .select(Projections.constructor(PostSummaryDto.class,
        post.id,
        post.title,
        post.likeCount,
        post.viewsCount,
        post.createdAt,
        member.nickname
    ))
    .from(post)
    .join(post.member, member)
    .where(condition)
    .orderBy(orders)
    .fetch();
```

## 6. 테스트 예제

```java
@Test
@DisplayName("동적 정렬 테스트 - 좋아요순")
void dynamicSortByLikeCount() {
    // given
    Pageable pageable = PageRequest.of(0, 10,
        Sort.by(Sort.Direction.DESC, "likeCount"));

    // when
    Page<Post> result = postRepository.findAllActiveWithMember(pageable);

    // then
    assertThat(result.getContent()).isSortedAccordingTo(
        Comparator.comparing(Post::getLikeCount).reversed()
    );
}

@Test
@DisplayName("허용되지 않은 필드 정렬 시도 - 무시됨")
void sortByDisallowedField() {
    // given
    Pageable pageable = PageRequest.of(0, 10,
        Sort.by(Sort.Direction.DESC, "password"));  // 화이트리스트에 없음

    // when
    Page<Post> result = postRepository.findAllActiveWithMember(pageable);

    // then
    // 기본 정렬(createdAt desc)로 조회됨
    assertThat(result.getContent()).isSortedAccordingTo(
        Comparator.comparing(Post::getCreatedAt).reversed()
    );
}
```

## 7. 주의사항

### 1. N+1 문제 방지

```java
// Bad
List<Post> posts = queryFactory
    .selectFrom(post)
    .fetch();
// posts.getMember().getName() 호출 시 N+1 발생

// Good
List<Post> posts = queryFactory
    .selectFrom(post)
    .join(post.member, member).fetchJoin()  // fetch join 사용
    .fetch();
```

### 2. MultipleBagFetchException 주의

```java
// Bad: 두 개 이상의 컬렉션 fetch join
queryFactory
    .selectFrom(post)
    .join(post.comments, comment).fetchJoin()
    .join(post.attachments, file).fetchJoin()  // 에러!
    .fetch();

// Good: 한 번에 하나의 컬렉션만 fetch join
// 나머지는 batch size 설정으로 해결
queryFactory
    .selectFrom(post)
    .join(post.comments, comment).fetchJoin()
    .fetch();
```

### 3. 페이징과 컬렉션 fetch join

```java
// Bad: 페이징 + 컬렉션 fetch join (메모리에서 페이징)
queryFactory
    .selectFrom(post)
    .join(post.comments, comment).fetchJoin()
    .offset(0)
    .limit(10)
    .fetch();  // 경고 발생

// Good: 컬렉션은 별도 쿼리로 조회
List<Post> posts = queryFactory
    .selectFrom(post)
    .offset(0)
    .limit(10)
    .fetch();

// 별도로 댓글 조회
Map<Long, Long> commentCount =
    commentRepository.countCommentsByPostIds(postIds);
```

## 요약

1. **동적 정렬**: 화이트리스트 방식으로 안전하게 구현
2. **공통 유틸리티**: Null-safe 조건, 빈 문자열 처리
3. **성능 최적화**: Count 쿼리 최적화, Fetch Join 위치
4. **보안**: 허용된 필드만 정렬 가능
5. **유연성**: 클라이언트가 다양한 정렬 조건 지정 가능
