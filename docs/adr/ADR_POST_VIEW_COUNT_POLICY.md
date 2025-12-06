# ADR: 게시글 조회수 증가 정책 (View Count Policy)

## 상태
검토 중 (Under Review)

## 컨텍스트
조회수는 단순히 "조회할 때마다 +1"이 아니라, 다양한 정책을 통해 **의미 있는 조회**만 카운트해야 한다. 무분별한 조회수 증가는 다음과 같은 문제를 야기한다:

1. **조회수 조작**: 새로고침으로 조회수 부풀리기
2. **봇/크롤러**: 검색엔진 봇의 반복 접근
3. **왜곡된 인기도**: 작성자 본인의 반복 조회
4. **성능 문제**: 불필요한 DB 쓰기 작업

따라서 **어떤 조회를 카운트할지**에 대한 명확한 정책이 필요하다.

## 실제 서비스 사례 분석

### 1. YouTube
**정책:**
- 동일 사용자가 짧은 시간 내 재조회 시 카운트 제외
- 30초 이상 시청해야 조회수 인정 (단순 클릭은 제외)
- 봇/자동화된 조회 필터링
- 일정 시간 후 조회수 검증 (급격한 증가 감지 시 일시 동결)

**구현 추정:**
- 세션/쿠키 기반 중복 제거
- 시청 시간 추적
- ML 기반 이상 패턴 감지

### 2. Medium
**정책:**
- 비로그인 사용자: IP + User-Agent 기반 중복 제거 (일정 시간)
- 로그인 사용자: 사용자 ID 기반 중복 제거 (일정 시간)
- 작성자 본인 조회는 카운트하지만 별도 표시

**구현 추정:**
- 쿠키/로컬 스토리지 활용
- Redis 기반 중복 체크 (TTL 설정)

### 3. Stack Overflow
**정책:**
- 15분 간격으로 동일 IP/사용자는 1회만 카운트
- 작성자 조회도 포함
- 봇 User-Agent 필터링

**구현 추정:**
- IP + User-Agent 해시 기반 캐싱
- 15분 TTL

### 4. 네이버 블로그
**정책:**
- 하루(24시간) 내 동일 사용자 재조회 제외
- 작성자 본인 조회 제외
- 로그인/비로그인 사용자 구분

**구현 추정:**
- 쿠키 기반 (비로그인)
- 사용자 ID 기반 (로그인)
- 자정 초기화

### 5. Reddit
**정책:**
- 매우 빠른 조회는 제외 (3초 이내 이탈)
- Unique visitor 기반 카운트
- 작성자 조회도 포함

## 조회수 정책 설계 옵션

### Option 1: 작성자 제외 정책

#### 장점
- 더 객관적인 인기도 지표
- 조회수 부풀리기 방지

#### 단점
- 작성자가 자신의 글을 수정하기 위해 조회할 때도 제외됨
- 구현 복잡도 증가

#### 구현
```java
public interface ViewCountPolicy {
    boolean shouldCount(Long postId, ViewContext context);
}

@Component
public class ExcludeAuthorPolicy implements ViewCountPolicy {

    private final PostRepository postRepository;

    @Override
    public boolean shouldCount(Long postId, ViewContext context) {
        // 작성자 본인이면 제외
        Post post = postRepository.findById(postId).orElseThrow();
        return !post.getMember().getId().equals(context.getMemberId());
    }
}
```

### Option 2: 시간 기반 중복 제거 (권장)

#### 2-1. 일정 시간 내 중복 제거 (15분 ~ 1시간)
**실무에서 가장 많이 사용하는 방식**

**장점:**
- 실질적인 조회수 측정
- 새로고침 조작 방지
- 합리적인 중복 제거

**단점:**
- 캐시 또는 DB 저장소 필요
- TTL 관리 필요

**구현 (Redis):**
```java
@Component
@RequiredArgsConstructor
public class TimeBasedDeduplicationPolicy implements ViewCountPolicy {

    private final RedisTemplate<String, String> redisTemplate;
    private static final Duration DEDUPLICATION_WINDOW = Duration.ofMinutes(30);

    @Override
    public boolean shouldCount(Long postId, ViewContext context) {
        String key = generateKey(postId, context);

        // Redis에 키가 없으면 조회수 카운트하고 키 설정
        Boolean isNewView = redisTemplate.opsForValue()
            .setIfAbsent(key, "1", DEDUPLICATION_WINDOW);

        return Boolean.TRUE.equals(isNewView);
    }

    private String generateKey(Long postId, ViewContext context) {
        // 로그인 사용자면 memberId 사용
        if (context.getMemberId() != null) {
            return String.format("view:%d:user:%d", postId, context.getMemberId());
        }
        // 비로그인이면 IP + User-Agent 해시 사용
        String identifier = context.getIpAddress() + context.getUserAgent();
        String hash = DigestUtils.md5DigestAsHex(identifier.getBytes());
        return String.format("view:%d:guest:%s", postId, hash);
    }
}
```

**구현 (로컬 캐시 - Redis 없을 때):**
```java
@Component
public class LocalCacheDeduplicationPolicy implements ViewCountPolicy {

    // Caffeine 캐시 사용
    private final Cache<String, Boolean> viewCache = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofMinutes(30))
        .maximumSize(100_000)  // 메모리 제한
        .build();

    @Override
    public boolean shouldCount(Long postId, ViewContext context) {
        String key = generateKey(postId, context);

        // 캐시에 없으면 true 반환하고 캐시에 저장
        Boolean alreadyViewed = viewCache.getIfPresent(key);
        if (alreadyViewed == null) {
            viewCache.put(key, true);
            return true;
        }
        return false;
    }

    private String generateKey(Long postId, ViewContext context) {
        if (context.getMemberId() != null) {
            return postId + ":" + context.getMemberId();
        }
        String identifier = context.getIpAddress() + context.getUserAgent();
        return postId + ":" + DigestUtils.md5DigestAsHex(identifier.getBytes());
    }
}
```

#### 2-2. 하루(24시간) 단위 중복 제거
**네이버 블로그, 일부 커뮤니티 사이트 방식**

**장점:**
- 일일 방문자 수(DAU) 개념과 유사
- 더 보수적인 조회수 집계

**단점:**
- 하루에 여러 번 방문하는 사용자도 1회만 카운트
- 시간대별 트래픽 분석 어려움

**구현:**
```java
@Component
@RequiredArgsConstructor
public class DailyDeduplicationPolicy implements ViewCountPolicy {

    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public boolean shouldCount(Long postId, ViewContext context) {
        String key = generateDailyKey(postId, context);

        // 자정까지 유효한 키 설정
        long secondsUntilMidnight = getSecondsUntilMidnight();
        Boolean isNewView = redisTemplate.opsForValue()
            .setIfAbsent(key, "1", Duration.ofSeconds(secondsUntilMidnight));

        return Boolean.TRUE.equals(isNewView);
    }

    private String generateDailyKey(Long postId, ViewContext context) {
        String today = LocalDate.now().toString();
        String identifier = context.getMemberId() != null
            ? "user:" + context.getMemberId()
            : "guest:" + hashGuestIdentifier(context);

        return String.format("daily_view:%s:%d:%s", today, postId, identifier);
    }

    private long getSecondsUntilMidnight() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime midnight = now.toLocalDate().atStartOfDay().plusDays(1);
        return ChronoUnit.SECONDS.between(now, midnight);
    }

    private String hashGuestIdentifier(ViewContext context) {
        String identifier = context.getIpAddress() + context.getUserAgent();
        return DigestUtils.md5DigestAsHex(identifier.getBytes());
    }
}
```

### Option 3: 복합 정책 (권장)

여러 정책을 조합하여 더 정교한 필터링

```java
@Component
@RequiredArgsConstructor
public class CompositeViewCountPolicy implements ViewCountPolicy {

    private final List<ViewCountPolicy> policies;

    public CompositeViewCountPolicy(
            BotFilterPolicy botFilterPolicy,
            TimeBasedDeduplicationPolicy timeBasedPolicy,
            ExcludeAuthorPolicy excludeAuthorPolicy  // 선택적
    ) {
        this.policies = List.of(
            botFilterPolicy,
            timeBasedPolicy
            // excludeAuthorPolicy  // 필요 시 추가
        );
    }

    @Override
    public boolean shouldCount(Long postId, ViewContext context) {
        // 모든 정책을 통과해야 카운트
        return policies.stream()
            .allMatch(policy -> policy.shouldCount(postId, context));
    }
}
```

### Option 4: 봇/크롤러 필터링 (필수)

```java
@Component
public class BotFilterPolicy implements ViewCountPolicy {

    private static final List<String> BOT_PATTERNS = List.of(
        "bot", "crawler", "spider", "scraper",
        "Googlebot", "Bingbot", "Yahoo", "Baiduspider",
        "facebookexternalhit", "Twitterbot", "LinkedInBot"
    );

    @Override
    public boolean shouldCount(Long postId, ViewContext context) {
        String userAgent = context.getUserAgent();
        if (userAgent == null) {
            return false;  // User-Agent 없으면 봇으로 간주
        }

        // 봇 패턴 매칭
        return BOT_PATTERNS.stream()
            .noneMatch(pattern ->
                userAgent.toLowerCase().contains(pattern.toLowerCase()));
    }
}
```

### Option 5: 머신러닝 기반 이상 탐지 (고급)

**YouTube, Medium 등 대형 서비스 사용**

```java
@Component
public class AnomalyDetectionPolicy implements ViewCountPolicy {

    @Override
    public boolean shouldCount(Long postId, ViewContext context) {
        // 1. 짧은 시간 내 동일 IP에서 과도한 조회 감지
        if (isRapidFirePattern(context.getIpAddress())) {
            return false;
        }

        // 2. 의심스러운 User-Agent 패턴
        if (isSuspiciousUserAgent(context.getUserAgent())) {
            return false;
        }

        // 3. 비정상적인 조회 패턴 (예: 순차적인 게시글 ID 조회)
        if (isSequentialAccessPattern(context)) {
            return false;
        }

        return true;
    }

    private boolean isRapidFirePattern(String ipAddress) {
        // Redis에서 최근 1분간 해당 IP의 조회 수 확인
        // 예: 1분에 100회 이상이면 봇으로 간주
        // 실제로는 Sliding Window Counter 알고리즘 사용
        return false;
    }
}
```

## 아키텍처 설계: Policy Pattern + Strategy Pattern

### 1. 핵심 인터페이스 및 DTO

```java
// ViewContext.java - 조회 컨텍스트 정보
@Getter
@Builder
public class ViewContext {
    private Long memberId;           // 로그인 사용자 ID (nullable)
    private String ipAddress;        // IP 주소
    private String userAgent;        // User-Agent
    private LocalDateTime viewedAt;  // 조회 시각
    private String sessionId;        // 세션 ID (선택적)

    public static ViewContext from(HttpServletRequest request, Long memberId) {
        return ViewContext.builder()
            .memberId(memberId)
            .ipAddress(extractIpAddress(request))
            .userAgent(request.getHeader("User-Agent"))
            .viewedAt(LocalDateTime.now())
            .sessionId(request.getSession().getId())
            .build();
    }

    private static String extractIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}

// ViewCountPolicy.java - 정책 인터페이스
public interface ViewCountPolicy {
    /**
     * 조회수를 카운트해야 하는지 판단
     * @return true면 카운트, false면 스킵
     */
    boolean shouldCount(Long postId, ViewContext context);
}
```

### 2. 서비스 레이어 통합

```java
// PostViewService.java
@Service
@RequiredArgsConstructor
@Slf4j
public class PostViewService {

    private final PostRepository postRepository;
    private final ViewCountPolicy viewCountPolicy;  // 주입된 정책
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 조회수 증가 시도 (정책 검증 후)
     */
    public void tryIncrementViewCount(Long postId, ViewContext context) {
        // 정책 검증
        if (viewCountPolicy.shouldCount(postId, context)) {
            // 비동기 이벤트 발행 (실제 증가는 비동기로)
            eventPublisher.publishEvent(PostViewedEvent.of(postId, context));
            log.debug("View count increment event published for post {}", postId);
        } else {
            log.debug("View count skipped for post {} by policy", postId);
        }
    }
}

// PostViewEventListener.java
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
        }
    }
}
```

### 3. Controller 통합

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
            HttpServletRequest request
    ) {
        // 1. 게시글 조회 (readOnly)
        PostResponse response = postService.getPostDetails(postId);

        // 2. 조회수 증가 시도 (비동기, 정책 적용)
        Long memberId = getCurrentMemberId();  // JWT에서 추출 또는 null
        ViewContext context = ViewContext.from(request, memberId);
        postViewService.tryIncrementViewCount(postId, context);

        return ApiResponse.success(response, "post_retrieved");
    }

    private Long getCurrentMemberId() {
        // TODO: JWT 구현 후 SecurityContext에서 추출
        // 현재는 null 반환 (비로그인 사용자로 처리)
        return null;
    }
}
```

## 정책 조합 설정

### Configuration
```java
@Configuration
public class ViewCountPolicyConfig {

    /**
     * 기본 정책: 봇 필터링 + 30분 중복 제거
     */
    @Bean
    @ConditionalOnProperty(
        name = "view-count.policy",
        havingValue = "basic",
        matchIfMissing = true
    )
    public ViewCountPolicy basicPolicy(
            BotFilterPolicy botFilter,
            LocalCacheDeduplicationPolicy timeBasedPolicy
    ) {
        return new CompositeViewCountPolicy(
            botFilter,
            timeBasedPolicy
        );
    }

    /**
     * 고급 정책: 봇 필터링 + 30분 중복 제거 + 작성자 제외
     */
    @Bean
    @ConditionalOnProperty(name = "view-count.policy", havingValue = "advanced")
    public ViewCountPolicy advancedPolicy(
            BotFilterPolicy botFilter,
            LocalCacheDeduplicationPolicy timeBasedPolicy,
            ExcludeAuthorPolicy excludeAuthor
    ) {
        return new CompositeViewCountPolicy(
            botFilter,
            timeBasedPolicy,
            excludeAuthor
        );
    }

    /**
     * Redis 기반 정책 (프로덕션 환경)
     */
    @Bean
    @ConditionalOnProperty(name = "view-count.policy", havingValue = "redis")
    public ViewCountPolicy redisPolicy(
            BotFilterPolicy botFilter,
            TimeBasedDeduplicationPolicy redisTimeBasedPolicy
    ) {
        return new CompositeViewCountPolicy(
            botFilter,
            redisTimeBasedPolicy
        );
    }
}
```

### application.yml
```yaml
view-count:
  policy: basic  # basic, advanced, redis
  deduplication-window: 30m  # 중복 제거 시간 윈도우
  exclude-author: false      # 작성자 조회 제외 여부
  bot-filter: true           # 봇 필터링 활성화
```

## 권장 구현 로드맵

### Phase 1: 기본 구현 (MVP)
**목표**: 기본적인 중복 제거 및 봇 필터링

1. `ViewContext` 및 `ViewCountPolicy` 인터페이스 구현
2. `BotFilterPolicy` 구현
3. `LocalCacheDeduplicationPolicy` 구현 (30분 TTL)
4. `PostViewService` 통합
5. Controller에서 `ViewContext` 생성 및 호출

**장점:**
- Redis 불필요 (인프라 간단)
- 기본적인 조회수 조작 방지
- 빠른 구현

**단점:**
- 다중 서버 환경에서 중복 제거 불완전
- 서버 재시작 시 캐시 초기화

### Phase 2: Redis 기반 고도화 (스케일 업)
**조건**: 트래픽 증가 또는 다중 서버 환경

1. Redis 인프라 구축
2. `TimeBasedDeduplicationPolicy` (Redis) 구현
3. 로컬 캐시를 Redis로 교체

**장점:**
- 다중 서버에서 정확한 중복 제거
- 영구 저장 가능 (Redis persistence)

### Phase 3: 고급 정책 (선택적)
**조건**: 비즈니스 요구사항에 따라

1. `ExcludeAuthorPolicy` 추가
2. 일일 중복 제거 옵션 추가
3. `AnomalyDetectionPolicy` (이상 탐지)

### Phase 4: 분석 및 최적화
1. 조회수 증가 로그 수집
2. 정책 효과 분석 (필터링된 조회 비율)
3. 정책 파라미터 튜닝 (중복 제거 시간 등)

## 테스트 전략

### 단위 테스트
```java
@Test
void botFilterPolicy_shouldExcludeBots() {
    // given
    BotFilterPolicy policy = new BotFilterPolicy();
    ViewContext botContext = ViewContext.builder()
        .userAgent("Mozilla/5.0 (compatible; Googlebot/2.1)")
        .build();

    // when
    boolean result = policy.shouldCount(1L, botContext);

    // then
    assertThat(result).isFalse();
}

@Test
void timeBasedDeduplicationPolicy_shouldAllowFirstView() {
    // given
    LocalCacheDeduplicationPolicy policy = new LocalCacheDeduplicationPolicy();
    ViewContext context = createTestContext();

    // when
    boolean firstView = policy.shouldCount(1L, context);
    boolean secondView = policy.shouldCount(1L, context);

    // then
    assertThat(firstView).isTrue();   // 첫 조회는 허용
    assertThat(secondView).isFalse(); // 30분 내 재조회는 차단
}
```

### 통합 테스트
```java
@Test
void postView_withValidPolicy_shouldIncrementCount() throws InterruptedException {
    // given
    Long postId = 1L;
    ViewContext context = ViewContext.from(mockRequest, null);

    // when
    postViewService.tryIncrementViewCount(postId, context);
    Thread.sleep(100); // 비동기 처리 대기

    // then
    Post post = postRepository.findById(postId).orElseThrow();
    assertThat(post.getViewsCount()).isEqualTo(1L);
}

@Test
void postView_duplicateWithinWindow_shouldNotIncrementCount() {
    // given
    Long postId = 1L;
    ViewContext context = ViewContext.from(mockRequest, null);

    // when
    postViewService.tryIncrementViewCount(postId, context);
    postViewService.tryIncrementViewCount(postId, context);  // 중복

    Thread.sleep(100);

    // then
    Post post = postRepository.findById(postId).orElseThrow();
    assertThat(post.getViewsCount()).isEqualTo(1L);  // 1회만 증가
}
```

## 실무 권장 사항

### 1. 작성자 제외 정책은 선택적으로
- **제외하는 경우**: 커뮤니티, SNS (순수 타인의 관심도 측정)
- **포함하는 경우**: 블로그, 뉴스 (총 조회수 중요)

### 2. 중복 제거 시간은 서비스 특성에 따라
- **15분 ~ 30분**: 일반적인 커뮤니티, 블로그
- **1시간 ~ 3시간**: 뉴스, 미디어 사이트
- **24시간**: DAU 개념이 중요한 서비스

### 3. 봇 필터링은 필수
- User-Agent 기반 기본 필터링은 반드시 구현
- 고급 기능은 트래픽에 따라 추가

### 4. 로그 및 모니터링
```java
@Slf4j
public class ViewCountPolicyLogger implements ViewCountPolicy {

    private final ViewCountPolicy delegate;

    @Override
    public boolean shouldCount(Long postId, ViewContext context) {
        boolean result = delegate.shouldCount(postId, context);

        if (!result) {
            log.info("View count filtered - postId: {}, reason: {}, context: {}",
                postId, getFilterReason(), context);
        }

        return result;
    }
}
```

## 비교 요약

| 정책 | 구현 난이도 | 정확도 | 인프라 | 실제 사용 |
|------|------------|--------|--------|----------|
| 봇 필터링 | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | 없음 | 필수 |
| 시간 기반 중복 제거 (로컬) | ⭐⭐⭐⭐ | ⭐⭐⭐ | 없음 | 권장 (MVP) |
| 시간 기반 중복 제거 (Redis) | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ | Redis | 권장 (프로덕션) |
| 일일 중복 제거 | ⭐⭐⭐ | ⭐⭐⭐ | Redis | 선택적 |
| 작성자 제외 | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | 없음 | 선택적 |
| 이상 탐지 | ⭐⭐ | ⭐⭐⭐⭐⭐ | Redis | 고급 |

## 결론

**권장 구현 순서:**
1. ✅ **Phase 1**: 봇 필터링 + 로컬 캐시 중복 제거 (30분)
2. ⏭ **Phase 2**: Redis 기반 중복 제거 (다중 서버 환경)
3. 🔄 **Phase 3**: 비즈니스 요구에 따라 추가 정책

**핵심 원칙:**
- Policy Pattern으로 유연한 확장
- 비동기 처리로 성능 보장
- 단계적 고도화 (과도한 설계 지양)

## 참고 자료
- [YouTube View Count Algorithm](https://support.google.com/youtube/answer/2991785)
- [Redis TTL and Expiration](https://redis.io/commands/expire/)
- [Caffeine Cache](https://github.com/ben-manes/caffeine)
- [Spring Events](https://spring.io/blog/2015/02/11/better-application-events-in-spring-framework-4-2)
