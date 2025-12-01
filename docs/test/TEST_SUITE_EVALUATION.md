# 테스트 평가 리포트

마지막 업데이트: 2025-11-30

---

## 1. 현재 테스트 자산 요약

- **도메인/리포지토리 층**: `MemberRepositoryTest`, `PostRepositoryTest` 등에서 새 필드, Soft Delete, 카운터 쿼리까지 꾸준히 검증하고 있다.
- **애플리케이션 서비스 층**: `PostServiceTest`, `CommentServiceTest`, `MemberServiceTest` 등 핵심 서비스마다 기본적인 성공 플로우와 일부 예외를 단위 테스트로 다루고 있다.
- **웹 계층**: `@ControllerWebMvcTest` 기반의 슬라이스 테스트와 `MockMvc` 인수 테스트가 주요 API(회원, 게시글, 댓글, 인증) happy path를 검증한다.
- **시큐리티**: `AuthIntegrationTest`에서 `/auth/refresh` 흐름만 검증하며, 필터/핸들러/서비스 단위 테스트는 존재하지 않는다.

---

## 2. 잘 하고 있는 부분

- `src/test/java/com/devon/techblog/domain/post/repository/PostRepositoryTest.java` 전반(1-210행)에서 좋아요/댓글/조회수 증감, 태그 `@ElementCollection`, 시리즈 매핑까지 DB 스키마 변화에 맞춰 꼼꼼히 검증한다.
- `src/test/java/com/devon/techblog/application/post/service/PostViewServiceTest.java:18-55`처럼 정책 객체(`ViewCountPolicy`)를 더블로 주입해 부수효과 여부만 검증하는 패턴이 잘 잡혀 있다.
- `src/test/java/com/devon/techblog/integration/post/PostIntegrationTest.java:37-192`, `MemberIntegrationTest`, `CommentIntegrationTest` 등은 API 응답 래퍼(`ApiResponse`) 구조, Soft Delete, 카운터 변화까지 실제 DB와 함께 검증하여 회귀 버그를 잡는 데 도움이 된다.
- `src/test/java/com/devon/techblog/application/member/validator/MemberValidatorTest.java:25-86`, `AuthValidatorTest`에서 검증 로직을 서비스와 분리해 단독으로 테스트하고 있어 리팩터링 내성이 있다.

---

## 3. 위험 신호 및 커버리지 공백

1. **서비스 단위 테스트가 성공 흐름 편향**  
   - `PostServiceTest`는 생성/수정/삭제 성공과 단일 not-found만 검증한다(`src/test/java/.../PostServiceTest.java:70-158`). 하지만 실제 서비스(`src/main/java/com/devon/techblog/application/post/service/PostService.java:53-170`)는 소유권 검사, 태그/썸네일/visibility/Draft 전환, 이미지 교체 로직을 포함한다. 비정상 플로우(회원 미존재, 소유권 불일치, 첨부 이미지 교체 시 기존 첨부 삭제, 태그 필터 조회 등)가 테스트되지 않아 회귀에 취약하다.
   - `CommentServiceTest` 또한 `OwnershipPolicy` 예외, 댓글 수 증감, `CommentErrorCode.COMMENT_NOT_FOUND` 흐름을 검증하지 않는다(`src/test/java/.../CommentServiceTest.java:68-158`).
   - `MemberServiceTest`는 검증기(`MemberValidator`) 상호작용 실패, 추가 프로필 필드 업데이트, 탈퇴 후 재조회 등의 케이스를 다루지 않는다(`src/test/java/.../MemberServiceTest.java:46-99`).

2. **컨트롤러/인수 테스트가 모두 Happy Path**  
   - `MemberControllerTest`, `PostControllerTest`, `CommentControllerTest` 등 (`src/test/java/com/devon/techblog/application/**/controller/*.java`)은 성공 응답만 검증한다. 입력 검증 실패, 권한 오류, 서비스 예외 발생 시 공통 예외 핸들링이 올바른지 확인할 방법이 없다.
   - 인수 테스트(`src/test/java/com/devon/techblog/integration/**`) 역시 `TestCurrentUserContext`로 강제 로그인한 성공 케이스에 집중되어 있으며, 인증 실패(401/403), 잘못된 파라미터, 소유권 불일치 시 흐름을 확인하지 않는다.

3. **시큐리티 계층 테스트 부재**  
   - `JwtAuthenticationFilter`, `CustomLogoutFilter`, `LogoutHandler`, `TokenRefreshService`, `TokenBlacklistService`, `CookieProvider`, `LoginService`, `OAuthLoginService` 등 핵심 컴포넌트에 단위/통합 테스트가 전무하다 (`src/main/java/com/devon/techblog/application/security/**`).  
   - 특히 `LogoutHandler`는 `Optional`을 문자열로 변환한 뒤 블랙리스트에 등록하는 버그 가능성이 있는데(`src/main/java/com/devon/techblog/application/security/handler/LogoutHandler.java:27-47`), 이를 잡아줄 테스트가 없다.
   - `/auth/logout`, `/auth/login` 엔드포인트는 Swagger 문서용이라고 하지만 필터 체인 흐름 자체를 검증하지 않아 쿠키 삭제, 블랙리스트 등록, 토큰 만료 처리의 신뢰도가 낮다.

4. **정책/도메인 테스트의 공백**  
   - `ViewCountPolicyTest`가 전체 비활성화 상태로 방치되어 있어 정책 코드 변경 시 아무런 안전망이 없다(`src/test/java/com/devon/techblog/domain/post/policy/ViewCountPolicyTest.java:15-27`).
   - `PostService`의 태그 기반 페이지 조회(`getPostPageByTags`), 댓글/좋아요 카운터 동기화 로직 등은 단위/통합 테스트 어디에서도 검증되지 않는다.

---

## 4. 추가로 작성이 필요한 테스트 제안 (우선순위순)

1. **시큐리티 컴포넌트 단위 테스트**
   - `JwtAuthenticationFilter`에 대해 유효한 토큰 시 `SecurityContext` 세팅/만료 토큰 무시/예외 로깅을 검증 (`src/main/java/.../JwtAuthenticationFilter.java:24-67`).
   - `TokenRefreshService`의 토큰 유형/만료/회원 상태 분기, `TokenBlacklistService`의 TTL·해시 키 생성, `CookieProvider`의 쿠키 생성·삭제, `LogoutHandler`의 블랙리스트 등록/쿠키 삭제/응답 바디를 각각 단위 테스트.
   - `CustomLogoutFilter`가 `/auth/logout` POST 외 요청을 패스시키는지, OAuth 로그인 서비스가 신규/기존 사용자 흐름을 구분하는지 검증.

2. **서비스 계층의 예외/정책 테스트 보강**
   - `PostServiceTest`: 회원 미존재, `OwnershipPolicy` 위반, 첨부 이미지 교체 시 기존 첨부 삭제 여부, `getPostPageByTags` 결과 매핑 등을 추가.
   - `CommentServiceTest`: `OwnershipPolicy` 실패, 댓글/포스트 미존재, 댓글 수 증감 보장, `findPostIdByCommentId` 미반환 시 `CommentErrorCode` 확인.
   - `MemberServiceTest`: 닉네임 중복 시 `MemberValidator` 예외, 프로필 필드 null/빈 리스트 처리, 탈퇴 후 재조회 불가 등을 검증.

3. **컨트롤러/인수 테스트의 오류 흐름**
   - 각 Controller 슬라이스 테스트에 입력 유효성 실패, 서비스 예외 발생 시 HTTP Status/메시지를 확인하는 케이스 추가.
   - 인수 테스트에 인증 누락(401), 소유권 불일치(403), 존재하지 않는 리소스(404), 잘못된 파라미터(400)를 명시적으로 포함. 특히 `/api/v1/posts/{id}/like`, `/api/v1/comments/{id}` 등에 대한 cross-member 요청 시 동작 검증.

4. **로그아웃/블랙리스트 플로우 통합 테스트**
   - `MockMvc` 기반으로 `/auth/logout` 요청 시 쿠키 삭제, 블랙리스트 저장, 응답 메세지, 재요청 시 차단 여부를 검증.  
   - `/auth/refresh`에 블랙리스트 적용 여부, 만료 직전·만료 이후 케이스를 추가해 회귀를 막는다.

5. **정책/도메인 테스트 복구**
   - `ViewCountPolicyTest`를 활성화하여 IP/UA/멤버 조합별 정책을 검증하고, `ViewCountPolicy` 변경이 `PostViewService`와 함께 동작하는지 교차 테스트.
   - 게시글/댓글 카운터 정책(증가/감소)이 동시성/경계값에서 잘 유지되는지 레포지토리 테스트를 보강.

6. **OAuth/외부 연동 Stub 테스트**
   - `OAuthLoginService`에 Fake `OAuth2UserService` 응답을 주입해 신규 가입/기존 사용자 로그인 시 멱등 행동을 검증.
   - Cloudinary `ImageController`에는 실패/예외 상황(서명 생성 실패, 지원되지 않는 type)을 테스트해 API 안정성을 높인다.

위 테스트를 순차적으로 보강하면 Happy Path 중심의 테스트 편중을 해소하고, 특히 인증/보안/정책 영역에서 회귀 위험을 크게 줄일 수 있다.
