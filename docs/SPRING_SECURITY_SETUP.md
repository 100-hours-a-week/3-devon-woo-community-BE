# Spring Security 적용 가이드

> 커뮤니티 프로젝트에 Spring Security를 단계적으로 적용하는 과정

## 개요

이 문서는 Spring Security를 프로젝트에 단계적으로 적용하는 과정을 기록합니다.

---

## Step 1: Spring Security 의존성 추가

### 1.1 의존성 추가

`build.gradle` 파일의 dependencies 블록에 Spring Security 스타터를 추가합니다.

```gradle
dependencies {
    // Spring Boot 스타터
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-security' // 추가

    // ... 기타 의존성
}
```

### 1.2 변경 사항

- **파일**: `build.gradle:33`
- **추가된 의존성**: `org.springframework.boot:spring-boot-starter-security`

### 1.3 의존성 다운로드 확인

```bash
./gradlew clean build
```

---

## Step 2: SecurityConfig 기본 설정

### 2.1 패키지 구조 결정

**위치 선정 과정**

Spring Security 설정의 적절한 위치를 결정하기 위해 클린 아키텍처 원칙을 검토했습니다.

- ❌ `common/security`: Common 모듈은 순수 Java 지향, Spring Framework 강한 의존성은 부적합
- ✅ `application/security`: Spring Security는 Application 레벨 보안 정책, 프레임워크 의존성 격리

**선택한 구조**: `application/security/`
```
application/security/
├── config/              # SecurityConfig
├── filter/              # JwtAuthenticationFilter 등 (예정)
├── handler/             # 예외 처리 핸들러 (예정)
├── provider/            # JwtTokenProvider 등 (예정)
└── service/             # CustomUserDetailsService 등 (예정)
```

자세한 아키텍처 설명은 [CLEAN_ARCHITECTURE.md](CLEAN_ARCHITECTURE.md)를 참고하세요.

### 2.2 SecurityConfig 클래스 생성

REST API 서버에 맞는 기본 보안 설정을 구성합니다.

**파일 위치**: `src/main/java/com/kakaotechbootcamp/community/application/security/config/SecurityConfig.java`

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // CSRF 보호 비활성화 (REST API는 stateless하므로 불필요)
                .csrf(AbstractHttpConfigurer::disable)

                // 세션 사용하지 않음 (JWT 기반 인증 준비)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // 요청에 대한 권한 설정
                .authorizeHttpRequests(auth -> auth
                        // 모든 요청 허용 (임시 - 추후 인증/인가 정책 적용 예정)
                        .anyRequest().permitAll()
                );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

### 2.3 주요 설정 설명

#### CSRF 비활성화
```java
.csrf(AbstractHttpConfigurer::disable)
```
- REST API는 stateless하므로 CSRF 보호가 불필요
- JWT 기반 인증 사용 시 CSRF 토큰 대신 JWT 토큰으로 보안 유지

#### 세션 정책 설정
```java
.sessionManagement(session -> session
    .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
)
```
- `STATELESS`: 서버에서 세션을 생성하거나 사용하지 않음
- JWT 기반 인증을 위한 필수 설정

#### 권한 설정
```java
.authorizeHttpRequests(auth -> auth
    .anyRequest().permitAll()
)
```
- 현재는 모든 요청 허용 (개발 단계)
- 추후 인증이 필요한 엔드포인트와 공개 엔드포인트를 구분하여 설정 예정

#### PasswordEncoder 빈 등록
```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```
- BCrypt 해시 알고리즘을 사용한 비밀번호 암호화
- 회원가입/로그인 시 비밀번호 검증에 사용

### 2.4 빌드 확인

```bash
./gradlew clean compileJava
```

---

---

## Step 3: BCrypt 비밀번호 암호화 설정

### 3.1 PasswordEncoder Bean 등록

`SecurityConfig`에 BCrypt 기반 비밀번호 암호화를 위한 Bean을 등록합니다.

**파일 위치**: `application/security/config/SecurityConfig.java:45-47`

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

### 3.2 회원가입 시 비밀번호 암호화

회원가입 서비스에서 PasswordEncoder를 주입받아 비밀번호를 암호화하여 저장합니다.

**파일 위치**: `application/member/service/SignupService.java`

```java
@Service
@RequiredArgsConstructor
public class SignupService {

    private final PasswordEncoder passwordEncoder;
    private final MemberRepository memberRepository;

    public SignupResponse signup(SignupRequest request) {
        String encodedPassword = passwordEncoder.encode(request.password());

        Member member = Member.builder()
                .email(request.email())
                .password(encodedPassword)
                .build();

        memberRepository.save(member);
        return new SignupResponse(member.getId());
    }
}
```

### 3.3 BCrypt 동작 원리

- Salt 자동 생성 및 포함
- 단방향 해시 알고리즘 (복호화 불가능)
- 같은 비밀번호라도 매번 다른 해시값 생성
- 로그인 시 `passwordEncoder.matches(rawPassword, encodedPassword)`로 검증

---

## Step 4: CORS 설정

### 4.1 CORS 설정 파일 생성

**파일 위치**: `application/security/config/CorsConfig.java`

```java
@Configuration
@RequiredArgsConstructor
public class CorsConfig {

    private final CorsProperties corsProperties;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(corsProperties.getAllowedOrigins());
        configuration.setAllowedMethods(corsProperties.getAllowedMethods());
        configuration.setAllowedHeaders(corsProperties.getAllowedHeaders());
        configuration.setAllowCredentials(corsProperties.getAllowCredentials());
        configuration.setMaxAge(corsProperties.getMaxAge());
        configuration.setExposedHeaders(corsProperties.getExposedHeaders());

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}
```

### 4.2 CORS Properties 설정

**파일 위치**: `application/security/config/CorsProperties.java`

```java
@Configuration
@ConfigurationProperties(prefix = "cors")
@Getter
@Setter
public class CorsProperties {
    private List<String> allowedOrigins = List.of("http://localhost:3000");
    private List<String> allowedMethods = List.of("GET", "POST", "PUT", "DELETE", "PATCH");
    private List<String> allowedHeaders = List.of("*");
    private List<String> exposedHeaders = List.of("Authorization");
    private Boolean allowCredentials = true;
    private Long maxAge = 3600L;
}
```

### 4.3 application.yml 설정

```yaml
cors:
  allowed-origins:
    - http://localhost:3000
    - http://localhost:8080
  allowed-methods:
    - GET
    - POST
    - PUT
    - DELETE
    - PATCH
  allowed-headers:
    - "*"
  exposed-headers:
    - Authorization
  allow-credentials: true
  max-age: 3600
```

### 4.4 SecurityConfig에 CORS 적용

```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            // ...
    ;
    return http.build();
}
```

---

## Step 5: 필터 체인 예외 처리

### 5.1 FilterChainExceptionHandler 생성

필터 체인 전체에서 발생하는 예외를 처리하는 핸들러를 작성합니다.

**파일 위치**: `application/security/filter/FilterChainExceptionHandler.java`

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
            throw e;
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

### 5.2 핵심 동작

- `OncePerRequestFilter` 확장하여 요청당 한 번만 실행
- 모든 필터 체인에서 발생하는 예외를 catch
- `AuthenticationException`, `AccessDeniedException`은 재throw하여 전용 핸들러에서 처리
- 기타 예외는 500 에러로 통일된 JSON 응답 반환

### 5.3 SecurityConfig에 필터 추가

필터 체인의 **최상단**에 추가하여 모든 필터보다 먼저 실행되도록 설정합니다.

```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
            // 필터 체인 전역 예외 처리 핸들러 (모든 필터보다 먼저 실행)
            .addFilterBefore(
                    filterChainExceptionHandler, UsernamePasswordAuthenticationFilter.class
            )
            // ...
    ;
    return http.build();
}
```

---

## Step 6: 인증/인가 예외 처리 핸들러

### 6.1 AuthenticationEntryPoint 구현

인증되지 않은 사용자가 보호된 리소스에 접근할 때 처리합니다.

**파일 위치**: `application/security/handler/CustomAuthenticationEntryPoint.java`

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

        ApiResponse<Object> apiResponse = ApiResponse.failure("인증이 필요합니다");
        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }
}
```

### 6.2 AccessDeniedHandler 구현

인증은 되었으나 권한이 없는 사용자가 접근할 때 처리합니다.

**파일 위치**: `application/security/handler/CustomAccessDeniedHandler.java`

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

        ApiResponse<Object> apiResponse = ApiResponse.failure("접근 권한이 없습니다");
        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }
}
```

### 6.3 SecurityConfig에 핸들러 등록

```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
            // 인증/인가 예외 처리 핸들러 설정
            .exceptionHandling(exception -> exception
                    .authenticationEntryPoint(authenticationEntryPoint)
                    .accessDeniedHandler(accessDeniedHandler)
            )
            // ...
    ;
    return http.build();
}
```

### 6.4 응답 예시

**401 Unauthorized**
```json
{
  "status": "FAILURE",
  "data": null,
  "message": "인증이 필요합니다"
}
```

**403 Forbidden**
```json
{
  "status": "FAILURE",
  "data": null,
  "message": "접근 권한이 없습니다"
}
```

---

## Step 7: 로그인 필터 구현

### 7.1 LoginAuthenticationFilter 생성

JSON 형식의 로그인 요청을 처리하는 커스텀 필터를 작성합니다.

**파일 위치**: `application/security/filter/LoginAuthenticationFilter.java`

```java
@RequiredArgsConstructor
public class LoginAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    private final AuthenticationManager authenticationManager;
    private final LoginSuccessHandler successHandler;
    private final LoginFailureHandler failureHandler;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        try {
            String requestBody = new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            LoginRequest loginRequest = objectMapper.readValue(requestBody, LoginRequest.class);

            String username = loginRequest.email();
            String password = loginRequest.password();

            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                    username, password);

            return authenticationManager.authenticate(authenticationToken);

        } catch (AuthenticationException e) {
            throw e;
        } catch (IOException e) {
            throw new AuthenticationServiceException("로그인 요청 파싱에 실패했습니다", e);
        } catch (Exception e) {
            throw new AuthenticationServiceException("인증 처리 중 오류가 발생했습니다", e);
        }
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authentication) throws IOException, ServletException {
        successHandler.onAuthenticationSuccess(request, response, authentication);
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) throws IOException, ServletException {
        failureHandler.onAuthenticationFailure(request, response, failed);
    }
}
```

### 7.2 핵심 동작

1. `UsernamePasswordAuthenticationFilter`를 확장
2. JSON 요청 body를 파싱하여 `LoginRequest` DTO로 변환
3. email과 password를 추출하여 `UsernamePasswordAuthenticationToken` 생성
4. `AuthenticationManager`를 통해 인증 처리
5. 성공/실패 시 각각의 핸들러 호출

### 7.3 LoginRequest DTO

```java
public record LoginRequest(
        @NotBlank String email,
        @NotBlank String password
) {
}
```

---

## Step 8: 로그인 성공/실패 핸들러

### 8.1 LoginSuccessHandler 구현

**파일 위치**: `application/security/handler/LoginSuccessHandler.java`

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        log.info("로그인 성공: {}", authentication.getName());

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long userId = userDetails.getMemberId();

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        LoginResponse loginResponse = new LoginResponse(userId);
        ApiResponse<LoginResponse> apiResponse = ApiResponse.success(loginResponse, "로그인이 성공했습니다");
        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }
}
```

### 8.2 LoginFailureHandler 구현

**파일 위치**: `application/security/handler/LoginFailureHandler.java`

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class LoginFailureHandler implements AuthenticationFailureHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {

        log.info("로그인 실패: {}", exception.getMessage());

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        ApiResponse<Void> apiResponse = ApiResponse.failure("로그인이 실패했습니다");
        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }
}
```

### 8.3 응답 예시

**로그인 성공 (200 OK)**
```json
{
  "status": "SUCCESS",
  "data": {
    "userId": 1
  },
  "message": "로그인이 성공했습니다"
}
```

**로그인 실패 (401 Unauthorized)**
```json
{
  "status": "FAILURE",
  "data": null,
  "message": "로그인이 실패했습니다"
}
```

---

## Step 9: SecurityConfig 통합 설정

### 9.1 SecurityConfig 최종 구성

모든 보안 설정을 통합하여 완성합니다.

**파일 위치**: `application/security/config/SecurityConfig.java`

```java
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final AuthenticationConfiguration authenticationConfiguration;
    private final LoginSuccessHandler loginSuccessHandler;
    private final LoginFailureHandler loginFailureHandler;
    private final CorsConfigurationSource corsConfigurationSource;
    private final CustomAuthenticationEntryPoint authenticationEntryPoint;
    private final CustomAccessDeniedHandler accessDeniedHandler;
    private final FilterChainExceptionHandler filterChainExceptionHandler;

    @Bean
    public AuthenticationManager authenticationManager() throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))

                .csrf(AbstractHttpConfigurer::disable)

                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                )

                .requestCache(cache -> cache
                        .requestCache(new HttpSessionRequestCache())
                )

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(SecurityConstants.PUBLIC_URLS).permitAll()
                        .requestMatchers(SecurityConstants.SECURE_URLS).hasRole("USER")
                        .requestMatchers(SecurityConstants.ADMIN_URLS).hasRole("ADMIN")
                        .anyRequest().authenticated()
                )

                .addFilterAt(
                        createLoginAuthenticationFilter(authenticationManager()),
                        UsernamePasswordAuthenticationFilter.class
                )

                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )

                .addFilterBefore(
                        filterChainExceptionHandler, UsernamePasswordAuthenticationFilter.class
                )
        ;

        return http.build();
    }

    private LoginAuthenticationFilter createLoginAuthenticationFilter(AuthenticationManager authenticationManager) {
        LoginAuthenticationFilter filter = new LoginAuthenticationFilter(
                authenticationManager, loginSuccessHandler, loginFailureHandler);
        filter.setFilterProcessesUrl(SecurityConstants.LOGIN_URL);
        return filter;
    }
}
```

### 9.2 세션 기반 인증 설정

```java
.sessionManagement(session -> session
        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
)

.requestCache(cache -> cache
        .requestCache(new HttpSessionRequestCache())
)
```

- `IF_REQUIRED`: 필요시 세션 생성 (기본 Spring Security 동작)
- `HttpSessionRequestCache`: 인증 전 요청 URL을 세션에 저장하여 로그인 후 redirect

### 9.3 URL 권한 설정

**SecurityConstants 정의**

**파일 위치**: `application/security/constants/SecurityConstants.java`

```java
public class SecurityConstants {

    public static final String LOGIN_URL = "/auth/login";

    public static final String[] PUBLIC_URLS = {
            "/auth/login",
            "/auth/signup",
            "/error",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/actuator/**"
    };

    public static final String[] SECURE_URLS = {
            "/api/*/secure/**",
            "/api/*/user/**"
    };

    public static final String[] ADMIN_URLS = {
            "/api/*/admin/**",
            "/admin/**"
    };
}
```

**권한 규칙**
- `PUBLIC_URLS`: 인증 불필요 (회원가입, 로그인, Swagger 등)
- `SECURE_URLS`: USER 역할 필요
- `ADMIN_URLS`: ADMIN 역할 필요
- 나머지 모든 요청: 인증 필요

### 9.4 필터 체인 구성도

```
FilterChainExceptionHandler (최상단)
    ↓
LoginAuthenticationFilter (/auth/login 요청만 처리)
    ↓
UsernamePasswordAuthenticationFilter (기본 필터)
    ↓
... 기타 Spring Security 필터들 ...
    ↓
DispatcherServlet (컨트롤러 호출)
```

---

## Step 10: 권한 관리 (requestMatchers)

### 10.1 requestMatchers 패턴

Spring Security에서 URL 패턴 매칭을 위해 `requestMatchers()`를 사용합니다.

```java
.authorizeHttpRequests(auth -> auth
        .requestMatchers("/public/**").permitAll()
        .requestMatchers("/admin/**").hasRole("ADMIN")
        .requestMatchers("/user/**").hasAnyRole("USER", "ADMIN")
        .anyRequest().authenticated()
)
```

### 10.2 주요 메서드

| 메서드 | 설명 | 예시 |
|--------|------|------|
| `permitAll()` | 모든 사용자 허용 | 회원가입, 로그인 |
| `authenticated()` | 인증된 사용자만 허용 | 마이페이지 |
| `hasRole()` | 특정 역할 필요 | ADMIN 페이지 |
| `hasAnyRole()` | 여러 역할 중 하나 | USER 또는 ADMIN |
| `hasAuthority()` | 특정 권한 필요 | WRITE 권한 |
| `denyAll()` | 모든 접근 차단 | 비활성화된 엔드포인트 |

### 10.3 와일드카드 패턴

```java
.requestMatchers("/api/*/user/**").hasRole("USER")
```

- `*`: 한 개의 경로 세그먼트 매칭
- `**`: 여러 경로 세그먼트 매칭

**예시**
- `/api/v1/user/profile` → 매칭 O
- `/api/v2/user/settings/password` → 매칭 O
- `/api/user/profile` → 매칭 X (`*`가 없음)

### 10.4 권한 검증 순서

**중요**: `requestMatchers()`는 **순서대로 평가**되므로 **구체적인 규칙을 먼저** 작성해야 합니다.

```java
.authorizeHttpRequests(auth -> auth
        .requestMatchers("/admin/public").permitAll()
        .requestMatchers("/admin/**").hasRole("ADMIN")
        .anyRequest().authenticated()
)
```

위 순서를 바꾸면 `/admin/public`도 ADMIN 역할이 필요하게 됩니다.

---

## 정리 및 컨벤션

### 프로젝트 패키지 구조

```
application/security/
├── config/
│   ├── SecurityConfig.java           # 메인 보안 설정
│   ├── CorsConfig.java                # CORS 설정
│   └── CorsProperties.java            # CORS 프로퍼티
├── filter/
│   ├── LoginAuthenticationFilter.java # 로그인 필터
│   └── FilterChainExceptionHandler.java # 전역 예외 처리
├── handler/
│   ├── LoginSuccessHandler.java       # 로그인 성공 핸들러
│   ├── LoginFailureHandler.java       # 로그인 실패 핸들러
│   ├── CustomAuthenticationEntryPoint.java # 401 핸들러
│   └── CustomAccessDeniedHandler.java # 403 핸들러
├── constants/
│   └── SecurityConstants.java         # 보안 상수
└── dto/
    ├── LoginRequest.java              # 로그인 요청 DTO
    └── LoginResponse.java             # 로그인 응답 DTO
```

### 핵심 설정 요약

| 설정 항목 | 설정 값 | 이유 |
|-----------|---------|------|
| CSRF | 비활성화 | REST API는 CSRF 취약점 없음 |
| 세션 정책 | IF_REQUIRED | 세션 기반 인증 사용 |
| CORS | 활성화 | 프론트엔드 요청 허용 |
| PasswordEncoder | BCrypt | 안전한 단방향 암호화 |
| 예외 처리 | JSON 응답 | REST API 통일된 응답 포맷 |

### 보안 체크리스트

- [x] BCrypt로 비밀번호 암호화
- [x] CORS 설정으로 허용된 Origin만 접근 가능
- [x] 권한별 URL 접근 제어 (PUBLIC, USER, ADMIN)
- [x] 통일된 JSON 응답 포맷 (ApiResponse)
- [x] 전역 예외 처리 (FilterChainExceptionHandler)
- [x] 인증/인가 실패 시 적절한 HTTP 상태 코드 반환
- [x] 로그인 성공 시 세션에 인증 정보 저장

---

## 참고 사항

### Spring Security 기본 동작

Spring Security 의존성을 추가하면 다음과 같은 기본 동작이 자동으로 활성화됩니다:

1. **모든 엔드포인트 보호**: 기본적으로 모든 HTTP 요청에 인증이 필요합니다.
2. **기본 로그인 페이지**: `/login` 경로에 자동 생성된 로그인 폼이 제공됩니다.
3. **기본 사용자**: 콘솔에 출력되는 임의의 비밀번호를 가진 `user` 계정이 생성됩니다.
4. **CSRF 보호**: Cross-Site Request Forgery 공격 방어가 기본으로 활성화됩니다.

### 세션 기반 vs JWT 기반 인증

**세션 기반 인증 (현재 구현)**
- 서버에 세션 저장 (메모리/DB/Redis)
- 클라이언트는 쿠키에 JSESSIONID 보관
- 서버가 세션 상태 관리 (Stateful)
- 단일 서버 또는 세션 공유 필요

**JWT 기반 인증 (미구현)**
- 토큰에 모든 정보 포함 (Stateless)
- 서버는 토큰 검증만 수행
- 확장성이 좋음 (서버 간 상태 공유 불필요)
- 토큰 만료 전까지 강제 로그아웃 어려움
