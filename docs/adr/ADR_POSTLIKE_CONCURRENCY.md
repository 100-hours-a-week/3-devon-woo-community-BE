# ADR: 게시글 좋아요 동시성 처리 전략

## 상태
채택됨 (Accepted)

## 컨텍스트
게시글 좋아요 기능에서 여러 사용자가 동시에 같은 게시글에 좋아요를 추가/취소할 때, `Post` 엔티티의 `likeCount` 필드를 안전하게 업데이트해야 하는 동시성 제어 문제가 발생했다.

### 문제 상황
초기 구현에서는 영속성 컨텍스트를 통한 단순한 카운트 업데이트 방식을 사용했다:

```java
// 초기 구현 - 더티 체킹 방식
Post post = postRepository.findById(postId).orElseThrow();
post.incrementLikeCount();  // 또는 decrementLikeCount()
// 트랜잭션 커밋 시 자동으로 UPDATE
```

**발생한 문제: 더티 리드(Dirty Read)로 인한 동시성 문제**

```
시간  |  스레드 A              |  스레드 B
-----|----------------------|----------------------
t1   |  post 조회 (count=0) |
t2   |                      |  post 조회 (count=0)
t3   |  count++ (1)         |
t4   |                      |  count++ (1)
t5   |  UPDATE count=1      |
t6   |                      |  UPDATE count=1
결과  |  최종 count = 1 (기대값: 2)
```

여러 트랜잭션이 동일한 초기값을 읽고 각자 증가시켜 마지막 업데이트만 반영되는 **Lost Update** 문제가 발생한다.

## 고려한 대안들

### 1. 비관적 락 (Pessimistic Lock)
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT p FROM Post p WHERE p.id = :postId")
Optional<Post> findByIdWithLock(@Param("postId") Long postId);
```

**장점:**
- 구현이 단순하고 직관적
- 데이터 일관성이 강력하게 보장됨
- 충돌이 자주 발생하는 상황에서 효과적

**단점:**
- 데드락(Deadlock) 발생 가능성
- 락을 기다리는 동안 다른 트랜잭션이 블로킹됨
- 처리량(Throughput)이 낮아짐
- 좋아요와 같은 높은 동시성 요구사항에는 부적합

### 2. 낙관적 락 (Optimistic Lock)
```java
@Entity
public class Post {
    @Version
    private Long version;
    // ...
}
```

**장점:**
- 락을 획득하지 않아 성능이 좋음
- 데드락이 발생하지 않음
- 충돌이 적은 환경에서 효과적

**단점:**
- 충돌 시 재시도 로직 필요 (복잡도 증가)
- 충돌이 빈번한 경우 재시도가 반복되어 오히려 성능 저하
- 좋아요처럼 충돌이 자주 발생할 수 있는 작업에는 부적합
- 클라이언트에게 실패를 전달하고 재시도를 요구할 수 있음

### 3. 원자적 업데이트 (Atomic Update) ✅ 채택
```java
@Modifying
@Query("UPDATE Post p SET p.likeCount = p.likeCount + 1 WHERE p.id = :postId")
void incrementLikeCount(@Param("postId") Long postId);

@Modifying
@Query("UPDATE Post p SET p.likeCount = p.likeCount - 1 WHERE p.id = :postId")
void decrementLikeCount(@Param("postId") Long postId);
```

**장점:**
- 데이터베이스 수준에서 원자성 보장 (단일 SQL 문으로 실행)
- 락을 사용하지 않아 성능이 우수
- 동시성이 높은 환경에서도 안정적
- 구현이 간단하고 명확
- Lost Update 문제 완전 해결

**단점:**
- 엔티티가 영속성 컨텍스트에 로드되어 있는 경우 불일치 발생 가능
  - 해결책: 벌크 연산 후 `em.clear()` 또는 필요 시 재조회
- 복잡한 비즈니스 로직 적용이 어려움 (단순 증감에만 적합)

## 결정
**원자적 업데이트(Atomic Update) 방식을 채택**

### 선택 이유

1. **성능**
   - 락을 사용하지 않아 블로킹이 발생하지 않음
   - 단일 SQL UPDATE 문으로 실행되어 효율적
   - 높은 동시 요청에도 안정적인 처리량 유지

2. **안정성**
   - 데이터베이스의 원자성 보장으로 Lost Update 문제 완전 해결
   - 재시도 로직 불필요
   - 데드락 발생 가능성 없음

3. **단순성**
   - 구현이 직관적이고 이해하기 쉬움
   - 추가적인 예외 처리나 재시도 로직 불필요
   - 유지보수 용이

4. **적합성**
   - 좋아요는 단순 카운트 증감 작업으로 원자적 업데이트에 최적
   - 높은 동시성이 예상되는 기능에 적합

## 구현 상세

### PostRepository
```java
@Modifying
@Query("UPDATE Post p SET p.likeCount = p.likeCount + 1 WHERE p.id = :postId")
void incrementLikeCount(@Param("postId") Long postId);

@Modifying
@Query("UPDATE Post p SET p.likeCount = p.likeCount - 1 WHERE p.id = :postId")
void decrementLikeCount(@Param("postId") Long postId);
```

### PostLikeService
```java
@Transactional
public void likePost(Long postId, Long memberId) {
    Post post = findPostById(postId);
    Member member = findMemberById(memberId);

    postLikePolicyValidator.checkNotAlreadyLiked(postId, memberId);

    postLikeRepository.save(PostLike.create(post, member));
    postRepository.incrementLikeCount(postId);  // 원자적 업데이트
}

@Transactional
public void unlikePost(Long postId, Long memberId) {
    findPostById(postId);
    postLikePolicyValidator.checkLikeExists(postId, memberId);

    postLikeRepository.deleteByPostIdAndMemberId(postId, memberId);
    postRepository.decrementLikeCount(postId);  // 원자적 업데이트
}
```

### API 응답
- 204 No Content 상태 코드만 반환
- 응답 바디 없음 (PostLikeResponse 제거)
- 이점:
  - `likeCount` 조회를 위한 추가 SELECT 쿼리 제거
  - flush 관리 불필요
  - API 단순화

## 테스트 전략

### 동시성 테스트
```java
@Test
@DisplayName("여러 사용자가 동시에 좋아요 추가 - 원자적 업데이트 검증")
void likePost_ConcurrentLikes_AtomicUpdate() throws InterruptedException {
    int numberOfThreads = 10;
    ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
    CountDownLatch latch = new CountDownLatch(numberOfThreads);

    // 10명이 동시에 좋아요
    for (int i = 0; i < numberOfThreads; i++) {
        executorService.execute(() -> {
            try {
                postLikeService.likePost(TEST_POST_ID, uniqueMemberId);
                successCount.incrementAndGet();
            } finally {
                latch.countDown();
            }
        });
    }

    latch.await();

    // 검증: 정확히 10개의 좋아요가 추가되어야 함
    Post post = postRepository.findById(TEST_POST_ID).orElseThrow();
    assertThat(post.getLikeCount()).isEqualTo(10);
}
```

## 향후 고려사항

### 1. 비동기 처리 및 최종 일관성 (Eventual Consistency)
트래픽이 더욱 증가하면 다음과 같은 방식을 고려할 수 있다:

```java
// Redis나 메시지 큐를 활용한 비동기 처리
@Transactional
public void likePost(Long postId, Long memberId) {
    // 1. PostLike 엔티티만 저장 (빠른 응답)
    postLikeRepository.save(PostLike.create(post, member));

    // 2. 비동기로 카운트 업데이트 (이벤트 발행 또는 메시지 큐)
    eventPublisher.publishEvent(new PostLikedEvent(postId));
    // 또는
    likeCountQueue.increment(postId);
}

// 별도의 워커가 주기적으로 배치 업데이트
@Scheduled(fixedRate = 1000)  // 1초마다
public void flushLikeCounts() {
    Map<Long, Integer> counts = likeCountQueue.drainAll();
    counts.forEach((postId, delta) -> {
        postRepository.incrementLikeCountBy(postId, delta);
    });
}
```

**장점:**
- 응답 시간 최소화
- 데이터베이스 부하 감소 (배치 처리)
- 더 높은 처리량

**단점:**
- 복잡도 증가 (Redis, 메시지 큐 등 추가 인프라 필요)
- 실시간 정합성 보장 불가 (최종 일관성)
- 실패 처리 로직 필요

**적용 시점:**
- 현재는 원자적 업데이트로 충분
- 트래픽이 현저히 증가하여 성능 병목이 발생할 때 검토

### 2. 읽기 성능 최적화
좋아요 수를 자주 조회하는 경우:
- Redis 캐시 활용
- CQRS 패턴으로 읽기 전용 모델 분리

## 결과
- ✅ Lost Update 문제 해결
- ✅ 높은 동시성 환경에서 안정적 동작
- ✅ 성능 우수 (락 없음)
- ✅ 구현 및 유지보수 용이
- ✅ 모든 동시성 테스트 통과

## 참고 자료
- [Spring Data JPA @Modifying Queries](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#jpa.modifying-queries)
- [Database Concurrency Control](https://en.wikipedia.org/wiki/Concurrency_control)
- [Optimistic vs Pessimistic Locking](https://vladmihalcea.com/optimistic-vs-pessimistic-locking/)
