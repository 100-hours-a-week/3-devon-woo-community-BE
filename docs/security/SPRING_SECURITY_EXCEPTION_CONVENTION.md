# Spring Security 예외 처리 컨벤션

## 목차
1. [개요](#개요)
2. [Spring Security Filter Chain의 특수성](#spring-security-filter-chain의-특수성)
3. [예외 처리 전략](#예외-처리-전략)
4. [구현 상세](#구현-상세)
5. [예외 처리 플로우](#예외-처리-플로우)
6. [실무 권장사항](#실무-권장사항)

---

## 개요

Spring Security Filter Chain에서 발생하는 예외는 일반적인 @ControllerAdvice로 처리할 수 없습니다.
이 문서는 프로젝트에서 채택한 Spring Security 예외 처리 전략과 구현 방법을 정리합니다.

### 핵심 원칙
- **ErrorCode 기반 메시지 관리**: 하드코딩 금지, AuthErrorCode 사용
- **Spring Security 예외 체계 활용**: AuthenticationException, AccessDeniedException 상속
- **전역 예외 안전망 구축**: FilterChainExceptionHandler로 모든 예외 대응

---

## Spring Security Filter Chain의 특수성

### 왜 @ControllerAdvice가 작동하지 않는가?

```
Request → Filter Chain → DispatcherServlet → Controller
          ↑
          여기서 발생한 예외는 @ControllerAdvice가 잡지 못함
```

**이유:**
- Filter는 DispatcherServlet 이전에 실행
- @ControllerAdvice는 DispatcherServlet 내부에서만 동작
- Filter 예외는 서블릿 컨테이너 레벨에서 처리됨

**문제점:**
- 일반 RuntimeException 발생 시 HTML 500 에러 페이지 반환
- REST API 환경에서 JSON 응답 불가
- 클라이언트 파싱 실패 발생

---

## 예외 처리 전략

### 1. 커스텀 Security 예외 생성

#### 왜 기존 CustomException을 사용하지 않는가?

**기존 CustomException:**
```java
public class CustomException extends RuntimeException {
    private final ErrorCode errorCode;
}
```

**문제:**
- RuntimeException을 상속하므로 Spring Security의 예외 처리 체계에 포함되지 않음
- AuthenticationEntryPoint와 AccessDeniedHandler가 처리하지 못함

**해결:**
```java
public class CustomAuthenticationException extends AuthenticationException {
    private final ErrorCode errorCode;

    public CustomAuthenticationException(ErrorCode errorCode) {
        super(errorCode.getMessage());  // 메시지는 ErrorCode에서
        this.errorCode = errorCode;
    }
}

public class CustomAccessDeniedException extends AccessDeniedException {
    private final ErrorCode errorCode;

    public CustomAccessDeniedException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
```

**장점:**
- Spring Security 예외 체계 그대로 활용
- ErrorCode로 메시지 중앙 관리
- Handler에서 `getMessage()`만 호출하면 됨
- ErrorCode 의존성 불필요 (super로 전달)

### 2. AuthErrorCode 설계

#### 분리 기준

**현재: AuthErrorCode 하나로 통합 (23개)**
- JWT 토큰 관련 (6개)
- Refresh Token 관련 (3개)
- OAuth2 관련 (5개)
- 인증/인가 관련 (6개)
- 계정 상태 관련 (4개)

**분리 고려 시점:**
- 30-40개 이상: JwtErrorCode, OAuth2ErrorCode 분리 검토
- MSA 환경: 서비스별 분리
- 현재 프로젝트: **통합 유지**

#### AuthErrorCode 구조

```java
@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {

    // 기본 인증 (401)
    AUTHENTICATION_REQUIRED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "인증 정보가 유효하지 않습니다"),

    // JWT 토큰 (401)
    TOKEN_MISSING(HttpStatus.UNAUTHORIZED, "토큰이 제공되지 않았습니다"),
    TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다"),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다"),
    TOKEN_MALFORMED(HttpStatus.UNAUTHORIZED, "잘못된 형식의 토큰입니다"),
    TOKEN_UNSUPPORTED(HttpStatus.UNAUTHORIZED, "지원하지 않는 토큰 형식입니다"),
    TOKEN_SIGNATURE_INVALID(HttpStatus.UNAUTHORIZED, "토큰 서명 검증에 실패했습니다"),

    // Refresh Token (401)
    REFRESH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "유효하지 않은 리프레시 토큰입니다"),
    REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "리프레시 토큰이 만료되었습니다"),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "리프레시 토큰을 찾을 수 없습니다"),

    // OAuth2 (400/401/502)
    OAUTH2_AUTHENTICATION_FAILED(HttpStatus.UNAUTHORIZED, "OAuth2 인증에 실패했습니다"),
    OAUTH2_PROVIDER_ERROR(HttpStatus.BAD_GATEWAY, "OAuth2 제공자 오류가 발생했습니다"),
    OAUTH2_USER_INFO_ERROR(HttpStatus.BAD_GATEWAY, "사용자 정보를 가져올 수 없습니다"),
    OAUTH2_INVALID_STATE(HttpStatus.BAD_REQUEST, "잘못된 state 파라미터입니다"),
    OAUTH2_INVALID_CODE(HttpStatus.BAD_REQUEST, "잘못된 authorization code입니다"),

    // 인가 (403)
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "접근 권한이 없습니다"),
    INSUFFICIENT_PERMISSIONS(HttpStatus.FORBIDDEN, "권한이 부족합니다"),
    ROLE_NOT_FOUND(HttpStatus.FORBIDDEN, "사용자 권한을 찾을 수 없습니다"),

    // 계정 상태 (403)
    ACCOUNT_LOCKED(HttpStatus.FORBIDDEN, "계정이 잠겼습니다"),
    ACCOUNT_DISABLED(HttpStatus.FORBIDDEN, "비활성화된 계정입니다"),
    ACCOUNT_EXPIRED(HttpStatus.FORBIDDEN, "만료된 계정입니다"),
    CREDENTIALS_EXPIRED(HttpStatus.FORBIDDEN, "자격 증명이 만료되었습니다");

    private final HttpStatus httpStatus;
    private final String message;
}
```

### 3. FilterChainExceptionHandler 필요성

#### 왜 필요한가?

**Spring Security의 기본 예외 처리:**
- `AuthenticationException` → AuthenticationEntryPoint
- `AccessDeniedException` → AccessDeniedHandler
- **그 외 모든 예외** → 처리 안됨 (서블릿 컨테이너로 전달)

#### 처리되지 않는 예외 예시

```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(...) {
        String token = extractToken(request);

        // NullPointerException 발생 시?
        // IOException 발생 시?
        // RuntimeException 발생 시?
        // → 모두 HTML 500 페이지 반환
    }
}
```

#### 해결: FilterChainExceptionHandler

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class FilterChainExceptionHandler extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            filterChain.doFilter(request, response);
        } catch (AuthenticationException | AccessDeniedException e) {
            throw e;  // 기존 핸들러가 처리
        } catch (Exception e) {
            log.error("[FilterChain] Unexpected error: {}", e.getMessage(), e);

            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");

            ApiResponse<Object> apiResponse = ApiResponse.failure("서버 오류가 발생했습니다");
            response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
        }
    }
}
```

**핵심:**
1. 모든 필터보다 먼저 실행
2. AuthenticationException/AccessDeniedException는 그대로 전달
3. 그 외 모든 예외는 JSON 응답으로 변환

---

## 구현 상세

### 1. 디렉토리 구조

```
application/security/
├── exception/
│   ├── CustomAuthenticationException.java
│   └── CustomAccessDeniedException.java
├── handler/
│   ├── CustomAuthenticationEntryPoint.java
│   └── CustomAccessDeniedHandler.java
├── filter/
│   ├── FilterChainExceptionHandler.java
│   └── LoginAuthenticationFilter.java
└── config/
    └── SecurityConfig.java

common/exception/code/
└── AuthErrorCode.java
```

### 2. SecurityConfig 설정

```java
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomAuthenticationEntryPoint authenticationEntryPoint;
    private final CustomAccessDeniedHandler accessDeniedHandler;
    private final FilterChainExceptionHandler filterChainExceptionHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(...)

            /* 커스텀 로그인 필터 */
            .addFilterAt(
                createLoginAuthenticationFilter(authenticationManager()),
                UsernamePasswordAuthenticationFilter.class
            )

            /* Auth 예외 처리 핸들러 (401, 403) */
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler)
            )

            /* 필터 체인 전역 예외 처리 (모든 필터보다 먼저 실행) */
            .addFilterBefore(
                filterChainExceptionHandler,
                UsernamePasswordAuthenticationFilter.class
            );

        return http.build();
    }
}
```

**주의사항:**
- FilterChainExceptionHandler는 **모든 필터보다 먼저** 실행
- `addFilterBefore`를 제일 먼저 실행되는 필터 기준으로 설정

### 3. Handler 구현

#### CustomAuthenticationEntryPoint (401)

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {
        log.error("[AuthenticationEntryPoint] {}", authException.getMessage(), authException);

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ApiResponse<Object> apiResponse = ApiResponse.failure(authException.getMessage());
        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }
}
```

**핵심:**
- `authException.getMessage()`만 호출
- ErrorCode는 CustomAuthenticationException 생성 시점에 이미 메시지로 변환됨

#### CustomAccessDeniedHandler (403)

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException, ServletException {
        log.error("[AccessDeniedHandler] {}", accessDeniedException.getMessage(), accessDeniedException);

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ApiResponse<Object> apiResponse = ApiResponse.failure(accessDeniedException.getMessage());
        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }
}
```

### 4. 사용 예시

#### Service 레이어

```java
@Service
@RequiredArgsConstructor
public class LoginService implements UserDetailsService {

    @Override
    public UserDetails loadUserByUsername(String email) throws AuthenticationException {
        Member member = memberRepository.findByEmail(email)
            .orElseThrow(() -> new CustomAuthenticationException(
                AuthErrorCode.INVALID_CREDENTIALS
            ));

        if (member.getStatus() == MemberStatus.INACTIVE) {
            throw new CustomAuthenticationException(AuthErrorCode.ACCOUNT_DISABLED);
        }

        return new CustomUserDetails(member);
    }
}
```

#### Filter 레이어

```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(...) throws ServletException, IOException {
        String token = extractToken(request);

        if (token == null) {
            throw new CustomAuthenticationException(AuthErrorCode.TOKEN_MISSING);
        }

        try {
            Claims claims = jwtProvider.parseToken(token);
            // ...
        } catch (ExpiredJwtException e) {
            throw new CustomAuthenticationException(AuthErrorCode.TOKEN_EXPIRED);
        } catch (MalformedJwtException e) {
            throw new CustomAuthenticationException(AuthErrorCode.TOKEN_MALFORMED);
        } catch (SignatureException e) {
            throw new CustomAuthenticationException(AuthErrorCode.TOKEN_SIGNATURE_INVALID);
        }
    }
}
```

---

## 예외 처리 플로우

### 전체 예외 처리 흐름도

```
[Request]
    ↓
[FilterChainExceptionHandler]  ← 모든 필터보다 먼저 실행
    ↓ try-catch로 감싼 상태에서
    ↓
[Other Security Filters]
    ↓
    ├─ CustomAuthenticationException 발생
    │   ↓
    │   [FilterChainExceptionHandler] → 그대로 throw
    │   ↓
    │   [CustomAuthenticationEntryPoint] → 401 JSON 응답
    │
    ├─ CustomAccessDeniedException 발생
    │   ↓
    │   [FilterChainExceptionHandler] → 그대로 throw
    │   ↓
    │   [CustomAccessDeniedHandler] → 403 JSON 응답
    │
    └─ RuntimeException / NullPointerException 등 발생
        ↓
        [FilterChainExceptionHandler] → catch하여 500 JSON 응답
```

### 응답 형식

#### 인증 실패 (401)
```json
{
  "success": false,
  "message": "토큰이 만료되었습니다",
  "data": null
}
```

#### 인가 실패 (403)
```json
{
  "success": false,
  "message": "접근 권한이 없습니다",
  "data": null
}
```

#### 서버 오류 (500)
```json
{
  "success": false,
  "message": "서버 오류가 발생했습니다",
  "data": null
}
```

---

## 실무 권장사항

### 1. ErrorCode 기반 예외 생성

**❌ 나쁜 예:**
```java
throw new AuthenticationException("토큰이 만료되었습니다");  // 하드코딩
```

**✅ 좋은 예:**
```java
throw new CustomAuthenticationException(AuthErrorCode.TOKEN_EXPIRED);
```

### 2. Handler 매핑 vs 커스텀 예외

#### Handler 매핑 방식
```java
private ErrorCode determineErrorCode(AuthenticationException e) {
    if (e instanceof BadCredentialsException) return AuthErrorCode.INVALID_CREDENTIALS;
    if (e instanceof InsufficientAuthenticationException) return AuthErrorCode.AUTHENTICATION_REQUIRED;
    return AuthErrorCode.AUTHENTICATION_FAILED;
}
```

#### 커스텀 예외 방식 (현재 프로젝트 채택)
```java
throw new CustomAuthenticationException(AuthErrorCode.INVALID_CREDENTIALS);
```

**선택 기준:**
- Handler 매핑: Spring Security 기본 예외 활용, 간단한 프로젝트
- 커스텀 예외: **명시적 에러 관리, ErrorCode 중앙화** (채택)

### 3. FilterChainExceptionHandler 필수 구현

**실무 통계:**
- 대부분의 프로덕션 프로젝트에서 필수 구현
- 없으면 Filter 에러 시 HTML 500 페이지 반환
- 클라이언트 JSON 파싱 실패 발생

### 4. 로깅 전략

```java
log.error("[AuthenticationEntryPoint] {}", authException.getMessage(), authException);
```

**권장:**
- 에러 메시지 + 스택 트레이스 모두 로깅
- 보안 민감 정보 (비밀번호, 토큰 원본) 로깅 금지
- 요청 IP, 경로 등 컨텍스트 정보 포함 고려

### 5. 테스트 전략

#### 단위 테스트
```java
@Test
void 만료된_토큰_예외_발생() {
    assertThatThrownBy(() -> jwtProvider.parseToken(expiredToken))
        .isInstanceOf(CustomAuthenticationException.class)
        .hasFieldOrPropertyWithValue("errorCode", AuthErrorCode.TOKEN_EXPIRED)
        .hasMessage("토큰이 만료되었습니다");
}
```

#### 통합 테스트
```java
@Test
void 인증_실패_시_401_응답() throws Exception {
    mockMvc.perform(get("/api/protected")
            .header("Authorization", "Bearer invalid-token"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.message").value("유효하지 않은 토큰입니다"));
}
```

---

## 마이그레이션 가이드

### 기존 코드에서 전환

#### Before
```java
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {
    @Override
    public void commence(...) {
        ApiResponse<Object> apiResponse = ApiResponse.failure("인증이 필요합니다");  // 하드코딩
    }
}
```

#### After
```java
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {
    @Override
    public void commence(..., AuthenticationException authException) {
        ApiResponse<Object> apiResponse = ApiResponse.failure(authException.getMessage());  // ErrorCode에서 자동
    }
}
```

---

## 참고자료

- [Spring Security Reference](https://docs.spring.io/spring-security/reference/index.html)
- [Spring Security Exception Handling](https://docs.spring.io/spring-security/reference/servlet/architecture.html#servlet-exceptiontranslationfilter)
- 프로젝트 내부 문서: `docs/SPRING_SECURITY_SETUP.md`

---

## 변경 이력

| 날짜 | 작성자 | 내용 |
|------|--------|------|
| 2025-11-18 | - | 초기 작성 |