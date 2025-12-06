# ADR: 조회수 증가 로직의 위치 (Where to Place View Count Logic)

## 상태
검토 중 (Under Review)

## 컨텍스트
조회수 증가 로직을 **어디에 배치할 것인가**는 중요한 아키텍처 결정이다. 이는 다음과 같은 이슈들과 연관된다:

1. **관심사의 분리 (Separation of Concerns)**
2. **단일 책임 원칙 (Single Responsibility Principle)**
3. **테스트 용이성**
4. **재사용성**
5. **부수 효과(Side Effect) 관리**

## 문제 정의

조회수 증가는 **부수 효과(Side Effect)**이다:
- 주 목적: 게시글 데이터 조회
- 부수 효과: 조회수 카운트 증가

이러한 부수 효과를 어디서 처리할 것인가?

## 고려한 대안들

### Option 1: Controller에서 처리 (분리)

```java
@GetMapping("/{postId}")
public ApiResponse<PostResponse> getPost(
        @PathVariable Long postId,
        HttpServletRequest request) {

    // 1. 게시글 조회 (주 목적)
    PostResponse response = postService.getPostDetails(postId);

    // 2. 조회수 증가 (부수 효과)
    Long memberId = getCurrentMemberId();
    ViewContext context = ViewContext.from(request, memberId);
    postViewService.tryIncrementViewCount(postId, context);

    return ApiResponse.success(response);
}
```

#### 장점
- ✅ **관심사 완전 분리**
  - PostService는 순수하게 조회 로직만 담당
  - PostViewService는 조회수 증가만 담당
- ✅ **PostService 테스트 용이**
  - 조회 로직만 단위 테스트 가능
  - Mock 없이 순수한 테스트
- ✅ **명시적**
  - 조회수 증가가 Controller에서 명확히 보임
  - 코드 읽기 쉬움
- ✅ **유연성**
  - 특정 조회에서만 조회수 증가 제외 가능
  - 예: 미리보기, 수정 페이지 등

#### 단점
- ❌ **Controller에 비즈니스 로직 노출**
  - Controller가 두 개의 서비스를 조율
  - "조회하면 조회수가 증가한다"는 비즈니스 규칙이 Controller에 존재
- ❌ **재사용성 문제**
  - 다른 곳에서 게시글 조회 시 조회수 증가를 잊을 수 있음
  - 예: 관리자 페이지, 다른 API 등
- ❌ **일관성 보장 어려움**
  - 개발자가 실수로 조회수 증가를 호출하지 않을 수 있음
- ❌ **HTTP 계층 의존성**
  - ViewContext가 HttpServletRequest에 의존
  - Service 레이어를 HTTP 없이 재사용하기 어려움

### Option 2: Service에서 처리 (통합)

```java
// PostService.java
@Transactional(readOnly = true)
public PostResponse getPostDetails(Long postId, ViewContext context) {
    Post post = findByIdWithMember(postId);
    Member member = post.getMember();
    Attachment file = attachmentRepository.findByPostId(postId).orElse(null);

    // 조회수 증가 (비동기)
    postViewService.tryIncrementViewCount(postId, context);

    return PostResponse.of(post, member, file);
}
```

```java
// Controller.java
@GetMapping("/{postId}")
public ApiResponse<PostResponse> getPost(@PathVariable Long postId,
                                          HttpServletRequest request) {
    Long memberId = getCurrentMemberId();
    ViewContext context = ViewContext.from(request, memberId);

    PostResponse response = postService.getPostDetails(postId, context);
    return ApiResponse.success(response);
}
```

#### 장점
- ✅ **비즈니스 로직 캡슐화**
  - "조회 = 조회수 증가" 규칙이 Service에 명확히 존재
  - Controller는 단순히 Service 호출만
- ✅ **재사용성**
  - 어디서든 `getPostDetails` 호출하면 조회수 증가 보장
  - 일관성 있는 동작
- ✅ **실수 방지**
  - 개발자가 조회수 증가를 잊을 수 없음

#### 단점
- ❌ **HTTP 의존성 Service 침투**
  - Service가 ViewContext(HTTP 정보)를 받음
  - Service의 순수성 저하
  - 단위 테스트 시 ViewContext mock 필요
- ❌ **단일 책임 원칙 위반**
  - PostService가 조회 + 조회수 증가 두 가지 책임
- ❌ **유연성 감소**
  - 조회수 증가 없이 조회만 하고 싶을 때 어려움
  - 별도 메서드 필요: `getPostDetailsWithoutViewCount`

### Option 3: AOP (Aspect-Oriented Programming) 사용

```java
// CountViewAspect.java
@Aspect
@Component
@RequiredArgsConstructor
public class CountViewAspect {

    private final PostViewService postViewService;

    @Around("@annotation(CountView)")
    public Object countView(ProceedingJoinPoint joinPoint) throws Throwable {
        // 1. 원본 메서드 실행
        Object result = joinPoint.proceed();

        // 2. 조회수 증가 (비동기)
        Long postId = extractPostId(joinPoint);
        ViewContext context = extractViewContext(joinPoint);
        postViewService.tryIncrementViewCount(postId, context);

        return result;
    }

    private Long extractPostId(ProceedingJoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        // postId 추출 로직
        return (Long) args[0];
    }

    private ViewContext extractViewContext(ProceedingJoinPoint joinPoint) {
        // ViewContext 추출 로직
        // ...
    }
}

// 사용
@GetMapping("/{postId}")
@CountView  // AOP 적용
public ApiResponse<PostResponse> getPost(@PathVariable Long postId,
                                          HttpServletRequest request,
                                          ViewContext context) {
    PostResponse response = postService.getPostDetails(postId);
    return ApiResponse.success(response);
}
```

#### 장점
- ✅ **횡단 관심사(Cross-cutting Concern) 분리**
  - 조회수 증가는 횡단 관심사로 취급
  - Controller/Service 모두 깔끔
- ✅ **선언적 사용**
  - `@CountView` 어노테이션만으로 적용
  - 코드 가독성 좋음
- ✅ **재사용성**
  - 어떤 메서드에도 `@CountView` 붙이면 적용

#### 단점
- ❌ **복잡도 증가**
  - AOP 이해도 필요
  - 디버깅 어려움 (어디서 실행되는지 불명확)
- ❌ **파라미터 추출 로직 복잡**
  - Reflection으로 postId, ViewContext 찾아야 함
  - 실수하기 쉬움
- ❌ **명시성 감소**
  - 코드만 봐서는 조회수 증가 로직 존재 파악 어려움
- ❌ **과도한 설계**
  - 조회수 증가는 특정 API에만 적용
  - 진정한 횡단 관심사(로깅, 트랜잭션)와는 성격 다름

### Option 4: Interceptor 사용

```java
@Component
public class ViewCountInterceptor implements HandlerInterceptor {

    private final PostViewService postViewService;

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) {
        // URL 패턴 확인
        if (isPostDetailRequest(request)) {
            Long postId = extractPostIdFromUrl(request);
            ViewContext context = ViewContext.from(request, getCurrentMemberId());
            postViewService.tryIncrementViewCount(postId, context);
        }
    }

    private boolean isPostDetailRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.matches("/api/v1/posts/\\d+") && "GET".equals(request.getMethod());
    }
}
```

#### 장점
- ✅ **완전한 분리**
  - Controller/Service 모두 조회수 증가 로직 없음
  - 순수하게 각자 역할만 수행
- ✅ **HTTP 레벨에서 처리**
  - 조회수는 HTTP 요청에 대한 부수 효과
  - 적절한 추상화 레벨

#### 단점
- ❌ **URL 패턴 기반 판단**
  - 하드코딩된 URL 패턴
  - URL 변경 시 Interceptor도 수정 필요
- ❌ **암시적**
  - Controller 코드만 봐서는 조회수 증가 파악 불가
  - 설정 파일 확인 필요
- ❌ **테스트 어려움**
  - Integration 테스트에서만 확인 가능
- ❌ **유연성 부족**
  - 특정 조회 API에서만 제외하기 어려움

### Option 5: Domain Event (가장 깔끔한 방식) ⭐ 권장

```java
// PostService.java
@Transactional(readOnly = true)
public PostResponse getPostDetails(Long postId) {
    Post post = findByIdWithMember(postId);
    Member member = post.getMember();
    Attachment file = attachmentRepository.findByPostId(postId).orElse(null);

    // 도메인 이벤트 발행 (조회 사실만 알림)
    eventPublisher.publishEvent(PostReadEvent.of(postId));

    return PostResponse.of(post, member, file);
}

// PostReadEvent.java
@Getter
@AllArgsConstructor
public class PostReadEvent {
    private final Long postId;
    // ViewContext는 여기 없음!

    public static PostReadEvent of(Long postId) {
        return new PostReadEvent(postId);
    }
}

// PostReadEventListener.java
@Component
@RequiredArgsConstructor
public class PostReadEventListener {

    private final PostViewService postViewService;
    private final HttpServletRequest request;  // Request-scoped

    @EventListener
    public void handlePostRead(PostReadEvent event) {
        // HTTP 컨텍스트는 여기서 추출
        ViewContext context = ViewContext.from(request, getCurrentMemberId());

        // 정책 검증 후 조회수 증가 (비동기)
        postViewService.tryIncrementViewCount(event.getPostId(), context);
    }

    private Long getCurrentMemberId() {
        // SecurityContext에서 추출 또는 null
        return null;
    }
}
```

#### 장점
- ✅ **완벽한 관심사 분리**
  - PostService: 순수하게 조회만 담당, HTTP 무관
  - PostViewService: 조회수 증가만 담당
  - Controller: 단순 호출만
- ✅ **Service의 순수성 유지**
  - ViewContext 없이 순수 도메인 로직만
  - 단위 테스트 매우 쉬움
- ✅ **확장 가능**
  - PostReadEvent를 다른 리스너도 구독 가능
  - 예: 최근 조회 기록, 추천 알고리즘 등
- ✅ **명시적**
  - Service에서 `publishEvent` 명확히 보임
  - 하지만 HTTP 의존성은 없음
- ✅ **유연성**
  - 리스너에서 정책 적용
  - 특정 상황에서 리스너 비활성화 가능

#### 단점
- △ **이벤트 기반 복잡도**
  - 이벤트 발행/구독 개념 이해 필요
  - 코드 흐름 추적이 직접 호출보다 어려움
- △ **Request-scoped Bean 필요**
  - HttpServletRequest를 리스너에 주입
  - Spring 설정 필요
- △ **동기/비동기 선택 필요**
  - `@EventListener` (동기) vs `@Async @EventListener` (비동기)
  - 설계 결정 필요

#### Request-scoped Bean 설정
```java
@Configuration
public class RequestScopeConfig {

    @Bean
    @Scope(value = WebApplicationContext.SCOPE_REQUEST, proxyMode = ScopedProxyMode.TARGET_CLASS)
    public HttpServletRequest httpServletRequest() {
        return ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                .getRequest();
    }
}
```

### Option 6: Custom Annotation + AOP + Event (하이브리드)

```java
// @TrackView 어노테이션
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface TrackView {
    String postIdParam() default "postId";  // postId 파라미터 이름
}

// TrackViewAspect.java
@Aspect
@Component
@RequiredArgsConstructor
public class TrackViewAspect {

    private final ApplicationEventPublisher eventPublisher;

    @AfterReturning("@annotation(trackView)")
    public void trackView(JoinPoint joinPoint, TrackView trackView) {
        Long postId = extractPostId(joinPoint, trackView.postIdParam());
        if (postId != null) {
            eventPublisher.publishEvent(PostReadEvent.of(postId));
        }
    }

    private Long extractPostId(JoinPoint joinPoint, String paramName) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] parameterNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();

        for (int i = 0; i < parameterNames.length; i++) {
            if (parameterNames[i].equals(paramName)) {
                return (Long) args[i];
            }
        }
        return null;
    }
}

// 사용
@GetMapping("/{postId}")
public ApiResponse<PostResponse> getPost(@PathVariable Long postId) {
    PostResponse response = postService.getPostDetails(postId);
    return ApiResponse.success(response);
}

@Service
public class PostService {

    @TrackView(postIdParam = "postId")  // 여기에 적용!
    @Transactional(readOnly = true)
    public PostResponse getPostDetails(Long postId) {
        // 순수한 조회 로직만
        // ...
    }
}
```

#### 장점
- ✅ Service 순수성 유지 (HTTP 무관)
- ✅ 선언적 사용 (`@TrackView`)
- ✅ Event 기반으로 확장 가능
- ✅ Controller는 아무것도 몰라도 됨

#### 단점
- ❌ 복잡도 매우 높음
- ❌ AOP + Event 두 가지 메커니즘
- ❌ 과도한 설계

## 실제 서비스 사례 분석

### 1. Medium
**추정 방식**: Option 5 (Domain Event)
- 조회 시 이벤트 발행
- 별도 서비스가 이벤트 구독하여 조회수 처리
- 마이크로서비스 아키텍처에서 이벤트 기반 설계

### 2. Stack Overflow
**추정 방식**: Option 1 (Controller 분리) 또는 Option 4 (Interceptor)
- HTTP 레벨에서 조회수 처리
- 쿠키/IP 기반 중복 제거

### 3. YouTube
**추정 방식**: Option 5 (Domain Event) + 복잡한 파이프라인
- 조회 이벤트 발행
- 별도 스트리밍 파이프라인에서 처리
- Kafka 등 메시지 큐 활용

### 4. WordPress
**추정 방식**: Option 2 (Service 통합)
- 단순한 아키텍처
- 조회 함수 내에서 조회수 증가

## 결정: Option 5 (Domain Event) 권장 ⭐

### 선택 이유

1. **관심사의 완벽한 분리**
   - PostService는 HTTP를 모름 (순수 도메인 로직)
   - 조회수 증가는 HTTP 이벤트에 대한 반응

2. **단위 테스트 용이**
   ```java
   @Test
   void getPostDetails_shouldReturnPost() {
       // ViewContext, HttpServletRequest 없이 테스트 가능!
       PostResponse response = postService.getPostDetails(1L);
       assertThat(response).isNotNull();
   }
   ```

3. **확장 가능한 아키텍처**
   - 다른 기능도 PostReadEvent 구독 가능
   - 예: 최근 조회 기록, 추천 알고리즘, 사용자 행동 분석

4. **실용성**
   - Option 3, 6처럼 복잡하지 않음
   - Spring Event는 Spring 기본 기능 (추가 라이브러리 불필요)
   - 팀원들이 이해하기 쉬움

5. **비동기 처리 자연스럽게 통합**
   ```java
   @Async  // 비동기 처리
   @EventListener
   public void handlePostRead(PostReadEvent event) { ... }
   ```

### 구현 가이드

#### 1. 이벤트 클래스
```java
@Getter
@AllArgsConstructor
public class PostReadEvent {
    private final Long postId;
    private final LocalDateTime readAt;

    public static PostReadEvent of(Long postId) {
        return new PostReadEvent(postId, LocalDateTime.now());
    }
}
```

#### 2. Service (순수)
```java
@Service
@RequiredArgsConstructor
public class PostService {

    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public PostResponse getPostDetails(Long postId) {
        Post post = findByIdWithMember(postId);
        // ... 조회 로직

        // 조회 사실만 이벤트로 알림 (HTTP 정보 없음!)
        eventPublisher.publishEvent(PostReadEvent.of(postId));

        return PostResponse.of(post, member, file);
    }
}
```

#### 3. Event Listener (HTTP 처리)
```java
@Component
@RequiredArgsConstructor
@Slf4j
public class PostReadEventListener {

    private final PostViewService postViewService;
    @Lazy private final HttpServletRequest request;  // Request-scoped

    @Async  // 비동기 처리
    @EventListener
    public void handlePostRead(PostReadEvent event) {
        try {
            // HTTP 컨텍스트는 여기서 추출
            Long memberId = getCurrentMemberId();
            ViewContext context = ViewContext.from(request, memberId);

            // 정책 검증 후 조회수 증가
            postViewService.tryIncrementViewCount(event.getPostId(), context);
        } catch (Exception e) {
            log.error("Failed to handle post read event: {}", e.getMessage());
            // 조회 실패해도 조회수 증가 실패는 무시 (부수 효과이므로)
        }
    }

    private Long getCurrentMemberId() {
        // TODO: SecurityContext에서 추출
        return null;
    }
}
```

#### 4. Controller (깔끔)
```java
@GetMapping("/{postId}")
public ApiResponse<PostResponse> getPost(@PathVariable Long postId) {
    // 단순히 조회만
    PostResponse response = postService.getPostDetails(postId);
    return ApiResponse.success(response);
}
```

#### 5. Request-scoped 설정
```java
@Configuration
public class WebConfig {

    @Bean
    @Scope(value = WebApplicationContext.SCOPE_REQUEST,
           proxyMode = ScopedProxyMode.TARGET_CLASS)
    public HttpServletRequest httpServletRequest() {
        return ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                .getRequest();
    }
}
```

## 대안: 프로젝트 규모가 작다면 Option 1 (Controller 분리)

만약 **빠른 구현**이 우선이거나 **팀원들이 이벤트 패턴에 익숙하지 않다면** Option 1도 충분히 실용적이다.

```java
@GetMapping("/{postId}")
public ApiResponse<PostResponse> getPost(@PathVariable Long postId,
                                          HttpServletRequest request) {
    // 1. 조회
    PostResponse response = postService.getPostDetails(postId);

    // 2. 조회수 증가 (명시적)
    Long memberId = getCurrentMemberId();
    ViewContext context = ViewContext.from(request, memberId);
    postViewService.tryIncrementViewCount(postId, context);

    return ApiResponse.success(response);
}
```

**Option 1의 실용성:**
- 코드가 명시적이고 이해하기 쉬움
- Controller가 "조율자" 역할 (허용 가능)
- 나중에 Option 5로 리팩토링 가능

## 비교 요약

| 방식 | Service 순수성 | 명시성 | 복잡도 | 테스트 | 확장성 | 권장도 |
|------|---------------|--------|--------|--------|--------|--------|
| Option 1: Controller | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐ | ✅ (간단한 프로젝트) |
| Option 2: Service | ⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐ | ⭐⭐⭐ | △ |
| Option 3: AOP | ⭐⭐⭐⭐ | ⭐⭐ | ⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐ | ❌ |
| Option 4: Interceptor | ⭐⭐⭐⭐⭐ | ⭐ | ⭐⭐⭐ | ⭐⭐ | ⭐⭐ | ❌ |
| **Option 5: Event** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | **⭐ (권장)** |
| Option 6: Hybrid | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ❌ (과도) |

## 권장 로드맵

### Phase 1: MVP (빠른 출시)
**Option 1 (Controller 분리)** 사용
- 구현 간단
- 이해하기 쉬움
- 빠른 개발

### Phase 2: 리팩토링 (안정화 후)
**Option 5 (Domain Event)**로 전환
- 테스트 커버리지 확보 후
- 팀원들과 이벤트 패턴 공유
- 점진적 마이그레이션

### Phase 3: 확장 (필요 시)
- 다른 기능도 PostReadEvent 구독
- 예: 추천 알고리즘, 사용자 분석

## 결론

**최종 권장:**
- **중대형 프로젝트, 확장 가능성 중요**: **Option 5 (Domain Event)**
- **소형 프로젝트, 빠른 개발 필요**: **Option 1 (Controller 분리)**

**피해야 할:**
- Option 2 (Service에 HTTP 의존성 침투)
- Option 3, 4, 6 (과도한 복잡도)

## 참고 자료
- [Spring Events Guide](https://spring.io/blog/2015/02/11/better-application-events-in-spring-framework-4-2)
- [Domain Events Pattern](https://martinfowler.com/eaaDev/DomainEvent.html)
- [Request-scoped Beans](https://docs.spring.io/spring-framework/reference/core/beans/factory-scopes.html#beans-factory-scopes-request)
