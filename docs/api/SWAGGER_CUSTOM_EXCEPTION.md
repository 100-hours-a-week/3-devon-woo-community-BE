# Swagger 커스텀 에러 응답 문서화

커스텀 어노테이션을 사용하여 Swagger에 에러 응답을 자동으로 추가하는 방법

## 구조

```
common/
├── exception/
│   └── ErrorCode.java              # 에러 코드 enum
└── swagger/
    ├── CustomExceptionDescription.java    # 커스텀 어노테이션
    ├── SwaggerResponseDescription.java    # API별 에러 정의
    └── SwaggerConfig.java                 # Swagger 설정
```

## Step 1: 커스텀 어노테이션 생성

**CustomExceptionDescription.java**
```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CustomExceptionDescription {
    SwaggerResponseDescription value();
}
```

## Step 2: API별 에러 코드 정의

**SwaggerResponseDescription.java**
```java
@Getter
public enum SwaggerResponseDescription {

    POST_CREATE(new LinkedHashSet<>(Set.of(
            USER_NOT_FOUND
    ))),
    POST_UPDATE(new LinkedHashSet<>(Set.of(
            POST_NOT_FOUND,
            NO_PERMISSION
    )));

    private final Set<ErrorCode> errorCodeList;

    SwaggerResponseDescription(Set<ErrorCode> errorCodeList) {
        // 공통 에러 자동 추가
        errorCodeList.addAll(new LinkedHashSet<>(Set.of(
                INVALID_REQUEST,
                INTERNAL_SERVER_ERROR
        )));
        this.errorCodeList = errorCodeList;
    }
}
```

## Step 3: OperationCustomizer 구현

**SwaggerConfig.java**
```java
@Configuration
public class SwaggerConfig {

    @Bean
    public OperationCustomizer customOperationCustomizer() {
        return (operation, handlerMethod) -> {
            CustomExceptionDescription annotation =
                handlerMethod.getMethodAnnotation(CustomExceptionDescription.class);

            if (annotation != null) {
                SwaggerResponseDescription responseDescription = annotation.value();
                ApiResponses apiResponses = operation.getResponses();

                for (ErrorCode errorCode : responseDescription.getErrorCodeList()) {
                    String statusCode = String.valueOf(errorCode.getHttpStatus().value());

                    Map<String, Object> example = new HashMap<>();
                    example.put("success", false);
                    example.put("data", null);
                    example.put("message", errorCode.getMessage());

                    Schema<?> schema = new Schema<>()
                            .type("object")
                            .addProperty("success", new Schema<>().type("boolean"))
                            .addProperty("data", new Schema<>().nullable(true))
                            .addProperty("message", new Schema<>().type("string"));

                    ApiResponse apiResponse = new ApiResponse()
                            .description(errorCode.getMessage())
                            .content(new Content()
                                    .addMediaType("application/json",
                                            new MediaType()
                                                    .schema(schema)
                                                    .example(example)));

                    apiResponses.addApiResponse(statusCode, apiResponse);
                }
            }
            return operation;
        };
    }
}
```

## Step 4: Controller에 적용

```java
@RestController
@RequestMapping("/api/v1/posts")
public class PostController {

    @Operation(summary = "게시글 생성", description = "새로운 게시글을 작성합니다.")
    @CustomExceptionDescription(SwaggerResponseDescription.POST_CREATE)
    @PostMapping
    public ApiResponse<PostResponse> createPost(@RequestBody PostCreateRequest request) {
        // ...
    }

    @Operation(summary = "게시글 수정", description = "기존 게시글을 수정합니다.")
    @CustomExceptionDescription(SwaggerResponseDescription.POST_UPDATE)
    @PatchMapping("/{postId}")
    public ApiResponse<PostResponse> updatePost(@PathVariable Long postId) {
        // ...
    }
}
```

## 사용법

1. `SwaggerResponseDescription`에 새 API의 에러 코드 정의
2. Controller 메서드에 `@CustomExceptionDescription` 추가
3. Swagger UI에서 자동으로 에러 응답 확인

## 결과

Swagger UI에서 각 API별로 정의된 에러 응답이 자동으로 표시됩니다:
- **400** Bad Request - invalid_request
- **404** Not Found - post_not_found
- **500** Internal Server Error - internal_server_error
- 등등...
