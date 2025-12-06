# @CurrentUser 구현 가이드

## 구현 개요
컨트롤러에서 현재 인증된 사용자의 ID를 자동으로 주입받기 위한 ArgumentResolver 패턴 구현

## 핵심 구성 요소

### 1. @CurrentUser 애노테이션
```java
@Parameter(hidden = true)  // Swagger 문서에서 숨김
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUser {
}
```
- Swagger의 `@Parameter(hidden = true)` 사용 (Hibernate Parameter 아님)
- 메서드 파라미터에만 적용

### 2. CurrentUserArgumentResolver
```java
@Component
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class)
                && parameter.getParameterType().equals(Long.class);
    }

    @Override
    public Object resolveArgument(...) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal().equals("anonymousUser")) {
            return null;
        }
        CustomUserDetails user = (CustomUserDetails) auth.getPrincipal();
        return user.getMemberId();
    }
}
```
- SecurityContext에서 인증 정보 추출
- UserId만 반환

### 3. WebConfig 등록
```java
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final CurrentUserArgumentResolver currentUserArgumentResolver;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentUserArgumentResolver);
    }
}
```

## 왜 UserId만 전달하는가?

### 1. Controller 책임 분리
Controller는 HTTP 요청을 도메인으로 변환하는 경계 계층이다. 최소한의 정보만 받아 Service를 호출하는 것이 단순하고 안전하다.
- UserId만으로 충분
- 권한, 이메일 등은 Service에서 필요시 조회

### 2. 보안 안정성
UserDetails 객체를 직접 전달하면 email, roles 등 민감 정보가 Service 계층으로 노출된다.
- UserId는 비민감 식별자
- 최소 권한 원칙 준수

### 3. 유지보수성
UserDetails 구조, Role 체계, 인증 정책 변경 시에도 Controller는 영향을 받지 않는다.
- Controller는 `Long userId`만 의존
- 변경 영향 범위 최소화

### 4. Domain 중심 설계
도메인 서비스는 ID 기반으로 엔티티를 탐색하고 조작한다.
```java
service.getProfile(userId)
service.updatePost(userId, request)
```
- DDD 원칙과 정확히 부합
- 도메인 모델의 독립성 유지

## 사용 예시
```java
@PostMapping("/posts")
public ResponseEntity<?> createPost(
    @CurrentUser Long userId,
    @RequestBody PostCreateRequest request
) {
    Post post = postService.create(userId, request);
    return ResponseEntity.ok(post);
}
```
