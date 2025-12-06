# API 스펙 v2 (개선 설계 버전)

이 문서는 기존 `docs/api-spec.md`를 기반으로,  
리뷰에서 도출된 개선 사항(보안, DTO 분리, 문서화 보완 등)을 반영한 **개선 버전 스펙**이다.  
실제 백엔드 구현과 완전히 일치하지 않을 수 있으며, “지향하는 설계”에 가깝다.

---

## 0. 전체 개요 & 목차

- **공통 규칙 / 토큰 & 에러 설계**
- **Auth API**
  - `POST /auth/login`
  - `POST /auth/signup`
  - `POST /auth/logout`
  - `POST /auth/refresh`
  - `GET /auth/check-email`
  - `GET /auth/check-nickname`
- **Member API**
  - `GET /api/v1/members/me`
  - `GET /api/v1/members/{memberId}`
  - `PATCH /api/v1/members/me`
  - `PATCH /api/v1/members/me/password`
  - `DELETE /api/v1/members/me`
- **Post API**
  - `GET /api/v1/posts`
  - `GET /api/v1/posts/{postId}`
  - `POST /api/v1/posts`
  - `PATCH /api/v1/posts/{postId}`
  - `DELETE /api/v1/posts/{postId}`
  - `POST /api/v1/posts/{postId}/like`
  - `DELETE /api/v1/posts/{postId}/like`
- **Comment API**
  - `GET /api/v1/posts/{postId}/comments`
  - `POST /api/v1/posts/{postId}/comments`
  - `PATCH /api/v1/comments/{commentId}`
  - `DELETE /api/v1/comments/{commentId}`

---

## 1. 공통 규칙

### 1-1. 베이스 URL & 버저닝

- 모든 API는 `API_BASE_URL`(환경변수 `VITE_API_BASE_URL`)를 기준으로 한다.
- **버전 경로**
  - 리소스 중심 도메인(`members`, `posts`, `comments`)는 `/api/v1/...` 경로를 사용한다.
  - 인증 도메인(`auth`)는 현재 `/auth/...` 경로를 사용하되, **장기적으로는 `/api/v1/auth/...`로 통합 버저닝하는 방향을 고려**한다.
    - 로그인의 변경 가능성이 상대적으로 낮더라도, 일관된 버저닝은 문서/클라이언트/게이트웨이 관점에서 이점이 크다.

### 1-2. 공통 응답 포맷 – `ApiResponse<T>`

- 모든 API 응답은 다음 래퍼를 사용한다.
- `ApiResponse<T>` (`src/types/common/ApiResponse.ts`)
  - `success: boolean`
  - `message: string | null`
  - `data: T | null`
  - `errors: { field: string; message: string }[] | null`

#### HTTP status 코드 & `ApiResponse` 사용 규칙(권장)

- **성공**
  - 200 OK / 201 Created 등 + `success: true`, `data`에 결과, `errors: null`.
- **클라이언트 오류 (4xx)**
  - 400 Bad Request: 유효성 검증 실패 → `success: false`, `errors`에 필드별 에러.
  - 401 Unauthorized: 인증 실패/토큰 만료 → `success: false`, `message`에 이유.
  - 403 Forbidden: 권한 부족 → `success: false`, `message`에 이유.
  - 404 Not Found: 리소스 없음 → `success: false`, `message`에 이유.
- **서버 오류 (5xx)**
  - 500 Internal Server Error 등 → `success: false`, `message`에 에러 요약.

프론트엔드는 **HTTP status + `success` 필드**를 조합하여 에러를 처리하는 것을 기본으로 한다.

### 1-3. 페이지네이션 – `PageResponse<T>`

- `PageResponse<T>` (`src/types/common/PageResponse.ts`)
  - `items: T[]`
  - `page: number`
  - `size: number`
  - `totalElements: number`
  - `totalPages: number`

### 1-4. 인증 & 토큰 설계

- **인증 방식 (Access Token)**
  - 로그인/회원가입/리프레시 시 **Access Token을 응답 바디로 내려준다.**
  - 프론트엔드는 Access Token을 **브라우저 영구 저장소(localStorage, sessionStorage 등)에 저장하지 않고, 메모리(인메모리 스토어)에만 보관**한다.
    - 권장 구현 (React 기준):
      - `AuthContext` 혹은 전역 상태 관리(Zustand, Redux 등)를 이용해 `accessToken`을 상태로 보관한다.
      - `httpClient`는 이 상태를 구독하거나, `setAccessToken(token: string | null)` 같은 함수를 통해 **내부 메모리 변수**에 토큰을 저장해, 매 요청마다 `Authorization: Bearer <accessToken>` 헤더를 설정한다.
    - 페이지 새로고침/브라우저 종료 시 메모리의 토큰은 사라지며, 이때는 리프레시 토큰(/auth/refresh)을 통해 다시 Access Token을 받아 Auth 상태를 복구한다.
  - 토큰 만료로 401이 발생하면:
    1. `POST {API_BASE_URL}/auth/refresh`를 `withCredentials: true`로 호출해 새 Access Token을 받는다.
    2. Access Token을 메모리 스토어에 갱신한 뒤 원 요청을 재시도한다.
- **리프레시 토큰 & CSRF 방어**
  - 리프레시 토큰은 **HttpOnly + SameSite 쿠키 전략**을 사용한다.
    - JavaScript에서 접근 불가(HttpOnly)로 XSS 시 탈취 난이도 증가.
    - SameSite 설정으로 CSRF 공격에 대해 기본적인 방어.
  - `/auth/refresh`는 이 리프레시 쿠키를 기반으로 Access Token을 발급한다.
- **Access Token을 바디로 내려주는 이유**
  - 로그인/회원가입/리프레시 이후, 클라이언트가 **추가 요청 없이** 바로 인메모리 스토어를 갱신하고 사용할 수 있어 UX와 구현이 단순하다.
  - JS 환경에 노출되는 리스크는 있지만, **디스크에 쓰지 않고 메모리에만 보관**함으로써 노출 범위를 줄이고, 다음과 같은 보안 조치를 전제로 한다.
    - XSS 방지(입력값 escape, sanitizer, CSP 적용 등).
    - 토큰 사용 코드(컨텍스트/상태관리/HTTP 클라이언트)에 대한 리뷰와 최소한의 접근 경로 유지.

#### 1-4-1. React 인메모리 Access Token 구현 가이드

> 실제 구현은 현재 코드(`httpClient`, `AuthContext`)와 다를 수 있으나,  
> 지향하는 구조는 아래와 같다.

- **HttpClient 레벨**
  - `HttpClient` 내부에 **클래스 필드로만 보관되는 `accessToken: string | null`**을 둔다.
  - 공개 메서드:
    - `setAccessToken(token: string | null)` – 토큰을 설정/초기화(로그아웃 시 `null`).
    - `clearTokens()` – 내부 토큰 초기화용 헬퍼(실제로는 `setAccessToken(null)` 호출 정도).
  - 요청 인터셉터는 더 이상 `localStorage`를 보지 않고, **내부 필드 `this.accessToken`만** 읽어 `Authorization` 헤더를 설정한다.
  - `refreshAccessToken()`는 `/auth/refresh` 호출 후 응답의 Access Token을 `setAccessToken`으로 반영한다.

- **AuthContext 레벨**
  - React의 `AuthContext`가 **사용자 정보(`user`)와 인증 상태, 그리고 Access Token 제어를 책임**진다.
  - 주요 흐름:
    - 로그인/회원가입 성공 시:
      - 응답 바디의 `accessToken`을 받아 `httpClient.setAccessToken(token)` 호출.
      - 이어서 `loadUser()`로 `/api/v1/members/me`를 호출해 `user` 상태를 세팅.
    - 앱 초기 로드 시:
      - `localStorage` 등에서 토큰을 읽지 않는다.
      - 필요시 앱 부팅 단계에서 `/auth/refresh`를 한 번 호출(리프레시 쿠키 기반)해 Access Token을 받아오고, 성공 시 `httpClient.setAccessToken` + `loadUser()`를 수행한다.
    - 로그아웃 시:
      - 서버에 `/auth/logout` 호출.
      - `httpClient.setAccessToken(null)` 또는 `clearTokens()`로 인메모리 토큰 제거.
      - `user` 상태를 `null`로 초기화.

- **요약**
  - Access Token은 **오직 메모리(React 상태/HttpClient 내부 필드)에만 존재**하고, 디스크(localStorage, sessionStorage, 쿠키)에 저장하지 않는다.
  - 세션 복원은 **리프레시 쿠키 + `/auth/refresh` 호출**을 통해 이루어지며, 네트워크 왕복이 추가되지만 보안/일관성 측면에서 이점이 있다.

### 1-5. 권한(인가) 표기 규칙

이 문서에서는 각 엔드포인트에 대해 다음과 같이 권한을 표기한다.

- `인증: 공개` – 로그인 여부와 무관하게 접근 가능.
- `인증: 필요` – Authorization 헤더가 필요.
- `권한: 자기 자신만` – 본인 리소스만 조작 가능 (예: `members/me`).
- `권한: 작성자만` – 게시글/댓글 작성자만 수정/삭제 가능.
- `권한: 관리자` – 관리 권한이 필요한 경우(현재 문서에는 구체 엔드포인트 없음, 추후 확장).

프론트엔드는 이 정보를 기준으로 **UI 레벨에서 최소한의 방어(버튼 숨김 등)**를 수행하고,  
최종 권한 검증은 항상 서버에서 한다.

---

## 2. Auth API

소스: `src/api/authApi.ts`, `src/api/httpClient.ts`, `src/features/auth/AuthContext.tsx`, `src/pages/OAuthCallbackPage.tsx`

### DTO

- `LoginRequest` (`src/types/auth/LoginRequest.ts`)
  - `email: string`
  - `password: string`
- `LoginResponse` (`src/types/auth/LoginResponse.ts`)
  - `userId: number`
  - `accessToken: string`
- `SignupRequest` (`src/types/auth/SignupRequest.ts`)
  - `email: string`
  - `password: string`
  - `passwordConfirm: string`
  - `nickname: string`

> **Validation 예시(권장)**  
> - `email`: 이메일 형식, 최대 길이 제한(예: 255자).  
> - `password`: 최소 8자 이상, 최대 64자, 영문/숫자/특수문자 조합 권장.  
> - `nickname`: 공백 불가, 최대 길이 제한(예: 30자).

### 2-1) 로그인 – `POST /auth/login`

- **설명**: 이메일/비밀번호 로그인.
- **인증**: 공개.
- **요청 바디**: `LoginRequest`
- **성공 응답**
  - 200 OK
  - `ApiResponse<LoginResponse>`
- **대표 에러**
  - 400: 입력값 형식 오류.
  - 401: 이메일/비밀번호 불일치.

### 2-2) 회원가입 – `POST /auth/signup`

- **설명**: 회원가입 후 Access Token 발급 및 로그인 상태 진입.
- **인증**: 공개.
- **요청 바디**: `SignupRequest`
- **성공 응답**
  - 201 Created
  - `ApiResponse<LoginResponse>`
- **대표 에러**
  - 400: 유효성 검증 실패 (이메일 형식, 비밀번호 정책 등).
  - 409: 이메일/닉네임 중복.

### 2-3) 로그아웃 – `POST /auth/logout`

- **설명**: 서버 측 세션/리프레시 토큰 무효화.
- **인증**: 필요.
- **요청 바디**: 없음.
- **성공 응답**
  - 200 OK
  - `ApiResponse<null>`

### 2-4) 토큰 재발급 – `POST /auth/refresh`

- **설명**: HttpOnly + SameSite 쿠키에 저장된 리프레시 토큰을 이용해 Access Token 재발급.
- **인증**: 쿠키 기반 (헤더 토큰 없이도 동작).
- **요청 바디**: `{}` (빈 객체).
- **성공 응답**
  - 200 OK
  - `ApiResponse<{ accessToken: string }>`
- **대표 에러**
  - 401: 리프레시 토큰 만료/위조.
  - 403: CSRF 등 보안 검증 실패 시.

### 2-5) 이메일 중복 체크 – `GET /auth/check-email`

- **설명**: 이메일 사용 가능 여부 확인.
- **인증**: 공개.
- **쿼리 파라미터**
  - `email: string`
- **성공 응답**
  - 200 OK
  - `ApiResponse<{ available: boolean }>`

### 2-6) 닉네임 중복 체크 – `GET /auth/check-nickname`

- **설명**: 닉네임 사용 가능 여부 확인.
- **인증**: 공개.
- **쿼리 파라미터**
  - `nickname: string`
- **성공 응답**
  - 200 OK
  - `ApiResponse<{ available: boolean }>`

---

## 3. Member API (회원)

소스: `src/api/memberApi.ts`, `src/types/member/MemberResponse.ts`

### DTO: `MemberResponse` (정리 버전)

> 기존 스펙의 `memberId`와 `id` 중복은 잘못된 설계로 판단하여,  
> **외부 노출용 ID는 `memberId` 하나만 사용**하는 것으로 통일한다.

- `MemberResponse`
  - `memberId: number`
  - `email: string`
  - `nickname: string`
  - `profileImage: string`
  - `handle: string`
  - `bio: string`
  - `role: string`
  - `company: string`
  - `location: string`
  - `primaryStack: string[]`
  - `interests: string[]`
  - `socialLinks: { github: string; website: string; linkedin: string; notion: string }`

### DTO: `UpdateProfileRequest` (신규 정의)

> 응답 모델과 수정용 요청 모델을 분리하여,  
> 사용자가 실제로 수정 가능한 필드만 포함한다.

- `UpdateProfileRequest`
  - `nickname?: string`
  - `profileImage?: string`
  - `handle?: string`
  - `bio?: string`
  - `role?: string`
  - `company?: string`
  - `location?: string`
  - `primaryStack?: string[]`
  - `interests?: string[]`
  - `socialLinks?: { github?: string; website?: string; linkedin?: string; notion?: string }`

### 3-1) 내 프로필 조회 – `GET /api/v1/members/me`

- **설명**: 현재 로그인한 회원의 프로필 조회.
- **인증**: 필요.
- **권한**: 자기 자신만.
- **성공 응답**
  - 200 OK
  - `ApiResponse<MemberResponse>`

### 3-2) 특정 회원 프로필 조회 – `GET /api/v1/members/{memberId}`

- **설명**: 특정 회원의 공개 프로필 조회.
- **인증**: 선택적 (서비스 정책에 따라 공개/비공개 여부 결정).
- **경로 파라미터**
  - `memberId: number`
- **성공 응답**
  - 200 OK
  - `ApiResponse<MemberResponse>`
- **대표 에러**
  - 404: 회원 없음 또는 비공개.

### 3-3) 내 프로필 수정 – `PATCH /api/v1/members/me`

- **설명**: 자신의 프로필 정보 수정 (부분 업데이트).
- **인증**: 필요.
- **권한**: 자기 자신만.
- **요청 바디**
  - `UpdateProfileRequest`
- **성공 응답**
  - 200 OK
  - `ApiResponse<MemberResponse>`

### 3-4) 비밀번호 변경 – `PATCH /api/v1/members/me/password`

- **설명**: 현재 비밀번호 검증 후 새 비밀번호로 변경.
- **인증**: 필요.
- **권한**: 자기 자신만.
- **요청 바디**
  - `currentPassword: string`
  - `newPassword: string`
- **성공 응답**
  - 200 OK
  - `ApiResponse<null>`

### 3-5) 계정 삭제 – `DELETE /api/v1/members/me`

- **설명**: 회원 탈퇴.
- **인증**: 필요.
- **권한**: 자기 자신만 (또는 관리자).
- **성공 응답**
  - 200 OK / 204 No Content
  - `ApiResponse<null>` (사용 시).

---

## 4. Post API (게시글)

소스: `src/api/postApi.ts`, `src/types/post/*`

### DTO

- `PostCreateRequest`
  - `title: string`
  - `content: string`
  - `image?: string`
  - `summary?: string`
  - `tags?: string[]`
  - `seriesId?: number | null`
  - `visibility?: 'public' | 'private'`
  - `isDraft?: boolean`
  - `commentsAllowed?: boolean`
- `PostUpdateRequest` (수정 버전)
  - 게시글 수정에서 사용.
  - **필수 필드는 없고**, 변경하고자 하는 필드만 포함할 수 있도록 모두 optional 권장.
  - 필드 목록은 `PostCreateRequest`와 동일:
    - `title?: string`
    - `content?: string`
    - `image?: string`
    - `summary?: string`
    - `tags?: string[]`
    - `seriesId?: number | null`
    - `visibility?: 'public' | 'private'`
    - `commentsAllowed?: boolean`
- `PostResponse`
  - `postId: number`
  - `member: MemberResponse`
  - `title: string`
  - `content: string`
  - `summary: string`
  - `imageUrl: string`
  - `createdAt: string`
  - `updatedAt: string`
  - `viewCount: number`
  - `likeCount: number`
  - `commentCount: number`
  - `isLiked: boolean`
  - `tags: string[]`
  - `seriesId: number | null`
  - `seriesName: string`
  - `visibility: 'public' | 'private'`
- `PostSummaryResponse`
  - `postId: number`
  - `title: string`
  - `summary: string`
  - `thumbnail: string`
  - `member: MemberResponse`
  - `createdAt: string`
  - `viewCount: number`
  - `likeCount: number`
  - `commentCount: number`

> **Validation 예시(권장)**  
> - `title`: 필수, 최대 100자.  
> - `content`: 필수, 최소 길이 제한(예: 1자 이상).  
> - `summary`: 생략 시 서버/클라이언트에서 자동 생성 가능.  

### 4-1) 게시글 목록 조회 – `GET /api/v1/posts`

- **설명**: 게시글 리스트 조회 (메인, 검색, 특정 유저 글 등).
- **인증**: 공개.
- **쿼리 파라미터**
  - `page?: number` (기본값 0)
  - `size?: number` (기본값 20)
  - `sort?: string` (기본값 `'createdAt,desc'`)
  - `memberId?: number`
  - `search?: string`
- **성공 응답**
  - 200 OK
  - `ApiResponse<PageResponse<PostSummaryResponse>>`

### 4-2) 게시글 상세 조회 – `GET /api/v1/posts/{postId}`

- **설명**: 단일 게시글 조회.
- **인증**: 선택적.
- **경로 파라미터**
  - `postId: number`
- **쿼리 파라미터(선택)**
  - `memberId?: number` – 로그인 사용자의 관점으로 `isLiked` 계산 등에 활용.
- **성공 응답**
  - 200 OK
  - `ApiResponse<PostResponse>`
- **대표 에러**
  - 404: 게시글 없음 또는 권한 없음.

### 4-3) 게시글 생성 – `POST /api/v1/posts`

- **설명**: 새 게시글 작성.
- **인증**: 필요.
- **권한**: 로그인 사용자.
- **요청 바디**
  - `PostCreateRequest`
- **성공 응답**
  - 201 Created
  - `ApiResponse<PostResponse>`

### 4-4) 게시글 수정 – `PATCH /api/v1/posts/{postId}`

- **설명**: 기존 게시글 수정 (부분 업데이트).
- **인증**: 필요.
- **권한**: 작성자만.
- **경로 파라미터**
  - `postId: number`
- **요청 바디**
  - `PostUpdateRequest`
- **성공 응답**
  - 200 OK
  - `ApiResponse<PostResponse>`

### 4-5) 게시글 삭제 – `DELETE /api/v1/posts/{postId}`

- **설명**: 게시글 삭제.
- **인증**: 필요.
- **권한**: 작성자 또는 관리자.
- **경로 파라미터**
  - `postId: number`
- **성공 응답**
  - 200 OK / 204 No Content
  - `ApiResponse<null>` (사용 시).

### 4-6) 게시글 좋아요 설정 – `POST /api/v1/posts/{postId}/like`

- **설명**: 해당 게시글에 “좋아요”를 설정한다.
- **인증**: 필요.
- **권한**: 로그인 사용자.
- **경로 파라미터**
  - `postId: number`
- **성공 응답**
  - 200 OK
  - `ApiResponse<{ liked: boolean }>` – 일반적으로 `liked: true`

### 4-7) 게시글 좋아요 해제 – `DELETE /api/v1/posts/{postId}/like`

- **설명**: 해당 게시글의 “좋아요”를 해제한다.
- **인증**: 필요.
- **권한**: 로그인 사용자.
- **경로 파라미터**
  - `postId: number`
- **성공 응답**
  - 200 OK
  - `ApiResponse<{ liked: boolean }>` – 일반적으로 `liked: false`

> **참고**: 백엔드는 이미 POST/DELETE로 분리되어 있고,  
> 프론트엔드는 이 스펙에 맞추어 “토글”이 아니라 “명시적 설정/해제” 방식으로 점진적으로 전환할 수 있다.

---

## 5. Comment API (댓글)

소스: `src/api/commentApi.ts`, `src/types/comment/CommentResponse.ts`

### DTO

- `CommentResponse`
  - `commentId: number`
  - `postId: number`
  - `content: string`
  - `member: MemberResponse`
  - `createdAt: string`
  - `updatedAt: string`
- `CommentCreateRequest`
  - `content: string`
- `CommentUpdateRequest`
  - `content: string`

> **Validation 예시(권장)**  
> - `content`: 공백 불가, 최대 길이 제한(예: 500자).

### 5-1) 댓글 목록 조회 – `GET /api/v1/posts/{postId}/comments`

- **설명**: 특정 게시글의 댓글 리스트 조회(페이지네이션).
- **인증**: 공개(공개 게시글의 경우).
- **경로 파라미터**
  - `postId: number`
- **쿼리 파라미터**
  - `page: number` (기본값 0)
  - `size: number` (기본값 20)
- **성공 응답**
  - 200 OK
  - `ApiResponse<PageResponse<CommentResponse>>`

### 5-2) 댓글 작성 – `POST /api/v1/posts/{postId}/comments`

- **설명**: 게시글에 댓글 작성.
- **인증**: 필요.
- **권한**: 로그인 사용자.
- **경로 파라미터**
  - `postId: number`
- **요청 바디**
  - `CommentCreateRequest`
- **성공 응답**
  - 201 Created
  - `ApiResponse<CommentResponse>`

### 5-3) 댓글 수정 – `PATCH /api/v1/comments/{commentId}`

- **설명**: 댓글 수정 (부분 업데이트).
- **인증**: 필요.
- **권한**: 작성자만.
- **경로 파라미터**
  - `commentId: number`
- **요청 바디**
  - `CommentUpdateRequest`
- **성공 응답**
  - 200 OK
  - `ApiResponse<CommentResponse>`

### 5-4) 댓글 삭제 – `DELETE /api/v1/comments/{commentId}`

- **설명**: 댓글 삭제.
- **인증**: 필요.
- **권한**: 작성자 또는 관리자.
- **경로 파라미터**
  - `commentId: number`
- **성공 응답**
  - 200 OK / 204 No Content
  - `ApiResponse<null>` (사용 시).

---

## 6. 요약

- 이 v2 스펙은 다음을 목표로 한다.
  - `MemberResponse` ID 필드 정리(`memberId` 단일화).
  - 프로필/게시글 수정용 DTO 분리 (`UpdateProfileRequest`, `PostUpdateRequest`).
  - 좋아요 API를 POST/DELETE로 명시적 분리.
  - Auth/토큰/권한/에러 정책을 문서 레벨에서 명확히 표기.
- 실제 백엔드/프론트 코드가 이 스펙과 다를 경우,  
  이 문서를 기준으로 점진적으로 정렬해 나가는 것을 권장한다.
