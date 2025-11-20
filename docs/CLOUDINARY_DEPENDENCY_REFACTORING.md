# Cloudinary 외부 의존성 분리를 통한 테스트 환경 개선

## 배경

### 문제 상황
애플리케이션 테스트 실행 시 모든 테스트(76개)가 실패하는 상황이 발생했다. 초기에는 DB 설정 동기화 문제로 추정했으나, 실제 원인은 **Cloudinary 외부 라이브러리 의존성**이었다.

### 근본 원인 분석

```
PlaceholderResolutionException: Could not resolve placeholder 'CLOUDINARY_API_KEY'
in value "${CLOUDINARY_API_KEY}" <-- "${storage.cloudinary.api.key}"
```

테스트 환경에서 Spring Boot 컨텍스트 초기화 시 다음과 같은 문제가 발생했다:

1. **CloudinaryConfig**가 무조건 로드되면서 환경변수를 요구
2. **ImageController**가 Cloudinary 속성을 직접 참조 (`@Value` 어노테이션)
3. 테스트 환경에는 Cloudinary 환경변수가 설정되어 있지 않음
4. Spring Context 초기화 실패 → 모든 테스트 실행 불가

### 아키텍처 문제점

```
Application Layer (ImageController)
         ↓ (직접 의존)
External Library (Cloudinary) + Config
```

- **높은 결합도**: 애플리케이션 코드가 외부 라이브러리에 강하게 결합
- **테스트 어려움**: 외부 서비스 없이 테스트 불가능
- **확장성 부족**: 다른 스토리지 서비스로 교체 어려움

## 해결 과정

### 1단계: 임시 해결 시도 (실패)

#### 시도 1: autoconfigure.exclude 사용
```yaml
spring:
  autoconfigure:
    exclude:
      - com.kakaotechbootcamp.community.config.CloudinaryConfig
```

**결과**: `@Configuration` 클래스는 자동 설정이 아니므로 제외 불가
```
IllegalStateException: The following classes could not be excluded
because they are not auto-configuration classes
```

#### 시도 2: Mockito Mock 객체 사용
```java
@Bean
@Primary
public Cloudinary testCloudinary() {
    return mock(Cloudinary.class);
}
```

**결과**: ImageController의 `@Value` 어노테이션이 여전히 속성을 요구하므로 실패

#### 시도 3: 더미 속성값 추가
```yaml
cloudinary:
  cloud_name: test-cloud
  api_key: test-key
  api_secret: test-secret
```

**결과**: 속성명 불일치 (`cloudinary.*` vs `storage.cloudinary.*`)로 실패

### 2단계: 근본적 해결 - 의존성 역전 원칙 적용

사용자의 제안에 따라 **인터페이스 기반 추상화**를 도입하여 외부 의존성을 분리했다.

#### 2-1. 인프라 계층 인터페이스 정의

**ImageStorageService 인터페이스**
```java
// src/main/java/com/kakaotechbootcamp/community/infrastructure/image/
public interface ImageStorageService {
    ImageSignature generateUploadSignature(String type);
}
```

**ImageSignature 레코드**
```java
public record ImageSignature(
    String apiKey,
    String cloudName,
    Long timestamp,
    String signature,
    String uploadPreset,
    String folder
) {}
```

**핵심 포인트**:
- 이미지 스토리지의 책임을 추상화
- 구현 기술과 무관한 인터페이스 정의
- 불변 데이터 전달을 위한 레코드 사용

#### 2-2. Cloudinary 구현체 생성

```java
// src/main/java/.../infrastructure/image/cloudinary/
@Service
public class CloudinaryImageStorageService implements ImageStorageService {

    @Value("${storage.cloudinary.api.key}")
    private String apiKey;

    @Value("${storage.cloudinary.api.secret}")
    private String apiSecret;

    @Value("${storage.cloudinary.cloud.name}")
    private String cloudName;

    @Value("${storage.cloudinary.upload.preset:unsigned_preset}")
    private String uploadPreset;

    @Override
    public ImageSignature generateUploadSignature(String type) {
        long timestamp = System.currentTimeMillis() / 1000L;
        String folder = "profile".equals(type) ? "profiles" : "posts";

        Map<String, String> paramsToSign = new TreeMap<>();
        paramsToSign.put("folder", folder);
        paramsToSign.put("timestamp", String.valueOf(timestamp));
        paramsToSign.put("upload_preset", uploadPreset);

        StringBuilder toSign = new StringBuilder();
        paramsToSign.forEach((key, value) -> {
            if (toSign.length() > 0) {
                toSign.append("&");
            }
            toSign.append(key).append("=").append(value);
        });
        toSign.append(apiSecret);

        String signature = DigestUtils.sha1Hex(toSign.toString());

        return new ImageSignature(
            apiKey,
            cloudName,
            timestamp,
            signature,
            uploadPreset,
            folder
        );
    }
}
```

**핵심 포인트**:
- Cloudinary 의존성을 인프라 계층으로 격리
- 외부 라이브러리 관련 설정과 로직을 한 곳에 집중
- `@Service`로 Spring Bean 등록

#### 2-3. 애플리케이션 계층 리팩토링

**변경 전**:
```java
@RestController
public class ImageController {
    @Value("${storage.cloudinary.api.key}")
    private String apiKey;

    @Value("${storage.cloudinary.api.secret}")
    private String apiSecret;

    @GetMapping("/sign")
    public ApiResponse<Map<String, Object>> sign(...) {
        long timestamp = System.currentTimeMillis() / 1000L;
        String folder = "profile".equals(type) ? "profiles" : "posts";

        Map<String, String> paramsToSign = new TreeMap<>();
        paramsToSign.put("folder", folder);
        paramsToSign.put("timestamp", String.valueOf(timestamp));
        paramsToSign.put("upload_preset", uploadPreset);

        StringBuilder toSign = new StringBuilder();
        paramsToSign.forEach((key, value) -> {
            if (toSign.length() > 0) {
                toSign.append("&");
            }
            toSign.append(key).append("=").append(value);
        });
        toSign.append(apiSecret);

        String signature = DigestUtils.sha1Hex(toSign.toString());

        Map<String, Object> body = new HashMap<>();
        body.put("apiKey", apiKey);
        body.put("cloudName", cloudName);
        body.put("timestamp", timestamp);
        body.put("signature", signature);
        body.put("uploadPreset", uploadPreset);
        body.put("folder", folder);

        return ApiResponse.success(body);
    }
}
```

**변경 후**:
```java
@RestController
@RequiredArgsConstructor
public class ImageController {
    private final ImageStorageService imageStorageService;

    @GetMapping("/sign")
    public ApiResponse<Map<String, Object>> sign(
        @RequestParam(value = "type", defaultValue = "post") String type
    ) {
        ImageSignature signature = imageStorageService.generateUploadSignature(type);

        Map<String, Object> body = new HashMap<>();
        body.put("apiKey", signature.apiKey());
        body.put("cloudName", signature.cloudName());
        body.put("timestamp", signature.timestamp());
        body.put("signature", signature.signature());
        body.put("uploadPreset", signature.uploadPreset());
        body.put("folder", signature.folder());

        return ApiResponse.success(body);
    }
}
```

**핵심 포인트**:
- Cloudinary 직접 의존성 완전 제거
- 인터페이스만 의존 (Dependency Inversion Principle)
- 코드 간소화 및 책임 분리
- 컨트롤러는 이미지 스토리지의 구체적인 구현을 알 필요 없음

#### 2-4. 테스트 환경 설정

**TestConfig에 Mock 구현체 추가**:
```java
@TestConfiguration
public class TestConfig {

    @Bean
    public DateTimeProvider testDateTimeProvider() {
        return () -> Optional.of(Instant.now());
    }

    @Bean
    @Primary
    public ImageStorageService testImageStorageService() {
        return type -> new ImageSignature(
            "test-api-key",
            "test-cloud-name",
            System.currentTimeMillis() / 1000L,
            "test-signature",
            "test-upload-preset",
            "profile".equals(type) ? "profiles" : "posts"
        );
    }
}
```

**CloudinaryConfig 조건부 로딩**:
```java
@Configuration
@ConditionalOnProperty(prefix = "cloudinary", name = "cloud_name")
public class CloudinaryConfig {

    @Value("${cloudinary.cloud_name}")
    private String cloudName;

    @Value("${cloudinary.api_key}")
    private String apiKey;

    @Value("${cloudinary.api_secret}")
    private String apiSecret;

    @Bean
    public Cloudinary cloudinary() {
        return new Cloudinary(ObjectUtils.asMap(
            "cloud_name", cloudName,
            "api_key", apiKey,
            "api_secret", apiSecret
        ));
    }
}
```

**application-test.yml 설정**:
```yaml
spring:
  main:
    allow-bean-definition-overriding: true

  datasource:
    url: jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
    driver-class-name: org.h2.Driver
    username: sa
    password:

  jpa:
    database-platform: org.hibernate.dialect.H2Dialect
    hibernate:
      ddl-auto: create-drop

  h2:
    console:
      enabled: true
      path: /h2-console

storage:
  cloudinary:
    api:
      key: test-api-key
      secret: test-api-secret
    cloud:
      name: test-cloud-name
    upload:
      preset: test-upload-preset
```

**CommunityApplicationTests 수정**:
```java
@SpringBootTest
@ActiveProfiles("test")
class CommunityApplicationTests {

    @Test
    void contextLoads() {
    }
}
```

**핵심 포인트**:
- `@Primary`로 테스트 환경에서 Mock 구현체 우선 사용
- `@ConditionalOnProperty`로 실제 Cloudinary Config는 프로덕션에서만 활성화
- 람다 표현식으로 간단한 Mock 구현
- `@ActiveProfiles("test")`로 테스트 프로파일 활성화

## 결과

### 테스트 성공률
- **변경 전**: 0/76 통과 (100% 실패)
- **변경 후**: 71/76 통과 (93.4% 성공)

나머지 5개 실패는 Cloudinary와 무관한 비즈니스 로직 테스트 (회원 탈퇴, 게시글 삭제)로, 별도 해결 필요.

### 아키텍처 개선

**변경 전**:
```
┌─────────────────────┐
│ ImageController     │
│ (Application)       │
└──────────┬──────────┘
           │ @Value 직접 참조
           ↓
┌─────────────────────┐
│ Cloudinary Library  │
│ (External)          │
└─────────────────────┘
```

**변경 후**:
```
┌─────────────────────┐
│ ImageController     │
│ (Application)       │
└──────────┬──────────┘
           │ 인터페이스 의존
           ↓
┌─────────────────────────────────┐
│ ImageStorageService (Interface) │
│ (Infrastructure)                │
└──────────┬──────────────────────┘
           │
    ┌──────┴───────┐
    ↓              ↓
┌────────────┐  ┌──────────────┐
│ Cloudinary │  │ Test Mock    │
│ Impl       │  │ Impl         │
└────────────┘  └──────────────┘
```

### 파일 구조 변경

```
src/
├── main/
│   └── java/
│       └── com/kakaotechbootcamp/community/
│           ├── application/
│           │   └── common/
│           │       └── controller/
│           │           └── ImageController.java (리팩토링)
│           ├── config/
│           │   └── CloudinaryConfig.java (조건부 로딩 추가)
│           └── infrastructure/
│               └── image/
│                   ├── ImageStorageService.java (신규)
│                   ├── ImageSignature.java (신규)
│                   └── cloudinary/
│                       └── CloudinaryImageStorageService.java (신규)
└── test/
    ├── java/
    │   └── com/kakaotechbootcamp/community/
    │       ├── CommunityApplicationTests.java (프로파일 추가)
    │       └── config/
    │           └── TestConfig.java (Mock 구현체 추가)
    └── resources/
        └── application-test.yml (Cloudinary 설정 추가)
```

## 얻은 교훈

### 1. 외부 의존성은 추상화해야 한다
- 외부 라이브러리를 직접 사용하면 결합도가 높아진다
- 인터페이스를 통한 추상화로 변경에 유연한 구조 확보
- 구현 기술의 변경이 애플리케이션 코드에 영향을 주지 않음

### 2. 테스트 가능성을 고려한 설계
- 외부 서비스 없이도 테스트 가능한 구조 필요
- Mock 객체로 쉽게 대체 가능한 설계가 중요
- 테스트 환경에서 실제 외부 API 호출 방지

### 3. SOLID 원칙의 실용성
- **DIP (의존성 역전 원칙)**: 구체적인 구현이 아닌 추상화에 의존
- **OCP (개방-폐쇄 원칙)**: S3, MinIO 등 새 구현체 추가 시 기존 코드 수정 불필요
- **SRP (단일 책임 원칙)**: 컨트롤러는 HTTP 처리, 서비스는 스토리지 로직 담당

### 4. 계층 분리의 중요성
- **Application 계층**: 비즈니스 로직에 집중, HTTP 요청/응답 처리
- **Infrastructure 계층**: 외부 기술 세부사항 격리, 구체적인 구현 제공
- 각 계층의 책임이 명확해지고 유지보수성 향상

### 5. 조건부 설정의 활용
- `@ConditionalOnProperty`를 사용하여 환경별 Bean 로딩 제어
- 프로덕션과 테스트 환경의 설정 분리
- 불필요한 Bean 생성 방지로 컨텍스트 초기화 속도 개선

## 향후 개선 방향

### 1. 다른 스토리지 지원
현재 구조로 쉽게 추가 가능:
```java
@Service
@ConditionalOnProperty(prefix = "storage", name = "type", havingValue = "s3")
public class S3ImageStorageService implements ImageStorageService {

    @Value("${storage.s3.bucket}")
    private String bucket;

    @Value("${storage.s3.region}")
    private String region;

    @Override
    public ImageSignature generateUploadSignature(String type) {
        // AWS S3 Pre-signed URL 생성 로직
    }
}
```

### 2. 전략 패턴 적용
여러 스토리지를 동적으로 선택해야 하는 경우:
```java
@Service
public class ImageStorageFactory {

    private final Map<StorageType, ImageStorageService> services;

    public ImageStorageFactory(List<ImageStorageService> serviceList) {
        this.services = serviceList.stream()
            .collect(Collectors.toMap(
                this::getStorageType,
                Function.identity()
            ));
    }

    public ImageStorageService getService(StorageType type) {
        return services.get(type);
    }
}
```

### 3. 통합 테스트 추가
ImageController와 ImageStorageService 간 통합 테스트 작성:
```java
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ImageControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void signRequest_ShouldReturnValidSignature() throws Exception {
        mockMvc.perform(get("/api/v1/images/sign")
                .param("type", "profile"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.apiKey").value("test-api-key"))
            .andExpect(jsonPath("$.data.folder").value("profiles"));
    }
}
```

### 4. 이미지 업로드/삭제 기능 추가
현재는 서명 생성만 지원하므로, 인터페이스 확장 고려:
```java
public interface ImageStorageService {
    ImageSignature generateUploadSignature(String type);
    String uploadImage(MultipartFile file, String folder);
    void deleteImage(String imageUrl);
}
```

## 결론

단순히 "테스트를 돌아가게 만드는" 것을 넘어서, **클린 아키텍처 원칙을 적용하여 근본적인 설계 문제를 해결**했다. 이를 통해:

1. ✅ 외부 의존성으로 인한 테스트 실패 해결
2. ✅ 유지보수성 및 확장성 향상
3. ✅ 의존성 역전 원칙 준수
4. ✅ 테스트 가능한 구조 확립
5. ✅ 계층 간 책임 명확화

**"문제를 해결하는 것"과 "올바른 방식으로 해결하는 것"의 차이**를 보여주는 사례였다.

이번 리팩토링은 단기적으로는 코드량이 증가했지만, 장기적으로는:
- 새로운 스토리지 서비스 추가가 용이함
- 테스트 작성 및 유지보수가 간편함
- 외부 라이브러리 변경 시 영향 범위가 제한됨
- 코드의 의도가 명확하고 이해하기 쉬움

결과적으로 **기술 부채를 줄이고 지속 가능한 코드베이스**를 구축할 수 있었다.
