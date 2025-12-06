# @CurrentUser 사용 가이드

## 개요
`@CurrentUser` 애노테이션을 사용하면 컨트롤러에서 현재 인증된 사용자의 ID를 간편하게 가져올 수 있습니다.

## 사용 방법

### 기본 사용
```java
@GetMapping("/me")
public ResponseEntity<?> getMyInfo(@CurrentUser Long userId) {
    // userId를 사용하여 비즈니스 로직 처리
    Member member = memberService.findById(userId);
    return ResponseEntity.ok(member);
}

@PostMapping("/posts")
public ResponseEntity<?> createPost(
    @CurrentUser Long userId,
    @RequestBody PostCreateRequest request
) {
    Post post = postService.create(userId, request);
    return ResponseEntity.ok(post);
}
```

## 주요 특징

### 1. 자동 파라미터 주입
- SecurityContext에서 인증 정보를 자동으로 추출
- UserId만 간단하게 전달

### 2. Swagger 숨김 처리
- `@Parameter(hidden = true)` 적용
- API 문서에서 파라미터 표시 안 됨

### 3. 미인증 시 동작
- 인증되지 않은 경우 `null` 반환
- 인증 필수 엔드포인트는 Security 설정으로 보호

## 구현 구조

```
@CurrentUser 애노테이션
    ↓
CurrentUserArgumentResolver
    ↓
SecurityContext에서 인증 정보 추출
    ↓
CustomUserDetails.getMemberId() 반환
```

## 파일 구조
- `@CurrentUser`: 애노테이션 정의
- `CurrentUserArgumentResolver`: 파라미터 리졸버 구현
- `WebConfig`: ArgumentResolver 등록
