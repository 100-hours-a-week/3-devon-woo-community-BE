# ADR: 게시글 조회수 증가 전략

## 상태
검토 중 (Under Review)

## 컨텍스트
게시글 조회 시 조회수를 증가시켜야 하는 요구사항이 있다. 조회수 증가는 좋아요와 달리 다음과 같은 특성을 가진다:

1. **조회 API와 함께 발생**: 별도의 엔드포인트가 아닌 GET 요청 시 자동으로 증가
2. **높은 빈도**: 모든 게시글 조회마다 발생 (좋아요보다 훨씬 빈번함)
3. **정확도 요구사항 완화**: 실시간 정확도가 좋아요만큼 중요하지 않을 수 있음
4. **읽기 작업 내 쓰기 작업**: 조회(읽기) API 내에서 카운트 증가(쓰기)가 발생

### 현재 구현 문제점
```java
@Transactional(readOnly = true)
public PostResponse getPostDetails(Long postId) {
    Post post = findByIdWithMember(postId);
    // ...
    post.incrementViews();         // ❌ 쓰기 작업
    postRepository.save(post);     // ❌ readOnly 트랜잭션 내에서 save

    return PostResponse.of(post, member, file);
}
```

**문제점:**
- `@Transactional(readOnly = true)` 내에서 쓰기 작업 수행
- `readOnly` 플래그는 최적화 힌트이며, DB에 따라 쓰기가 무시될 수 있음
- Hibernate는 읽기 전용 트랜잭션에서 flush를 스킵하므로 변경사항이 DB에 반영되지 않을 수 있음
- 의미론적으로 모순됨 (읽기 작업 내에 쓰기 작업)

## 고려한 대안들

### 1. 동기 쓰기 - 별도 트랜잭션

#### 1-1. 조회 메서드에서 readOnly 제거
```java
@Transactional  // readOnly 제거
public PostResponse getPostDetails(Long postId) {
    Post post = findByIdWithMember(postId);

    post.incrementViews();
    // 트랜잭션 커밋 시 자동 flush

    return PostResponse.of(post, member, file);
}
```

**장점:**
- 구현이 간단하고 직관적
- 트랜잭션 내에서 모든 작업이 원자적으로 처리
- 즉시 조회수가 증가된 결과를 반환 가능

**단점:**
- ❌ `readOnly` 최적화를 사용할 수 없음
  - DB 커넥션 최적화 불가
  - Hibernate 플러시 모드 최적화 불가
  - 읽기 전용 복제본(read replica) 사용 불가
- ❌ 조회 성능 저하
  - 매 조회마다 UPDATE 쿼리 실행
  - 쓰기 락 대기 발생 가능
- ❌ 의미론적으로 부적절함 (읽기 API에 쓰기 작업)

#### 1-2. 서비스 분리 + 원자적 업데이트
```java
// PostService.java
@Transactional(readOnly = true)
public PostResponse getPostDetails(Long postId) {
    Post post = findByIdWithMember(postId);
    // 조회 로직만
    return PostResponse.of(post, member, file);
}

// PostViewService.java (별도 서비스)
@Transactional
public void incrementViewCount(Long postId) {
    postRepository.incrementViewCount(postId);  // 원자적 UPDATE
}

// PostController.java
@GetMapping("/{postId}")
public ApiResponse<PostResponse> getPost(@PathVariable Long postId) {
    PostResponse response = postService.getPostDetails(postId);
    postViewService.incrementViewCount(postId);  // 별도 트랜잭션
    return ApiResponse.success(response);
}
```

**장점:**
- ✅ 읽기/쓰기 완전 분리 (SRP 준수)
- ✅ `readOnly` 최적화 활용 가능
- ✅ 원자적 업데이트로 동시성 안전

**단점:**
- ❌ 두 개의 트랜잭션 실행 (성능 오버헤드)
- ❌ 조회와 카운트 증가가 원자적이지 않음
  - 조회는 성공했으나 카운트 증가 실패 가능
  - 일관성 문제 발생 가능
- ❌ 컨트롤러에 비즈니스 로직 노출

### 2. 비동기 처리 ⭐ 권장

#### 2-1. ApplicationEvent 기반 비동기 처리
```java
// PostService.java
@Transactional(readOnly = true)
public PostResponse getPostDetails(Long postId) {
    Post post = findByIdWithMember(postId);

    // 이벤트 발행 (비동기)
    eventPublisher.publishEvent(new PostViewedEvent(postId));

    return PostResponse.of(post, member, file);
}

// PostViewEventListener.java
@Component
public class PostViewEventListener {

    @Async
    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handlePostViewed(PostViewedEvent event) {
        postRepository.incrementViewCount(event.getPostId());
    }
}
```

**장점:**
- ✅ `readOnly` 트랜잭션 유지 (읽기 성능 최적화)
- ✅ 읽기/쓰기 완전 분리
- ✅ 조회 응답 시간에 영향 없음 (비동기)
- ✅ 원자적 업데이트로 동시성 안전
- ✅ 확장 가능한 구조 (이벤트 기반)
- ✅ 조회 트랜잭션 실패해도 조회수 증가 처리 가능

**단점:**
- △ 약간의 구현 복잡도 증가
- △ 즉시 증가된 조회수를 반환할 수 없음 (최종 일관성)
  - 하지만 일반적으로 조회 시 방금 증가한 조회수를 보여줄 필요는 없음
- △ 비동기 처리 실패 시 재시도 로직 필요 (선택적)

**추가 고려사항:**
```java
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("view-count-");
        executor.initialize();
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (throwable, method, params) -> {
            log.error("Async exception in method {}: {}",
                method.getName(), throwable.getMessage());
            // 실패한 조회수 증가를 큐에 넣거나 재시도 로직 추가 가능
        };
    }
}
```

#### 2-2. @Async 메서드 직접 호출
```java
// PostService.java
@Transactional(readOnly = true)
public PostResponse getPostDetails(Long postId) {
    Post post = findByIdWithMember(postId);

    // 비동기 메서드 호출
    postViewService.incrementViewCountAsync(postId);

    return PostResponse.of(post, member, file);
}

// PostViewService.java
@Service
public class PostViewService {

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void incrementViewCountAsync(Long postId) {
        postRepository.incrementViewCount(postId);
    }
}
```

**장점:**
- ✅ 이벤트보다 더 직관적
- ✅ 모든 비동기 처리 장점 동일

**단점:**
- △ self-invocation 주의 필요 (같은 클래스 내 호출 불가)
- △ 이벤트 방식보다 결합도가 높음

### 3. 캐시 + 배치 업데이트

#### 3-1. Redis 캐시 기반 처리
```java
// PostService.java
@Transactional(readOnly = true)
public PostResponse getPostDetails(Long postId) {
    Post post = findByIdWithMember(postId);

    // Redis에 조회수 증가
    redisTemplate.opsForValue().increment("post:view:" + postId);

    return PostResponse.of(post, member, file);
}

// PostViewSyncScheduler.java
@Component
public class PostViewSyncScheduler {

    @Scheduled(fixedRate = 60000)  // 1분마다
    @Transactional
    public void syncViewCounts() {
        Set<String> keys = redisTemplate.keys("post:view:*");

        for (String key : keys) {
            Long postId = extractPostId(key);
            Long viewCount = redisTemplate.opsForValue().get(key);

            // DB에 배치 업데이트
            postRepository.incrementViewCountBy(postId, viewCount);
            redisTemplate.delete(key);
        }
    }
}
```

**장점:**
- ✅ 매우 빠른 응답 (Redis 메모리 연산)
- ✅ DB 부하 최소화 (배치 업데이트)
- ✅ 높은 처리량
- ✅ 조회 트랜잭션과 완전 독립적

**단점:**
- ❌ 인프라 복잡도 증가 (Redis 필요)
- ❌ Redis 장애 시 조회수 손실 가능
  - 해결책: Redis 영속성(persistence) 설정
- ❌ 최종 일관성 (지연 시간 발생)
- ❌ 구현 및 운영 복잡도 높음
- ❌ 현재 프로젝트 규모에 과한 설계

#### 3-2. 로컬 캐시 + 배치 업데이트
```java
@Component
public class ViewCountBuffer {
    private final ConcurrentHashMap<Long, AtomicLong> buffer = new ConcurrentHashMap<>();

    public void increment(Long postId) {
        buffer.computeIfAbsent(postId, k -> new AtomicLong(0))
              .incrementAndGet();
    }

    public Map<Long, Long> drainAll() {
        Map<Long, Long> drained = new HashMap<>();
        buffer.forEach((postId, count) -> {
            drained.put(postId, count.getAndSet(0));
        });
        return drained;
    }
}

@Scheduled(fixedRate = 5000)  // 5초마다
@Transactional
public void flushViewCounts() {
    Map<Long, Long> counts = viewCountBuffer.drainAll();
    counts.forEach((postId, delta) -> {
        postRepository.incrementViewCountBy(postId, delta);
    });
}
```

**장점:**
- ✅ 외부 의존성 없음 (Redis 불필요)
- ✅ DB 부하 감소 (배치 업데이트)
- ✅ 빠른 응답

**단점:**
- ❌ 서버 재시작 시 메모리 내 데이터 손실
- ❌ 다중 서버 환경에서 동기화 문제
- ❌ 최종 일관성

## 결정
**2-1. ApplicationEvent 기반 비동기 처리 방식을 권장**

### 선택 이유

1. **현재 프로젝트 규모에 적합**
   - Redis 등 추가 인프라 불필요
   - 구현 복잡도와 효과의 균형이 좋음
   - 향후 확장 가능한 구조

2. **성능 최적화**
   - `readOnly` 트랜잭션 유지로 읽기 성능 보장
   - 조회 응답 시간에 영향 없음
   - 비동기 처리로 DB 부하 분산

3. **안정성**
   - 원자적 업데이트로 동시성 안전
   - 조회 실패와 조회수 증가 실패가 독립적
   - 예외 처리 및 재시도 로직 추가 가능

4. **설계 원칙 준수**
   - 읽기/쓰기 분리 (CQRS 패턴의 경량 버전)
   - 단일 책임 원칙(SRP)
   - 이벤트 기반 아키텍처로 확장 용이

5. **실용적 trade-off**
   - 조회수는 실시간 정확도가 크게 중요하지 않음
   - 약간의 지연(수백ms~수초)은 사용자 경험에 영향 없음
   - 좋아요와 달리 사용자가 조회수 변화를 즉시 확인하지 않음

## 구현 계획

### Phase 1: 기본 비동기 이벤트 처리 (권장)
```java
// 1. 이벤트 클래스
@Getter
@AllArgsConstructor
public class PostViewedEvent {
    private final Long postId;
    private final LocalDateTime viewedAt;

    public static PostViewedEvent of(Long postId) {
        return new PostViewedEvent(postId, LocalDateTime.now());
    }
}

// 2. Repository 메서드 추가
@Modifying
@Query("UPDATE Post p SET p.viewsCount = p.viewsCount + 1 WHERE p.id = :postId")
void incrementViewCount(@Param("postId") Long postId);

// 3. 이벤트 리스너
@Component
@RequiredArgsConstructor
@Slf4j
public class PostViewEventListener {
    private final PostRepository postRepository;

    @Async
    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handlePostViewed(PostViewedEvent event) {
        try {
            postRepository.incrementViewCount(event.getPostId());
            log.debug("View count incremented for post {}", event.getPostId());
        } catch (Exception e) {
            log.error("Failed to increment view count for post {}: {}",
                event.getPostId(), e.getMessage());
            // 필요시 재시도 큐에 추가
        }
    }
}

// 4. Service 수정
@Transactional(readOnly = true)
public PostResponse getPostDetails(Long postId) {
    Post post = findByIdWithMember(postId);
    Member member = post.getMember();
    Attachment file = attachmentRepository.findByPostId(postId).orElse(null);

    // 비동기 이벤트 발행
    eventPublisher.publishEvent(PostViewedEvent.of(postId));

    return PostResponse.of(post, member, file);
}

// 5. Async 설정
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("view-count-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) ->
            log.error("Uncaught async exception in {}: {}", method.getName(), ex.getMessage());
    }
}
```

### Phase 2: 향후 최적화 (트래픽 증가 시)
트래픽이 매우 높아지면 다음 최적화를 고려:

1. **중복 조회 방지**
   ```java
   // IP + postId 기반으로 일정 시간 내 중복 조회 필터링
   @Component
   public class ViewCountDeduplicator {
       private final Cache<String, Boolean> viewCache =
           Caffeine.newBuilder()
               .expireAfterWrite(Duration.ofMinutes(5))
               .maximumSize(10_000)
               .build();

       public boolean shouldCount(Long postId, String ipAddress) {
           String key = postId + ":" + ipAddress;
           return viewCache.get(key, k -> {
               // 캐시에 없으면 카운트 허용
               return true;
           });
       }
   }
   ```

2. **배치 처리로 전환**
   ```java
   // 로컬 버퍼에 모아서 주기적으로 배치 업데이트
   @Scheduled(fixedRate = 5000)
   public void flushViewCounts() {
       // 위의 3-2 방식 적용
   }
   ```

3. **Redis 캐시 도입**
   - 매우 높은 트래픽에서만 고려
   - 3-1 방식 적용

## 추가 고려사항

### 1. 중복 조회 방지
현재는 매 조회마다 카운트 증가. 다음 방식으로 개선 가능:
- 쿠키 기반: 같은 사용자가 N분 내 재조회 시 카운트 제외
- IP 기반: 같은 IP에서 N분 내 재조회 시 카운트 제외
- 로그인 사용자 기반: 같은 사용자 N분 내 재조회 시 카운트 제외

### 2. 봇/크롤러 필터링
검색 엔진 봇 등은 조회수에서 제외:
```java
public boolean isBot(HttpServletRequest request) {
    String userAgent = request.getHeader("User-Agent");
    return userAgent != null &&
           (userAgent.contains("bot") || userAgent.contains("crawler"));
}
```

### 3. 모니터링
- 비동기 처리 실패율 모니터링
- 조회수 증가 지연 시간 모니터링
- Thread pool 사용률 모니터링

## 비교 요약

| 방식 | 성능 | 복잡도 | 정확도 | readOnly | 권장도 |
|------|------|--------|--------|----------|---------|
| 1-1. 동기 (readOnly 제거) | ⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ❌ | ❌ |
| 1-2. 서비스 분리 | ⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐ | ✅ | △ |
| 2-1. Event 비동기 | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ✅ | ✅ |
| 2-2. @Async 직접 호출 | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ✅ | ✅ |
| 3-1. Redis 배치 | ⭐⭐⭐⭐⭐ | ⭐⭐ | ⭐⭐⭐ | ✅ | △ |
| 3-2. 로컬 캐시 배치 | ⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐ | ✅ | △ |

**권장 순서:**
1. Phase 1에서는 **2-1 (Event 비동기)** 또는 **2-2 (@Async)** 구현
2. 트래픽 증가 시 **중복 조회 방지** 추가
3. 매우 높은 트래픽 시 **3-2 (로컬 캐시 배치)** 도입
4. 분산 환경 + 초고트래픽 시 **3-1 (Redis 배치)** 고려

## 결과 예상
- ✅ 조회 성능 최적화 (readOnly 유지)
- ✅ 동시성 안전 (원자적 업데이트)
- ✅ 확장 가능한 아키텍처
- ✅ 최종 일관성으로 충분한 정확도
- ✅ 낮은 인프라 복잡도

## 참고 자료
- [Spring Events](https://spring.io/blog/2015/02/11/better-application-events-in-spring-framework-4-2)
- [Spring @Async](https://docs.spring.io/spring-framework/docs/current/reference/html/integration.html#scheduling-annotation-support-async)
- [CQRS Pattern](https://martinfowler.com/bliki/CQRS.html)
- [Eventual Consistency](https://en.wikipedia.org/wiki/Eventual_consistency)
