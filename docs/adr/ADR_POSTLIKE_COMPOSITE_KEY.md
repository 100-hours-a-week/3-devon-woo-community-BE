# ADR: PostLike 엔티티의 복합키 설계

## 상태
채택됨 (2025-10-30)

## 컨텍스트
PostLike 엔티티는 게시글(Post)과 회원(Member) 간의 좋아요 관계를 나타내는 연관 테이블입니다.
이러한 다대다 관계 테이블의 Primary Key 설계 방식에는 두 가지 주요 접근법이 있습니다:

1. **단일 ID + 복합 유니크 인덱스 방식**
2. **복합키(@EmbeddedId) 방식**

## 결정
**복합키(@EmbeddedId) 방식을 채택**합니다.

```java
@EmbeddedId
private PostLikeId id;

@MapsId("postId")
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "post_id", nullable = false)
private Post post;

@MapsId("memberId")
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "member_id", nullable = false)
private Member member;
```

## 이유

### PostLike의 특성 분석
- **쓰기 작업(INSERT/DELETE)이 압도적으로 많음**: 사용자가 좋아요를 누르거나 취소하는 빈번한 작업
- **읽기 작업은 상대적으로 적음**: 주로 좋아요 개수 집계 정도
- **다른 테이블에서 PostLike를 참조할 일이 거의 없음**: 독립적인 연관 테이블
- **postId + memberId가 본질적으로 자연스러운 Primary Key**

### 두 방식 비교

| 항목 | 단일 ID + 유니크 인덱스 | 복합키 (@EmbeddedId) |
|------|------------------------|---------------------|
| 인덱스 개수 | 2개 (PK + 유니크) | 1개 (PK) |
| INSERT 성능 | 낮음 (2개 인덱스 업데이트) | 높음 (1개 인덱스만) |
| DELETE 성능 | 낮음 (2개 인덱스 업데이트) | 높음 (1개 인덱스만) |
| 저장 공간 | 추가 컬럼(id) 필요 | 절약 |
| 쿼리 복잡도 | 간단 | 약간 복잡 |
| 다른 테이블 참조 | 쉬움 | 어려움 |
| 비즈니스 의미 | 인위적 | 자연스러움 |

### 결정 근거
1. **성능 최적화**: 쓰기 작업이 빈번한 PostLike에서 인덱스 오버헤드 최소화가 중요
2. **저장 공간 효율**: 대용량 데이터가 예상되는 좋아요 테이블에서 불필요한 컬럼 제거
3. **참조 불필요**: PostLike를 외래키로 참조하는 다른 테이블이 없음
4. **비즈니스 로직 일치**: (postId, memberId) 조합이 본질적으로 유일성을 보장

## 결과

### 긍정적 영향
- INSERT/DELETE 성능 향상 (인덱스 업데이트 오버헤드 50% 감소)
- 저장 공간 절약 (Long 타입 컬럼 1개 제거)
- 인덱스 단편화 감소
- 비즈니스 도메인과 일치하는 자연스러운 설계

### 부정적 영향 (트레이드오프)
- Repository 메서드 사용 시 복잡도 증가
  ```java
  // 조회 예시
  PostLikeId id = PostLikeId.create(postId, memberId);
  postLikeRepository.findById(id);
  ```
- JPA 초보 개발자에게 학습 곡선 존재

## 참고사항
- 만약 PostLike를 다른 엔티티에서 참조해야 하는 요구사항이 생긴다면, 단일 ID 방식으로 재고려 필요
- 대부분의 연관 테이블(Join Table)은 단일 ID 방식을 사용하지만, PostLike는 쓰기 성능이 중요한 특수 케이스
