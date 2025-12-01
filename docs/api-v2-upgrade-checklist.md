# API v2 업그레이드 체크리스트

## 개요
- 작업 기간: TBD
- 담당자: TBD
- 목표: API 스팩 v2에 맞춰 Controller와 DTO 업그레이드
- 제약사항: 엔티티/서비스 로직 변경 금지, 엔티티에 없는 필드는 TODO 주석 + 더미 데이터

---

## Phase 1: Auth API (우선순위 1)

### 1.1 DTO 생성
- [ ] `CheckAvailabilityResponse.java` 생성
  - 위치: `src/main/java/com/devon/techblog/application/security/dto/response/`
  - 필드: `boolean available`
  - Record 타입으로 생성

### 1.2 AuthController 수정
- [ ] POST /auth/login 엔드포인트 추가
  - Request: LoginRequest
  - Response: `ApiResponse<LoginResponse>`
  - 설명: Swagger 문서화용 명시적 엔드포인트

- [ ] POST /auth/logout 엔드포인트 추가
  - @CurrentUser 사용
  - Response: `ApiResponse<Void>`
  - 리프레시 토큰 쿠키 무효화 로직

- [ ] GET /auth/check-email 엔드포인트 추가
  - Query 파라미터: email (String)
  - Response: `ApiResponse<CheckAvailabilityResponse>`
  - 이메일 중복 체크 로직

- [ ] GET /auth/check-nickname 엔드포인트 추가
  - Query 파라미터: nickname (String)
  - Response: `ApiResponse<CheckAvailabilityResponse>`
  - 닉네임 중복 체크 로직

### 1.3 SignupController 수정
- [ ] 경로 수정: `auth/signup` → `/auth/signup`
- [ ] Response 타입 변경: SignupResponse → LoginResponse
- [ ] accessToken 반환 로직 추가

---

## Phase 2: Member API (우선순위 2)

### 2.1 DTO 생성/수정

#### 신규 DTO
- [ ] `SocialLinks.java` 생성
  - 위치: `src/main/java/com/devon/techblog/application/member/dto/`
  - 필드: github, website, linkedin, notion (모두 String)
  - Record 타입

#### MemberDetailsResponse.java 수정
- [ ] role 필드 추가 (엔티티에 있음)
  - 타입: String
  - MemberRole enum을 String으로 변환

- [ ] handle 필드 추가 (TODO)
  - 타입: String
  - 더미값: null
  - 주석: `// TODO: Entity에 필드 추가 필요`

- [ ] bio 필드 추가 (TODO)
  - 타입: String
  - 더미값: null
  - 주석: `// TODO: Entity에 필드 추가 필요`

- [ ] company 필드 추가 (TODO)
  - 타입: String
  - 더미값: null
  - 주석: `// TODO: Entity에 필드 추가 필요`

- [ ] location 필드 추가 (TODO)
  - 타입: String
  - 더미값: null
  - 주석: `// TODO: Entity에 필드 추가 필요`

- [ ] primaryStack 필드 추가 (TODO)
  - 타입: List<String>
  - 더미값: Collections.emptyList()
  - 주석: `// TODO: Entity에 필드 추가 필요`

- [ ] interests 필드 추가 (TODO)
  - 타입: List<String>
  - 더미값: Collections.emptyList()
  - 주석: `// TODO: Entity에 필드 추가 필요`

- [ ] socialLinks 필드 추가 (TODO)
  - 타입: SocialLinks
  - 더미값: new SocialLinks(null, null, null, null)
  - 주석: `// TODO: Entity에 필드 추가 필요`

#### MemberUpdateRequest.java 수정
- [ ] handle 필드 추가 (TODO)
  - 타입: String (optional)

- [ ] bio 필드 추가 (TODO)
  - 타입: String (optional)

- [ ] company 필드 추가 (TODO)
  - 타입: String (optional)

- [ ] location 필드 추가 (TODO)
  - 타입: String (optional)

- [ ] primaryStack 필드 추가 (TODO)
  - 타입: List<String> (optional)

- [ ] interests 필드 추가 (TODO)
  - 타입: List<String> (optional)

- [ ] socialLinks 필드 추가 (TODO)
  - 타입: SocialLinks (optional)

### 2.2 MemberController 수정

- [ ] GET /api/v1/members/me 엔드포인트 추가
  - @CurrentUser 사용
  - Response: `ApiResponse<MemberDetailsResponse>`
  - 현재 로그인 사용자 프로필 조회

- [ ] GET /api/v1/members/{id} → GET /api/v1/members/{memberId}
  - 경로 변수명 변경: {id} → {memberId}
  - Response: `ApiResponse<MemberDetailsResponse>`

- [ ] PATCH → PUT 변경: PATCH /api/v1/members/{id} → PUT /api/v1/members/me
  - HTTP 메서드: @PatchMapping → @PutMapping
  - 경로 변경: /{id} → /me
  - @CurrentUser 사용, PathVariable 제거
  - Request: MemberUpdateRequest
  - Response: `ApiResponse<MemberDetailsResponse>`

- [ ] PATCH → PUT 변경: PATCH /api/v1/members/{id}/password → PUT /api/v1/members/me/password
  - HTTP 메서드: @PatchMapping → @PutMapping
  - 경로 변경: /{id}/password → /me/password
  - @CurrentUser 사용, PathVariable 제거

- [ ] DELETE /api/v1/members/{id} → DELETE /api/v1/members/me
  - 경로 변경: /{id} → /me
  - @CurrentUser 사용, PathVariable 제거

---

## Phase 3: Post API (우선순위 3)

### 3.1 DTO 생성/수정

#### 신규 DTO
- [ ] `PostUpdateRequest.java` 생성
  - 위치: `src/main/java/com/devon/techblog/application/post/dto/request/`
  - 모든 필드 optional (Jakarta Validation @NotBlank 제거)
  - 필드:
    - title (String, optional)
    - content (String, optional)
    - image (String, optional)
    - summary (String, optional, TODO)
    - tags (List<String>, optional, TODO)
    - seriesId (Long, optional, TODO)
    - visibility (String, optional, TODO)
    - commentsAllowed (Boolean, optional, TODO)

#### PostCreateRequest.java 수정
- [ ] summary 필드 추가 (TODO)
  - 타입: String (optional)
  - 주석: `// TODO: Entity에 필드 추가 필요`

- [ ] tags 필드 추가 (TODO)
  - 타입: List<String> (optional)
  - 주석: `// TODO: Entity에 필드 추가 필요`

- [ ] seriesId 필드 추가 (TODO)
  - 타입: Long (optional)
  - 주석: `// TODO: Entity에 필드 추가 필요`

- [ ] visibility 필드 추가 (TODO)
  - 타입: String (optional)
  - 주석: `// TODO: Entity에 필드 추가 필요`

- [ ] isDraft 필드 추가 (TODO)
  - 타입: Boolean (optional)
  - 주석: `// TODO: Entity에 필드 추가 필요`

- [ ] commentsAllowed 필드 추가 (TODO)
  - 타입: Boolean (optional)
  - 주석: `// TODO: Entity에 필드 추가 필요`

#### PostResponse.java 수정
- [ ] commentCount 필드 추가 (엔티티에 있음)
  - 타입: Long
  - Post entity에서 가져옴

- [ ] summary 필드 추가 (TODO)
  - 타입: String
  - 더미값: null
  - 주석: `// TODO: Entity에 필드 추가 필요`

- [ ] tags 필드 추가 (TODO)
  - 타입: List<String>
  - 더미값: Collections.emptyList()
  - 주석: `// TODO: Entity에 필드 추가 필요`

- [ ] seriesId 필드 추가 (TODO)
  - 타입: Long
  - 더미값: null
  - 주석: `// TODO: Entity에 필드 추가 필요`

- [ ] seriesName 필드 추가 (TODO)
  - 타입: String
  - 더미값: null
  - 주석: `// TODO: Entity에 필드 추가 필요`

- [ ] visibility 필드 추가 (TODO)
  - 타입: String
  - 더미값: "public"
  - 주석: `// TODO: Entity에 필드 추가 필요`

#### PostSummaryResponse.java 수정
- [ ] summary 필드 추가 (TODO)
  - 타입: String
  - 더미값: null
  - 주석: `// TODO: Entity에 필드 추가 필요`

- [ ] thumbnail 필드 추가 (TODO)
  - 타입: String
  - 더미값: null
  - 주석: `// TODO: Entity에 필드 추가 필요`

### 3.2 PostController 수정
- [ ] PATCH → PUT 변경: PATCH /api/v1/posts/{postId} → PUT /api/v1/posts/{postId}
  - HTTP 메서드: @PatchMapping → @PutMapping
  - Request: PostUpdateRequest (새로 생성한 DTO)

---

## Phase 4: Comment API (우선순위 4)

### 4.1 CommentController 수정
- [ ] PATCH → PUT 변경: PATCH /api/v1/comments/{commentId} → PUT /api/v1/comments/{commentId}
  - HTTP 메서드: @PatchMapping → @PutMapping
  - DTO 변경 불필요 (이미 스팩과 일치)

---

## 검증 체크리스트

### 컴파일 및 빌드
- [ ] Gradle 빌드 성공
- [ ] 컴파일 에러 없음
- [ ] Spotless 포맷 적용

### API 문서
- [ ] Swagger UI에서 모든 엔드포인트 확인
- [ ] 각 DTO 스키마 정상 표시
- [ ] TODO 필드에 대한 설명 확인

### 기능 테스트
- [ ] Auth API 테스트
  - [ ] POST /auth/login
  - [ ] POST /auth/logout
  - [ ] GET /auth/check-email
  - [ ] GET /auth/check-nickname
  - [ ] POST /auth/signup

- [ ] Member API 테스트
  - [ ] GET /api/v1/members/me
  - [ ] GET /api/v1/members/{memberId}
  - [ ] PUT /api/v1/members/me
  - [ ] PUT /api/v1/members/me/password
  - [ ] DELETE /api/v1/members/me

- [ ] Post API 테스트
  - [ ] GET /api/v1/posts
  - [ ] GET /api/v1/posts/{postId}
  - [ ] POST /api/v1/posts
  - [ ] PUT /api/v1/posts/{postId}
  - [ ] DELETE /api/v1/posts/{postId}

- [ ] Comment API 테스트
  - [ ] GET /api/v1/posts/{postId}/comments
  - [ ] POST /api/v1/posts/{postId}/comments
  - [ ] PUT /api/v1/comments/{commentId}
  - [ ] DELETE /api/v1/comments/{commentId}

### 응답 형식 검증
- [ ] 모든 API가 ApiResponse 래퍼 사용
- [ ] TODO 필드가 더미 데이터 반환
- [ ] HTTP 상태 코드 정확함 (200, 201, 204 등)

---

## 완료 기준
1. ✅ 모든 컨트롤러 엔드포인트가 API v2 스팩과 일치
2. ✅ 모든 DTO가 스팩에 정의된 필드를 포함
3. ✅ 엔티티에 없는 필드는 TODO 주석 + 더미 데이터로 처리
4. ✅ 엔티티/서비스 로직 변경 없음
5. ✅ Gradle 빌드 성공
6. ✅ Swagger UI 정상 작동

---

## 참고 사항

### 더미 데이터 규칙
- String: `null`
- List<String>: `Collections.emptyList()`
- Boolean: `false` 또는 기본값
- Long/Integer: `null`
- 객체 (SocialLinks): `new SocialLinks(null, null, null, null)`

### TODO 주석 형식
```java
// TODO: Entity에 필드 추가 필요
```

### 파일 경로 규칙
- Controllers: `src/main/java/com/devon/techblog/application/{domain}/controller/`
- DTOs: `src/main/java/com/devon/techblog/application/{domain}/dto/request|response/`
