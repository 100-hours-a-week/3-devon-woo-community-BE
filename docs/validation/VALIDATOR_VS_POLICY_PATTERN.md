# Validator vs Policy 패턴 가이드

> DDD 기반 Spring Boot 프로젝트의 검증 레이어 분리 및 네이밍 컨벤션

---

## 핵심 질문으로 구분하기

### Validator
**"요청이 시스템에 들어오기 전에 유효한가?"**
- 이메일 형식이 올바른가?
- 필수 필드가 누락되지 않았는가?
- 이미 존재하는 데이터인가? (중복 체크)
- 입력값의 길이/범위가 적절한가?

### Policy
**"비즈니스적으로 허용되는 행동인가?"**
- 자기 글에 좋아요를 누를 수 있는가?
- 하루 N번 이상 글을 작성할 수 있는가?
- 삭제된 게시글을 수정할 수 있는가?
- 블록한 사용자의 글에 댓글을 달 수 있는가?

---

## 1. 핵심 차이점

| 구분 | Validator | Policy |
|------|-----------|--------|
| **의도** | 입력/요청의 유효성 검증 | 도메인 규칙 검증 |
| **관점** | "요청이 올바른가?" | "행동이 허용되는가?" |
| **레이어** | Application Layer | Domain Layer |
| **위치** | `application.[context].validator` | `domain.[context].policy` |
| **검증 대상** | 외부 요청 데이터 | 도메인 객체의 상태/행위 |
| **예시** | 이메일 형식, 중복 체크 | 자기글 좋아요 불가, 일일 제한 |

### 검증 흐름

```
Controller
    ↓
Validator (전처리: 입력값 검증)
    ↓
Application Service
    ↓
Policy (비즈니스 규칙 검증)
    ↓
Entity (불변식 보장)
```

---

## 2. 패키지 구조

```
com.devon.techblog
├── application
│   └── post
│       ├── PostService.java
│       └── validator              ← 입력값 검증
│           └── PostCreateValidator.java
│
└── domain
    └── post
        ├── entity
        │   └── Post.java
        └── policy                  ← 비즈니스 규칙 검증
            ├── PostLikePolicy.java
            └── ViewCountPolicy.java
```

---

## 3. 네이밍 컨벤션

### Validator

| 항목 | 컨벤션 | 예시 |
|------|--------|------|
| **패키지** | `application.[context].validator` | `application.post.validator` |
| **클래스** | `*Validator` | `PostCreateValidator` |
| **메서드** | `validate()` | `validate(request)` |

```java
@Component
public class PostCreateValidator {
    public void validate(PostCreateRequest request) {
        if (request.getTitle().length() > 100) {
            throw new IllegalArgumentException("제목은 100자 이내여야 합니다.");
        }
    }
}
```

### Policy

| 항목 | 컨벤션 | 예시 |
|------|--------|------|
| **패키지** | `domain.[context].policy` | `domain.post.policy` |
| **클래스** | `*Policy` | `PostLikePolicy`, `ViewCountPolicy` |
| **메서드** | `validateCan[Action]()`, `should[Action]()` | `validateCanLike()`, `shouldCount()` |

```java
@Component
public class PostLikePolicy {
    public void validateCanLike(Post post, Member member) {
        if (post.hasLikedBy(member)) {
            throw new IllegalStateException("이미 좋아요를 누른 게시글입니다.");
        }
    }
}

@Component
public class ViewCountPolicy {
    public boolean shouldCount(Long postId, ViewContext context) {
        if (isBot(context.getUserAgent())) {
            return false;
        }
        return true;
    }
}
```

---

## 4. 실무 적용 예시

### 예시 1: 회원가입

```java
// Validator: 입력값 검증
@Component
public class SignupValidator {
    private final MemberRepository memberRepository;

    public void validate(SignupRequest request) {
        // 형식 검증
        if (!EMAIL_PATTERN.matcher(request.getEmail()).matches()) {
            throw new IllegalArgumentException("올바른 이메일 형식이 아닙니다.");
        }

        // 중복 체크 (DB 조회 기반 검증)
        if (memberRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        // 길이 검증
        if (request.getPassword().length() < 8) {
            throw new IllegalArgumentException("비밀번호는 8자 이상이어야 합니다.");
        }
    }
}

// Policy는 불필요 (회원가입은 주로 입력값 검증)
```

### 예시 2: 게시글 좋아요

```java
// Validator: 요청 검증
@Component
public class PostLikeValidator {
    private final PostRepository postRepository;

    public void validate(Long postId) {
        // 존재하는 게시글인지 확인
        if (!postRepository.existsById(postId)) {
            throw new IllegalArgumentException("존재하지 않는 게시글입니다.");
        }
    }
}

// Policy: 비즈니스 규칙 검증
@Component
public class PostLikePolicy {
    public void validateCanLike(Post post, Member member) {
        // 자기 글 좋아요 방지
        if (post.isOwner(member)) {
            throw new PolicyViolationException("자신의 글에는 좋아요를 누를 수 없습니다.");
        }

        // 중복 좋아요 방지
        if (post.isLikedBy(member)) {
            throw new PolicyViolationException("이미 좋아요를 누른 게시글입니다.");
        }
    }
}

// Service: 전체 흐름
@Service
@RequiredArgsConstructor
public class PostLikeService {
    private final PostLikeValidator validator;
    private final PostLikePolicy policy;
    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;

    @Transactional
    public void likePost(Long postId, Member member) {
        validator.validate(postId);  // 1. 입력값 검증

        Post post = postRepository.findById(postId).orElseThrow();
        policy.validateCanLike(post, member);  // 2. 정책 검증

        postLikeRepository.save(PostLike.create(post, member));  // 3. 실행
        post.incrementLikeCount();
    }
}
```

### 예시 3: 게시글 작성

```java
// Validator: 입력값 검증
@Component
public class PostCreateValidator {
    public void validate(PostCreateRequest request) {
        if (request.getTitle().length() > 100) {
            throw new IllegalArgumentException("제목은 100자 이내여야 합니다.");
        }

        if (request.getContent().length() > 10000) {
            throw new IllegalArgumentException("본문은 10000자 이내여야 합니다.");
        }

        // XSS 필터링
        if (containsScript(request.getContent())) {
            throw new IllegalArgumentException("허용되지 않는 문자가 포함되어 있습니다.");
        }
    }
}

// Policy: 작성 제한 규칙
@Component
public class PostCreatePolicy {
    private final PostRepository postRepository;

    public void validateCanCreate(Member member) {
        // 하루 작성 제한
        LocalDate today = LocalDate.now();
        long todayPostCount = postRepository.countByMemberAndCreatedAtAfter(
            member, today.atStartOfDay()
        );

        if (todayPostCount >= 10) {
            throw new PolicyViolationException("하루 작성 한도를 초과했습니다.");
        }

        // 탈퇴한 회원 체크
        if (member.isDeleted()) {
            throw new PolicyViolationException("탈퇴한 회원은 글을 작성할 수 없습니다.");
        }
    }
}
```

---

## 5. 안티패턴

### ❌ Validator에서 도메인 상태 검증

```java
// 나쁜 예
@Component
public class PostUpdateValidator {
    public void validate(Long postId) {
        Post post = postRepository.findById(postId).orElseThrow();
        if (post.isDeleted()) {  // ❌ 도메인 상태 검증
            throw new IllegalStateException("삭제된 게시글입니다.");
        }
    }
}

// 좋은 예
@Component
public class PostEditPolicy {
    public void validateCanEdit(Post post, Member member) {
        if (post.isDeleted()) {  // ✅ Policy에서 검증
            throw new IllegalStateException("삭제된 게시글입니다.");
        }
    }
}
```

### ❌ Policy에서 입력값 형식 검증

```java
// 나쁜 예
@Component
public class PostLikePolicy {
    public void validateCanLike(Post post, String reason) {
        if (reason.length() > 100) {  // ❌ 입력값 검증
            throw new IllegalArgumentException("100자 이내여야 합니다.");
        }
    }
}

// 좋은 예
@Component
public class PostLikeValidator {
    public void validate(PostLikeRequest request) {
        if (request.getReason().length() > 100) {  // ✅ Validator에서 검증
            throw new IllegalArgumentException("100자 이내여야 합니다.");
        }
    }
}
```

---

## 6. 도메인별 예시 모음

### 회원 도메인

| 시나리오 | Validator | Policy |
|---------|-----------|--------|
| 회원가입 | 이메일 형식, 비밀번호 길이, 중복 체크 | - |
| 로그인 | 존재하는 회원인지, 비밀번호 일치 여부 | 탈퇴한 회원은 로그인 불가 |
| 프로필 수정 | 닉네임 길이, 허용된 이미지 포맷 | - |

### 게시글 도메인

| 시나리오 | Validator | Policy |
|---------|-----------|--------|
| 글 작성 | 제목/본문 길이, XSS 필터링 | 하루 N회 제한, 탈퇴 회원 불가 |
| 글 수정 | 제목/본문 길이 | 본인만 수정 가능, 삭제된 글 수정 불가 |
| 좋아요 | 게시글 존재 여부 | 자기 글 좋아요 불가, 중복 좋아요 불가 |
| 조회수 증가 | - | 봇 제외, 중복 조회 방지 |

### 전자상거래 도메인

| 시나리오 | Validator | Policy |
|---------|-----------|--------|
| 주문 | 유효한 상품 ID, 가격 조작 검증 | 재고 부족 시 주문 불가, 0원 결제 불가 |
| 쿠폰 발급 | 쿠폰 코드 형식 | 중복 발급 불가, 만료된 쿠폰 사용 불가 |
| 구독 | 결제 정보 유효성 | 이미 구독 중이면 불가, 무료체험 1회 제한 |

---

## 7. 판단 기준 체크리스트

### Validator로 가야 할 경우

- [ ] 외부에서 들어온 요청 데이터를 검증하는가?
- [ ] 필드의 형식/길이/범위를 확인하는가?
- [ ] DB에 이미 존재하는 데이터인지 확인하는가? (중복 체크)
- [ ] null/empty 체크를 하는가?
- [ ] 보안 필터링 (XSS, SQL Injection)을 하는가?

### Policy로 가야 할 경우

- [ ] 도메인 객체의 상태를 확인하는가?
- [ ] 비즈니스 규칙을 위반하는지 확인하는가?
- [ ] "할 수 있는가?" (Can~) 질문에 답하는가?
- [ ] 시간/횟수/권한 제약을 확인하는가?
- [ ] 도메인 모델 간의 관계를 검증하는가?

---

## 8. 실무 적용 가이드

### 작은 프로젝트
- Validator 위주로 시작
- 명확한 비즈니스 규칙이 생기면 Policy로 분리

### 성장 중인 프로젝트
- 중요한 규칙은 Policy로 격상
- Validator와 Policy를 명확히 분리

### 대형/복잡한 프로젝트 (금융/결제/구독)
- Policy 필수
- 도메인 전문가와 함께 Policy 설계
- 불변식은 Entity에서 보장

---

## 요약

### Validator
- **위치**: `application.[context].validator`
- **역할**: 외부 요청 전처리 (형식, 필수값, 중복)
- **핵심**: "요청이 시스템에 들어오기 전에 유효한가?"

### Policy
- **위치**: `domain.[context].policy`
- **역할**: 도메인 규칙 보호 (권한, 상태, 제약)
- **핵심**: "비즈니스적으로 허용되는 행동인가?"

### 핵심 원칙

> **Validator = API 입력 검증**
> **Policy = 도메인 행위 제약**
>
> **흐름: Validator(입력) → Policy(규칙) → Entity(불변식)**
