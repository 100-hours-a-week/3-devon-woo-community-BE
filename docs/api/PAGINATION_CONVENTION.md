# 페이징 및 정렬 컨벤션

## 목차
1. [개요](#개요)
2. [아키텍처 결정 (ADR)](#아키텍처-결정-adr)
3. [구현 가이드](#구현-가이드)
4. [사용 예시](#사용-예시)
5. [설계 원칙](#설계-원칙)

---

## 개요

본 프로젝트에서는 페이징 및 정렬 기능을 일관되고 재사용 가능한 방식으로 구현하기 위해 공통 컴포넌트를 제공합니다.

### 핵심 컴포넌트

- **`PageSortRequest`**: 페이징 및 정렬 요청을 위한 공통 DTO
- **`PageResponse<T>`**: 페이징 응답을 위한 제네릭 DTO
- **`PaginationConstants`**: 페이징 관련 상수 관리

---

## 아키텍처 결정 (ADR)

### ADR-001: 공통 페이징 요청 DTO 사용

**상황**
- 여러 도메인에서 페이징 및 정렬 기능이 필요
- 각 Controller마다 개별 파라미터로 받으면 중복 코드 발생

**결정**
- `PageSortRequest` 공통 DTO를 사용하여 페이징/정렬 파라미터를 통합 관리

**근거**
- 코드 중복 제거
- 일관된 페이징 정책 적용
- 기본값을 중앙에서 관리 가능

**대안**
- Spring의 `@PageableDefault` 사용: 커스텀 검증 로직 추가가 어려움
- 개별 파라미터 사용: 중복 코드 발생

---

### ADR-002: Pageable 변환은 Controller에서 수행

**상황**
- `PageSortRequest`를 Spring Data의 `Pageable`로 변환하는 책임 위치 결정

**결정**
- Controller에서 `PageSortRequest.toPageable()`을 호출하여 변환
- Service는 `Pageable`만 의존

**근거**
- **레이어 분리**: Controller는 HTTP 요청을 도메인 계층이 이해할 수 있는 형태로 변환하는 책임
- **Service 독립성**: Service는 웹 프레임워크와 독립적이어야 함
- **재사용성**: Service는 표준 `Pageable` 인터페이스만 받아 재사용성 향상

**대안**
- Service에서 변환: Service가 웹 요청 DTO에 의존하게 되어 레이어 경계 위반

---

### ADR-003: 제네릭 기반 공통 PageResponse 사용

**상황**
- 각 도메인마다 `PostListResponse`, `CommentListResponse` 등 개별 응답 DTO 생성 시 중복 발생

**결정**
- `PageResponse<T>` 제네릭 DTO를 사용하여 모든 페이징 응답 통합

**근거**
- DRY 원칙: 페이징 메타데이터 로직을 한 곳에서 관리
- 타입 안정성: 제네릭으로 컴파일 타임 타입 체크
- 일관성: 모든 API가 동일한 페이징 응답 구조 사용

**대안**
- 도메인별 개별 Response: 중복 코드 발생, 일관성 저하
- Record 상속: Record는 final이라 상속 불가능

---

### ADR-004: 페이징 응답 필드 최소화

**상황**
- 페이징 응답에 많은 메타데이터 포함 여부 결정

**결정**
- 5개의 핵심 필드만 포함: `items`, `page`, `size`, `totalElements`, `totalPages`

**근거**
- **실용성**: 대부분의 실무 프로젝트에서 이 5개 필드면 충분
- **네트워크 효율성**: 불필요한 데이터 전송 방지
- **클라이언트 책임**: `hasNext`, `hasPrevious` 등은 클라이언트에서 쉽게 계산 가능
  - `first` → `page === 0`
  - `last` → `page === totalPages - 1`
  - `hasNext` → `page < totalPages - 1`
  - `hasPrevious` → `page > 0`

**제거한 필드**
- `first`, `last`, `hasNext`, `hasPrevious`

---

### ADR-005: 페이징 상수는 별도 클래스로 관리

**상황**
- 페이징 기본값(기본 페이지, 크기, 정렬)을 관리할 위치 결정

**결정**
- `PaginationConstants` 클래스에서 중앙 집중식 관리

**근거**
- **중앙 관리**: 기본값 변경 시 한 곳만 수정
- **재사용성**: 다른 컴포넌트에서도 상수 참조 가능
- **매직 넘버/스트링 제거**: 하드코딩 방지

---

## 구현 가이드

### 1. 페이징 상수 정의

```java
// PaginationConstants.java
public final class PaginationConstants {

    private PaginationConstants() {}

    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_SIZE = 20;
    public static final String DEFAULT_SORT = "createdAt,desc";
}
```

### 2. 페이징 요청 DTO

```java
// PageSortRequest.java
public record PageSortRequest(
        Integer page,
        Integer size,
        List<String> sort
) {
    public PageSortRequest {
        page = (page != null && page >= 0) ? page : PaginationConstants.DEFAULT_PAGE;
        size = (size != null && size > 0) ? size : PaginationConstants.DEFAULT_SIZE;
        sort = (sort != null && !sort.isEmpty()) ? sort : List.of(PaginationConstants.DEFAULT_SORT);
    }

    public Pageable toPageable() {
        return PageRequest.of(page, size, parseSort());
    }

    private Sort parseSort() {
        List<Sort.Order> orders = new ArrayList<>();

        for (String sortParam : sort) {
            String[] parts = sortParam.split(",");
            String property = parts[0].trim();
            String direction = (parts.length > 1) ? parts[1].trim() : "asc";

            orders.add(new Sort.Order(
                    direction.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC,
                    property
            ));
        }

        return Sort.by(orders);
    }
}
```

### 3. 페이징 응답 DTO

```java
// PageResponse.java
public record PageResponse<T>(
        List<T> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static <T> PageResponse<T> of(List<T> items, Page<?> page) {
        return new PageResponse<>(
                items,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
```

---

## 사용 예시

### Controller 레이어

```java
@GetMapping
public ApiResponse<PageResponse<PostSummaryResponse>> getPostPage(
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size,
        @RequestParam(required = false) List<String> sort
) {
    PageSortRequest request = new PageSortRequest(page, size, sort);
    PageResponse<PostSummaryResponse> response = postService.getPostPage(request.toPageable());
    return ApiResponse.success(response, "posts_retrieved");
}
```

### Service 레이어

```java
public PageResponse<PostSummaryResponse> getPostPage(Pageable pageable) {
    Page<PostSummaryDto> postDtoPage = postRepository.findAllActiveWithMemberAsDto(pageable);

    // 비즈니스 로직...
    List<PostSummaryResponse> postSummaries = ...;

    return PageResponse.of(postSummaries, postDtoPage);
}
```

### Repository 레이어

```java
public interface PostRepository extends JpaRepository<Post, Long> {
    Page<PostSummaryDto> findAllActiveWithMemberAsDto(Pageable pageable);
}
```

---

## API 사용 예시

### 기본 요청 (기본값 적용)
```bash
GET /api/v1/posts
# page=0, size=20, sort=createdAt,desc
```

### 페이지 지정
```bash
GET /api/v1/posts?page=2&size=10
```

### 정렬 지정
```bash
# 좋아요 많은 순
GET /api/v1/posts?page=0&size=10&sort=likeCount,desc

# 조회수 많은 순
GET /api/v1/posts?page=0&size=10&sort=viewsCount,desc

# 제목 가나다순
GET /api/v1/posts?page=0&size=10&sort=title,asc
```

### 다중 정렬
```bash
# 좋아요 많은 순 → 최신순
GET /api/v1/posts?page=0&size=10&sort=likeCount,desc&sort=createdAt,desc
```

### 응답 예시

```json
{
  "success": true,
  "message": "posts_retrieved",
  "data": {
    "items": [
      {
        "postId": 1,
        "title": "게시글 제목",
        "content": "게시글 내용",
        ...
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 100,
    "totalPages": 5
  }
}
```

---

## 설계 원칙

### 1. 레이어별 책임 분리

| 레이어 | 책임 |
|--------|------|
| **Controller** | HTTP 요청 파라미터 → `PageSortRequest` → `Pageable` 변환 |
| **Service** | 비즈니스 로직, `Pageable` 받아서 `PageResponse<T>` 반환 |
| **Repository** | `Pageable` 받아서 `Page<Entity>` 반환 |

### 2. DRY (Don't Repeat Yourself)

- 페이징 로직을 공통 컴포넌트로 추출하여 중복 제거
- 모든 도메인에서 `PageResponse<T>` 재사용

### 3. 타입 안정성

- 제네릭 사용으로 컴파일 타임 타입 체크
- `PageResponse<PostSummaryResponse>`, `PageResponse<CommentResponse>` 등 명확한 타입 정의

### 4. 확장성

- 새로운 정렬 기준 추가 시 클라이언트에서 `sort` 파라미터만 변경
- 서버 코드 수정 불필요 (Repository 쿼리에서 해당 필드 지원 시)

### 5. 실용성 우선

- 실무에서 자주 사용되는 기능만 포함
- 불필요한 복잡도 제거 (boolean 플래그 등)

---

## 다른 도메인 적용 예시

### CommentService

```java
public PageResponse<CommentResponse> getCommentPage(Long postId, Pageable pageable) {
    Page<Comment> commentPage = commentRepository.findByPostId(postId, pageable);

    List<CommentResponse> comments = commentPage.getContent()
        .stream()
        .map(CommentResponse::from)
        .toList();

    return PageResponse.of(comments, commentPage);
}
```

### MemberService

```java
public PageResponse<MemberResponse> getMemberPage(Pageable pageable) {
    Page<Member> memberPage = memberRepository.findAll(pageable);

    List<MemberResponse> members = memberPage.getContent()
        .stream()
        .map(MemberResponse::from)
        .toList();

    return PageResponse.of(members, memberPage);
}
```

---

## 참고사항

### 기본값 변경

기본값을 변경하려면 `PaginationConstants` 클래스의 상수만 수정:

```java
public static final int DEFAULT_PAGE = 0;      // 첫 페이지
public static final int DEFAULT_SIZE = 20;     // 페이지당 20개
public static final String DEFAULT_SORT = "createdAt,desc"; // 최신순
```

### 커스텀 정렬 검증

특정 도메인에서 허용되는 정렬 필드를 제한하려면 Service나 Validator에서 검증:

```java
private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
    "createdAt", "likeCount", "viewsCount", "title"
);

public void validateSortFields(Pageable pageable) {
    pageable.getSort().forEach(order -> {
        if (!ALLOWED_SORT_FIELDS.contains(order.getProperty())) {
            throw new IllegalArgumentException("Invalid sort field: " + order.getProperty());
        }
    });
}
```

---

## 버전 히스토리

| 버전 | 날짜 | 변경 사항 |
|------|------|-----------|
| 1.0.0 | 2025-11-01 | 초기 작성 |

---

## 관련 문서

- [QueryDSL 고급 사용법](./QUERYDSL_ADVANCED_USAGE.md)
- [API 설계 가이드](./API_DESIGN_GUIDE.md)
