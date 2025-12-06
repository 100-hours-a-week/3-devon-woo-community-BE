# 검증 계층 분리 가이드

> DDD 기반 Spring 프로젝트의 검증 책임 분리 원칙

## 1. 핵심 개념

### 불변식(Invariant) vs 비즈니스 규칙

| 구분 | 책임 주체 | 예시 |
|------|----------|------|
| ✅ **도메인 불변식** | Entity 내부 | 닉네임 비어있으면 안됨 / 비밀번호 20자 이하 |
| ⚠️ **비즈니스 규칙** | Domain Service | 이메일 중복 불가 / 탈퇴 회원 복구 정책 |
| ❌ **입력 유효성** | Controller/DTO | 이메일 형식 / 비밀번호 최소 길이 |

**핵심 원칙**:
- Entity: "객체가 유효한 상태로 존재할 수 있는가"
- DTO/Validator: "요청이 유효한 형식으로 들어왔는가"

---

## 2. 계층별 검증 책임

| 계층 | 검증 목적 | 예외 타입 | 메시지 관리 | 검증 도구 |
|------|----------|----------|------------|----------|
| **DTO** | 사용자 입력 형식 | `MethodArgumentNotValidException` | 하드코딩/메시지파일 | `@NotBlank`, `@Email`, `@Size` |
| **Service** | 비즈니스 규칙 위반 | `CustomException` | ErrorCode 사용 | 중복 체크, 권한 검증 |
| **Entity** | 도메인 불변식 보장 | `IllegalArgumentException` | 하드코딩 OK | `Assert.hasText()` |

---

## 3. 검증 흐름

```
[사용자 요청]
    ↓
[DTO Validation] ← @Valid, Bean Validation
    ↓  (형식 검증)
[Service Layer] ← 비즈니스 규칙 (중복 체크, 정책 검증)
    ↓  (비즈니스 로직)
[Entity] ← 도메인 불변식 (최종 방어선)
    ↓  (상태 무결성)
[DB]
```

---

## 4. 실무 코드 예시

### DTO (입력 검증)

```java
public record MemberCreateRequest(
    @NotBlank(message = "이메일은 필수입니다.")
    @Email(message = "이메일 형식이 올바르지 않습니다.")
    String email,

    @NotBlank(message = "비밀번호는 필수입니다.")
    @Size(min = 8, max = 20, message = "비밀번호는 8~20자여야 합니다.")
    String password,

    @NotBlank(message = "닉네임은 필수입니다.")
    @Size(max = 10, message = "닉네임은 10자 이하여야 합니다.")
    String nickname,

    @Size(max = 500, message = "프로필 이미지 URL은 500자 이하여야 합니다.")
    String profileImageUrl
) {}
```

**특징**:
- 사용자에게 친절한 메시지
- 형식/길이/필수 여부만 검증
- 비즈니스 규칙은 검증하지 않음

---

### Service (비즈니스 규칙)

```java
@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    public Member createMember(MemberCreateRequest request) {
        // 1. 비즈니스 규칙 검증
        validateEmailDuplication(request.email());
        validateNicknameDuplication(request.nickname());

        // 2. 인프라 로직 (암호화)
        String encodedPassword = passwordEncoder.encode(request.password());

        // 3. 엔티티 생성 (내부 불변식은 엔티티에서 검증)
        Member member = Member.create(
            request.email(),
            encodedPassword,
            request.nickname()
        );

        return memberRepository.save(member);
    }

    private void validateEmailDuplication(String email) {
        if (memberRepository.existsByEmail(email)) {
            throw new CustomException(MemberErrorCode.DUPLICATE_EMAIL);
        }
    }

    private void validateNicknameDuplication(String nickname) {
        if (memberRepository.existsByNickname(nickname)) {
            throw new CustomException(MemberErrorCode.DUPLICATE_NICKNAME);
        }
    }
}
```

**특징**:
- 복수 객체 간 규칙 (중복 체크)
- ErrorCode 기반 예외 처리
- 인프라 계층 활용 (암호화 등)

---

### Entity (도메인 불변식)

```java
@Entity
@Getter
public class Member {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false, length = 20)
    private String password;

    @Column(nullable = false, length = 10)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberStatus status;

    // 팩토리 메서드 - 생성 시 불변식 검증
    public static Member create(String email, String password, String nickname) {
        Assert.hasText(email, "email required");
        Assert.hasText(password, "password required");
        Assert.hasText(nickname, "nickname required");

        if (password.length() > 20) {
            throw new IllegalArgumentException("password too long");
        }
        if (nickname.length() > 10) {
            throw new IllegalArgumentException("nickname too long");
        }

        return Member.builder()
                .email(email)
                .password(password)
                .nickname(nickname)
                .status(MemberStatus.ACTIVE)
                .build();
    }

    // 변경 메서드 - 변경 시 불변식 검증
    public void changeNickname(String nickname) {
        Assert.hasText(nickname, "nickname required");
        if (nickname.length() > 10) {
            throw new IllegalArgumentException("nickname too long");
        }
        this.nickname = nickname;
    }

    public void changePassword(String password) {
        Assert.hasText(password, "password required");
        if (password.length() > 20) {
            throw new IllegalArgumentException("password too long");
        }
        this.password = password;
    }
}
```

**특징**:
- 단일 객체의 상태 무결성만 검증
- 개발자용 간결한 메시지
- `IllegalArgumentException` 사용

---

## 5. 검증 유틸 선택 가이드

### Spring Assert (권장)

```java
Assert.hasText(password, "password required");
```

**장점**: Spring Core 포함, 가장 간결하고 표준적

### Apache Commons Validate

```java
Validate.notBlank(password, "password required");
```

**장점**: Spring 없이 단독 사용 가능
**단점**: 외부 의존성 필요 (`commons-lang3`)

### Custom Utility

```java
public final class Validation {
    public static String requireNonBlank(String value, String message) {
        Objects.requireNonNull(value, message);
        if (value.isBlank()) throw new IllegalArgumentException(message);
        return value;
    }
}

// 사용
this.password = Validation.requireNonBlank(password, "password required");
```

**장점**: 완전 통제, 의존성 없음

### Optional 방식 (비권장)

```java
this.password = Optional.ofNullable(password)
    .filter(p -> !p.isBlank())
    .orElseThrow(() -> new IllegalArgumentException("password required"));
```

**단점**: 가독성 논쟁, 성능 오버헤드

---

## 6. 예외 메시지 관리 원칙

| 계층 | 메시지 관리 | 이유 | 노출 대상 |
|------|------------|------|----------|
| **Entity** | 하드코딩 ✅ | 개발자용 디버깅 메시지 | 개발자 |
| **Service** | ErrorCode ✅ | 사용자 응답 변환 | 사용자 |
| **DTO** | 하드코딩/메시지파일 ⚠️ | 사용자 직접 노출 | 사용자 |

### Entity 메시지 예시

```java
// ✅ 좋은 예 - 간결한 개발자용 메시지
Assert.hasText(nickname, "nickname required");
throw new IllegalArgumentException("password too long");

// ❌ 나쁜 예 - 사용자용 메시지
throw new CustomException(MemberErrorCode.INVALID_NICKNAME);
```

### Service 메시지 예시

```java
// ✅ 좋은 예 - ErrorCode 사용
if (memberRepository.existsByEmail(email)) {
    throw new CustomException(MemberErrorCode.DUPLICATE_EMAIL);
}

// ❌ 나쁜 예 - 하드코딩
if (memberRepository.existsByEmail(email)) {
    throw new IllegalArgumentException("이메일이 중복되었습니다.");
}
```

---

## 7. 실무 베스트 프랙티스

### 원칙

1. **계층 분리**: 각 계층이 자신의 책임만 검증
2. **중복 허용**: DTO + Entity 이중 검증은 Fail-safe 전략 (정상)
3. **예외 변환**: Entity의 `IllegalArgumentException`을 Service에서 `CustomException`으로 변환 가능
4. **명확한 메시지**: 개발자용 vs 사용자용 구분

### GlobalExceptionHandler 예시

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    // DTO 검증 실패
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(MethodArgumentNotValidException e) {
        return ResponseEntity.badRequest()
            .body(ErrorResponse.from(e.getBindingResult()));
    }

    // 비즈니스 규칙 위반
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<?> handleCustom(CustomException e) {
        return ResponseEntity.status(e.getErrorCode().getStatus())
            .body(ErrorResponse.from(e.getErrorCode()));
    }

    // 엔티티 불변식 위반 (개발 오류)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.badRequest()
            .body(ErrorResponse.from(ErrorCode.INVALID_ARGUMENT, e.getMessage()));
    }
}
```

---

## 8. 피해야 할 안티패턴

### ❌ 모든 검증을 엔티티에 몰아넣기

```java
// 나쁜 예 - 엔티티가 Repository를 의존
public static Member create(String email, MemberRepository repo) {
    if (repo.existsByEmail(email)) {  // ❌ 엔티티가 인프라 의존
        throw new CustomException(MemberErrorCode.DUPLICATE_EMAIL);
    }
    ...
}
```

**올바른 방법**: Service에서 검증

---

### ❌ 엔티티에서 CustomException 직접 사용

```java
// 나쁜 예 - 도메인이 인프라 계층에 의존
public void changeNickname(String nickname) {
    if (nickname == null) {
        throw new CustomException(MemberErrorCode.INVALID_NICKNAME);  // ❌
    }
}
```

**올바른 방법**: `IllegalArgumentException` 사용 후 상위에서 변환

---

### ❌ create 메서드에서 검증 생략

```java
// 나쁜 예 - "DTO에서 검증했으니까" 생략
public static Member create(String email, String password, String nickname) {
    return Member.builder()  // ❌ 검증 없음
            .email(email)
            .password(password)
            .nickname(nickname)
            .build();
}
```

**올바른 방법**: Entity는 항상 유효한 상태로만 존재해야 함

---

## 9. 요약

| 항목 | DTO | Service | Entity |
|------|-----|---------|--------|
| **검증 대상** | 입력 형식 | 비즈니스 규칙 | 도메인 불변식 |
| **예외 타입** | `MethodArgumentNotValidException` | `CustomException` | `IllegalArgumentException` |
| **메시지** | 사용자용 | ErrorCode | 개발자용 |
| **검증 도구** | Bean Validation | Repository 조회 | `Assert.hasText()` |
| **책임 범위** | 단일 필드 | 복수 객체/정책 | 단일 객체 상태 |

### 핵심 한 줄 요약

> DTO로 입력 검증 → Service로 비즈니스 규칙 → Entity로 불변식 보장 (최종 방어선)
