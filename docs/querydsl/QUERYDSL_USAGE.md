# QueryDSL 사용 가이드

## 개요
이 프로젝트에 QueryDSL을 적용하여 타입 안전한 쿼리 작성이 가능합니다.

## 설정 완료 항목

### 1. 의존성 추가 (build.gradle)
```gradle
// QueryDSL
implementation 'com.querydsl:querydsl-jpa:5.0.0:jakarta'
annotationProcessor "com.querydsl:querydsl-apt:5.0.0:jakarta"
annotationProcessor "jakarta.annotation:jakarta.annotation-api"
annotationProcessor "jakarta.persistence:jakarta.persistence-api"
```

### 2. Q-class 생성 설정
- 위치: `build/generated/querydsl`
- 빌드 시 자동 생성됨

### 3. QueryDSL Configuration
- [QueryDslConfig.java](../src/main/java/com/devon/techblog/common/config/QueryDslConfig.java)
- JPAQueryFactory 빈 등록

## 구현된 Custom Repository

### PostRepositoryCustom
복잡한 게시글 조회 쿼리를 처리합니다.

**주요 메서드:**
- `findAllWithMember(Pageable)`: 게시글 목록 조회 (Member와 fetch join, 페이징)
- `findAllActiveWithMember(Pageable)`: 삭제되지 않은 게시글 목록 조회
- `findByIdWithMember(Long)`: 게시글 단건 조회 (Member와 fetch join)
- `findByMemberId(Long)`: 특정 회원의 게시글 목록 조회
- `searchByTitleOrContent(String, Pageable)`: 제목/내용으로 게시글 검색

**사용 예시:**
```java
@Service
@RequiredArgsConstructor
public class PostService {
    private final PostRepository postRepository;

    public Page<Post> getActivePostsWithMember(Pageable pageable) {
        // N+1 문제 없이 Member와 함께 조회
        return postRepository.findAllActiveWithMember(pageable);
    }

    public Page<Post> searchPosts(String keyword, Pageable pageable) {
        // 제목 또는 내용으로 검색
        return postRepository.searchByTitleOrContent(keyword, pageable);
    }
}
```

### CommentRepositoryCustom
댓글 관련 복잡한 쿼리를 처리합니다.

**주요 메서드:**
- `findByPostIdWithMember(Long)`: 특정 게시글의 댓글 목록 (Member와 fetch join)
- `countCommentsByPostIds(List<Long>)`: 여러 게시글의 댓글 수를 한 번의 쿼리로 조회
- `findByMemberId(Long)`: 특정 회원의 댓글 목록 조회

**사용 예시:**
```java
@Service
@RequiredArgsConstructor
public class PostService {
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;

    public PostListResponse getPosts(Pageable pageable) {
        // 1. 게시글 목록 조회 (Member와 함께)
        Page<Post> posts = postRepository.findAllActiveWithMember(pageable);

        // 2. 댓글 수를 한 번의 쿼리로 조회 (기존 N번 -> 1번)
        List<Long> postIds = posts.stream()
            .map(Post::getId)
            .toList();
        Map<Long, Long> commentCountMap =
            commentRepository.countCommentsByPostIds(postIds);

        // 3. 응답 생성
        return PostListResponse.of(posts, commentCountMap);
    }
}
```

## QueryDSL의 장점

### 1. N+1 문제 해결
```java
// 기존 방식 (N+1 문제 발생)
List<Post> posts = postRepository.findAll(); // 1번 쿼리
for (Post post : posts) {
    post.getMember().getName(); // N번 추가 쿼리
}

// QueryDSL fetch join (1번 쿼리로 해결)
Page<Post> posts = postRepository.findAllWithMember(pageable);
```

### 2. 타입 안전성
```java
// 컴파일 타임에 오류 검출
queryFactory
    .selectFrom(post)
    .where(post.title.containsIgnoreCase(keyword)) // 필드명 오타 시 컴파일 에러
    .fetch();
```

### 3. 동적 쿼리 작성 용이
```java
private BooleanExpression titleContains(String keyword) {
    return keyword != null ? post.title.containsIgnoreCase(keyword) : null;
}

// where 절에 null은 자동으로 무시됨
queryFactory
    .selectFrom(post)
    .where(
        titleContains(keyword),
        contentContains(keyword2)
    )
    .fetch();
```

### 4. 페이징 최적화
```java
// count 쿼리를 별도로 최적화 가능
JPAQuery<Long> countQuery = queryFactory
    .select(post.count())
    .from(post)
    .where(post.isDeleted.eq(false));

return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
```

## 빌드 및 실행

### Q-class 생성
```bash
./gradlew clean compileJava
```

### 전체 빌드
```bash
./gradlew clean build
```

### IDE에서 Q-class 인식 안 될 때
1. Gradle Refresh (IntelliJ: Gradle 탭 -> Reload)
2. 또는 `./gradlew clean compileJava` 실행 후 IDE 재시작

## 추가 개선 가능 항목

1. **MemberRepository에도 QueryDSL 적용**
   - 회원 검색 기능
   - 회원 통계 조회

2. **Projection 활용**
   - DTO로 바로 조회하여 성능 최적화
   ```java
   List<PostSummaryDto> results = queryFactory
       .select(Projections.constructor(PostSummaryDto.class,
           post.id,
           post.title,
           post.member.nickname,
           post.likeCount
       ))
       .from(post)
       .fetch();
   ```

3. **동적 정렬 조건**
   - 사용자가 선택한 정렬 조건에 따라 동적으로 정렬

4. **복잡한 통계 쿼리**
   - 일별/월별 게시글 통계
   - 인기 게시글 조회

## 참고 자료
- [QueryDSL 공식 문서](http://www.querydsl.com/)
- [Spring Data JPA + QueryDSL](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#core.extensions)
