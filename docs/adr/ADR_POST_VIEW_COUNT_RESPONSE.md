# ADR: 비동기 조회수 증가 시 응답 값 처리

## 상태
검토 중 (Under Review)

## 컨텍스트

비동기로 조회수를 증가시킬 때, **응답으로 반환되는 조회수 값**에 대한 문제가 발생한다.

### 문제 시나리오

```java
// Controller
@GetMapping("/{postId}")
public ApiResponse<PostResponse> getPost(@PathVariable Long postId,
                                          HttpServletRequest request) {
    // 1. 게시글 조회 (현재 viewCount = 100)
    PostResponse response = postService.getPostDetails(postId);

    // 2. 비동기로 조회수 증가 요청 (viewCount 101로 증가 - 하지만 비동기!)
    ViewContext context = ViewContext.from(request, getCurrentMemberId());
    postViewService.tryIncrementViewCount(postId, context);

    // 3. 응답 반환 - viewCount는 여전히 100!
    return ApiResponse.success(response);
}
```

**문제:**
- 사용자가 조회했을 때 받는 응답의 `viewsCount`는 여전히 100
- 실제 DB에는 비동기로 101로 증가
- **사용자 입장에서는 자신의 조회가 카운트되지 않은 것처럼 보임**

## 핵심 질문

**"사용자가 자신의 조회가 반영된 조회수를 즉시 봐야 하는가?"**

## 실제 서비스 사례 분석

### 1. YouTube
**응답 값**: 이전 조회수 (비동기)
- 비디오 조회 시 표시되는 조회수는 실시간이 아님
- "조회수 100만회" 라고 표시되어도 실제로는 더 많을 수 있음
- 30초 이상 시청 후 검증 과정을 거쳐 조회수 반영
- **UX 영향**: 없음 (사용자는 자신의 조회로 인한 증가를 기대하지 않음)

### 2. Medium
**응답 값**: 이전 조회수 (비동기)
- 글 조회 시 표시되는 조회수는 약간의 지연 있음
- 페이지 새로고침 시 증가된 조회수 확인 가능
- **UX 영향**: 없음

### 3. Stack Overflow
**응답 값**: 이전 조회수 (비동기)
- 15분 단위 중복 제거이므로 즉시 증가 안함
- **UX 영향**: 없음

### 4. 네이버 블로그
**응답 값**: 이전 조회수
- 하루 단위 중복 제거
- 같은 날 재방문해도 조회수 변화 없음
- **UX 영향**: 없음 (예상된 동작)

### 5. 인스타그램, 페이스북
**응답 값**: 좋아요는 즉시, 조회수는 지연
- 좋아요/댓글: 즉시 반영 (사용자가 직접 액션)
- 조회수: 지연 반영 (수동적 액션)
- **UX 차이**: 명확한 의도가 있는 액션(좋아요)과 수동적 액션(조회)을 다르게 처리

## 핵심 인사이트

### 조회수 vs 좋아요의 차이

| 특성 | 조회수 | 좋아요 |
|------|--------|--------|
| 사용자 의도 | 수동적 (보기만 함) | 능동적 (명시적 클릭) |
| 즉시 피드백 필요성 | ❌ 낮음 | ✅ 높음 |
| 사용자 기대 | 자신의 조회 반영 기대 안함 | 즉시 반영 기대 |
| 실시간성 요구 | 낮음 | 높음 |

**결론**: 사용자는 **자신이 조회했다고 해서 조회수가 즉시 +1 되는 것을 기대하지 않는다.**

## 해결 방안

### Option 1: 이전 조회수 반환 (비동기, 최종 일관성) ⭐ 권장

**가장 실용적이고 실제 서비스에서 많이 사용하는 방식**

```java
@GetMapping("/{postId}")
public ApiResponse<PostResponse> getPost(@PathVariable Long postId,
                                          HttpServletRequest request) {
    // 1. 게시글 조회 (viewCount = 100)
    PostResponse response = postService.getPostDetails(postId);

    // 2. 비동기로 조회수 증가 (백그라운드에서 101로 증가)
    ViewContext context = ViewContext.from(request, getCurrentMemberId());
    postViewService.tryIncrementViewCount(postId, context);

    // 3. 이전 값(100) 반환
    return ApiResponse.success(response);
}
```

#### 장점
- ✅ **구현 단순**
- ✅ **성능 최고** (읽기 트랜잭션 readOnly 유지)
- ✅ **실제 서비스 검증됨** (YouTube, Medium 등)
- ✅ **사용자 경험 문제 없음**
  - 조회수는 "대략적인 인기도" 지표
  - 정확한 실시간 값이 중요하지 않음

#### 단점
- △ 방금 조회한 사용자의 카운트가 즉시 반영 안됨
  - 하지만 **사용자는 이를 기대하지 않음**

#### UX 개선 (선택적)
```javascript
// 프론트엔드에서 낙관적 업데이트
function displayPost(post) {
    // 서버 응답: viewCount = 100
    const displayCount = post.viewCount + 1;  // 화면에는 101 표시

    // 하지만 실제로는 정책에 의해 증가 안될 수도 있음 (중복 조회 등)
    // 따라서 이 방법은 권장하지 않음
}
```

**권장하지 않는 이유:**
- 정책(봇 필터링, 중복 제거)에 의해 실제로 증가 안될 수 있음
- 프론트엔드와 백엔드 로직 중복
- 새로고침 시 다시 원래 값으로 (혼란)

### Option 2: 조회수 +1 하여 반환 (낙관적 응답)

```java
@GetMapping("/{postId}")
public ApiResponse<PostResponse> getPost(@PathVariable Long postId,
                                          HttpServletRequest request) {
    PostResponse response = postService.getPostDetails(postId);

    ViewContext context = ViewContext.from(request, getCurrentMemberId());

    // 정책 검증
    boolean willIncrement = viewCountPolicy.shouldCount(postId, context);

    if (willIncrement) {
        // 비동기로 증가 요청
        postViewService.tryIncrementViewCount(postId, context);

        // 응답 값 조작 (실제 DB는 아직 100, 응답만 101)
        response = response.withIncrementedViewCount();
    }

    return ApiResponse.success(response);
}
```

#### 장점
- ✅ 사용자에게 즉시 증가된 값 표시
- ✅ 정책 검증 후 증가 여부 판단 가능

#### 단점
- ❌ **응답 값과 DB 값 불일치**
  - 응답: 101
  - DB (조회 직후): 100
  - DB (비동기 처리 후): 101
- ❌ **정책 로직 중복**
  - Controller에서 한 번 검증
  - PostViewService에서 또 한 번 검증
- ❌ **경쟁 조건 (Race Condition)**
  - 정책 검증과 실제 증가 사이에 시간 차이
  - 검증 시점에는 OK였지만 실제 증가 시점에는 실패할 수 있음
- ❌ **복잡도 증가**

### Option 3: 동기로 증가 후 반환 (즉시 일관성)

```java
@GetMapping("/{postId}")
public ApiResponse<PostResponse> getPost(@PathVariable Long postId,
                                          HttpServletRequest request) {
    // 1. 조회수 증가 (동기, 즉시 DB 반영)
    ViewContext context = ViewContext.from(request, getCurrentMemberId());
    boolean incremented = postViewService.incrementViewCountSync(postId, context);

    // 2. 증가된 값으로 조회
    PostResponse response = postService.getPostDetails(postId);

    return ApiResponse.success(response);
}
```

또는

```java
@GetMapping("/{postId}")
public ApiResponse<PostResponse> getPost(@PathVariable Long postId,
                                          HttpServletRequest request) {
    // 1. 먼저 조회 (viewCount = 100)
    PostResponse response = postService.getPostDetails(postId);

    // 2. 동기로 증가 (viewCount 101로 증가)
    ViewContext context = ViewContext.from(request, getCurrentMemberId());
    boolean incremented = postViewService.incrementViewCountSync(postId, context);

    // 3. 증가했다면 응답 값 수정
    if (incremented) {
        response = response.withIncrementedViewCount();
    }

    return ApiResponse.success(response);
}
```

#### 장점
- ✅ 응답 값과 DB 값 완벽히 일치
- ✅ 즉시 일관성 (Immediate Consistency)

#### 단점
- ❌ **성능 저하**
  - 매 조회마다 UPDATE 쿼리 동기 실행
  - 응답 시간 증가
- ❌ **readOnly 트랜잭션 사용 불가**
  - 읽기 최적화 포기
- ❌ **불필요한 실시간성**
  - 조회수는 실시간일 필요 없음
- ❌ **비동기 처리의 장점 포기**

### Option 4: WebSocket/SSE로 실시간 업데이트 (과도한 설계)

```java
// 초기 응답 (viewCount = 100)
@GetMapping("/{postId}")
public ApiResponse<PostResponse> getPost(@PathVariable Long postId) {
    return ApiResponse.success(postService.getPostDetails(postId));
}

// WebSocket으로 조회수 변경 알림
@EventListener
public void onViewCountUpdated(ViewCountUpdatedEvent event) {
    // 해당 게시글을 보고 있는 모든 클라이언트에게 업데이트 전송
    messagingTemplate.convertAndSend(
        "/topic/post/" + event.getPostId() + "/viewCount",
        event.getNewViewCount()
    );
}
```

#### 장점
- ✅ 실시간 업데이트
- ✅ 모든 사용자가 동시에 최신 조회수 확인

#### 단점
- ❌ **과도한 복잡도**
  - WebSocket 인프라 필요
  - 연결 관리 필요
- ❌ **불필요한 실시간성**
  - 조회수는 초 단위 실시간이 필요 없음
- ❌ **서버 리소스 낭비**
  - 수많은 WebSocket 연결 유지
- ❌ **현재 프로젝트에 과한 설계**

### Option 5: 클라이언트 측 주기적 폴링 (절충안)

```java
// 초기 응답 (viewCount = 100)
@GetMapping("/{postId}")
public ApiResponse<PostResponse> getPost(@PathVariable Long postId) {
    PostResponse response = postService.getPostDetails(postId);
    postViewService.tryIncrementViewCount(postId, context);  // 비동기
    return ApiResponse.success(response);
}

// 조회수만 조회하는 경량 API
@GetMapping("/{postId}/view-count")
public ApiResponse<ViewCountResponse> getViewCount(@PathVariable Long postId) {
    Long viewCount = postService.getViewCount(postId);
    return ApiResponse.success(new ViewCountResponse(viewCount));
}
```

```javascript
// 프론트엔드 - 5초마다 조회수만 업데이트
setInterval(() => {
    fetch(`/api/v1/posts/${postId}/view-count`)
        .then(res => res.json())
        .then(data => updateViewCountDisplay(data.viewCount));
}, 5000);
```

#### 장점
- ✅ 비동기 처리 유지
- ✅ 사용자가 최신 조회수 확인 가능
- ✅ 경량 API (전체 Post 조회보다 빠름)

#### 단점
- △ 추가 API 요청
- △ 여전히 약간의 지연
- △ 폴링 오버헤드

## 결정: Option 1 (이전 조회수 반환) ⭐

### 선택 이유

1. **사용자는 자신의 조회로 인한 즉시 증가를 기대하지 않음**
   - 조회수는 "인기도 지표"이지 "실시간 카운터"가 아님
   - YouTube, Medium 등 주요 서비스가 모두 이 방식 사용

2. **성능 최적화**
   - readOnly 트랜잭션 유지
   - 비동기 처리로 응답 시간 최소화

3. **단순성**
   - 복잡한 동기화 로직 불필요
   - 프론트엔드-백엔드 로직 중복 없음

4. **실용성**
   - 실제 서비스에서 검증된 방식
   - 사용자 불편 사항 보고된 바 없음

### 구현

```java
@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final PostViewService postViewService;

    @GetMapping("/{postId}")
    public ApiResponse<PostResponse> getPost(
            @PathVariable Long postId,
            HttpServletRequest request) {

        // 1. 게시글 조회 (readOnly 트랜잭션)
        PostResponse response = postService.getPostDetails(postId);

        // 2. 조회수 증가 (비동기, 정책 적용)
        Long memberId = getCurrentMemberId();
        ViewContext context = ViewContext.from(request, memberId);
        postViewService.tryIncrementViewCount(postId, context);

        // 3. 이전 조회수 값 반환 (문제 없음!)
        return ApiResponse.success(response, "post_retrieved");
    }

    private Long getCurrentMemberId() {
        // TODO: SecurityContext에서 추출
        return null;
    }
}
```

### PostResponse 예시
```json
{
    "status": "success",
    "message": "post_retrieved",
    "data": {
        "postId": 1,
        "title": "게시글 제목",
        "content": "내용",
        "viewsCount": 100,  // ← 이전 값 (실제 DB는 비동기로 101로 증가 중)
        "likeCount": 42
    }
}
```

**다음 조회 시 (또는 새로고침):**
```json
{
    "viewsCount": 101  // ← 이전 사용자의 조회가 반영됨
}
```

## 선택적 개선 사항

### 1. API 문서에 명시

Swagger/OpenAPI 문서에 명확히 기재:

```java
@Operation(
    summary = "게시글 조회",
    description = """
        게시글을 조회합니다.

        **참고**: 조회수는 비동기로 증가하며, 응답의 viewsCount는
        현재 조회가 반영되기 이전의 값입니다.
        다음 조회 시 증가된 값을 확인할 수 있습니다.
        """
)
@GetMapping("/{postId}")
public ApiResponse<PostResponse> getPost(@PathVariable Long postId) {
    // ...
}
```

### 2. 프론트엔드 가이드

프론트엔드 팀에게 안내:

```markdown
## 조회수 표시 가이드

- 조회수는 "대략적인 인기도" 지표로 표시
- 실시간 정확도가 필요 없음
- "조회 100회" 또는 "100+ 조회" 등으로 표시
- 사용자가 조회했다고 즉시 +1 되지 않음 (정상 동작)
```

### 3. 대규모 조회수는 근사값 표시

```java
public class ViewCountFormatter {

    public static String format(Long viewCount) {
        if (viewCount < 1000) {
            return viewCount.toString();
        } else if (viewCount < 10000) {
            return String.format("%.1fK", viewCount / 1000.0);
        } else if (viewCount < 1000000) {
            return String.format("%dK", viewCount / 1000);
        } else {
            return String.format("%.1fM", viewCount / 1000000.0);
        }
    }
}
```

```json
{
    "viewsCount": 125678,
    "viewsCountFormatted": "125K"  // 근사값으로 표시
}
```

**장점:**
- 사용자가 정확한 값을 기대하지 않게 됨
- "125K"에서 "125.1K"로 변해도 크게 의미 없음

## 대안 시나리오: 즉시 반영이 필요한 경우

만약 **비즈니스 요구사항으로 즉시 반영이 필수**라면:

### 방법 1: 동기 처리 + 캐싱

```java
@GetMapping("/{postId}")
public ApiResponse<PostResponse> getPost(@PathVariable Long postId,
                                          HttpServletRequest request) {
    ViewContext context = ViewContext.from(request, getCurrentMemberId());

    // 1. 정책 검증
    boolean willIncrement = viewCountPolicy.shouldCount(postId, context);

    // 2. 캐시된 조회수 조회
    Long currentViewCount = viewCountCache.get(postId);

    // 3. 즉시 캐시 업데이트 (낙관적)
    if (willIncrement) {
        currentViewCount++;
        viewCountCache.set(postId, currentViewCount);

        // 4. 비동기로 DB 업데이트
        postViewService.tryIncrementViewCount(postId, context);
    }

    // 5. 게시글 조회 후 조회수 오버라이드
    PostResponse response = postService.getPostDetails(postId);
    response = response.withViewCount(currentViewCount);

    return ApiResponse.success(response);
}
```

**단점:**
- 캐시와 DB 동기화 복잡
- 캐시 장애 시 문제

### 방법 2: 클라이언트 낙관적 업데이트 (가장 간단)

```javascript
// 프론트엔드
function displayPost(post) {
    // 서버 응답의 조회수
    const serverViewCount = post.viewCount;

    // 화면에는 +1 해서 표시 (낙관적)
    // 실제로는 증가 안될 수도 있지만 사용자 경험 개선
    displayViewCount(serverViewCount + 1);
}
```

**권장하지 않는 이유:** 정책으로 증가 안될 수 있음

## 결론

**최종 권장: Option 1 (이전 조회수 반환)**

**핵심 원칙:**
1. 조회수는 정확한 실시간 값이 아니어도 됨
2. 사용자는 자신의 조회로 인한 즉시 증가를 기대하지 않음
3. 성능과 단순성을 우선
4. 실제 서비스(YouTube, Medium 등)의 검증된 방식

**만약 비즈니스 팀에서 즉시 반영을 요구한다면:**
1. 먼저 실제 사용자 불편이 있는지 확인
2. YouTube 등 주요 서비스 사례 공유
3. 성능 트레이드오프 설명
4. 그래도 필요하다면 방법 2 (클라이언트 낙관적 업데이트)

## 참고 자료
- [YouTube View Count System](https://support.google.com/youtube/answer/2991785)
- [Eventual Consistency](https://en.wikipedia.org/wiki/Eventual_consistency)
- [Optimistic UI Updates](https://www.apollographql.com/docs/react/performance/optimistic-ui/)
