# T-Log WebFlux Module (`app-webflux`)

T-Log의 **AI / 스트리밍 전용 WebFlux 애플리케이션**입니다.  
LLM 기반 텍스트 생성·요약·리뷰 기능을 제공하며, `/webflux/**` 경로로 Nginx 뒤에서 API 모듈(`app-api`)과 함께 동작합니다.

---

## 1. 모듈 역할

- **LLM Proxy / Orchestrator**
  - OpenAI API를 래핑해 텍스트 생성, 요약, 리뷰 기능 제공
  - 스트리밍(SSE)과 비스트리밍(일반 HTTP) 응답을 모두 지원
- **WebFlux 기반 비동기 처리**
  - Spring WebFlux + WebClient로 **I/O 논블로킹** 호출
  - 프론트엔드에서 긴 응답을 토큰 단위로 받아볼 수 있도록 설계
- **T-Log 메인 API와 분리된 런타임**
  - `app-api`와 분리된 프로세스로 배포하여 **AI 트래픽과 일반 트래픽을 격리**
  - Nginx에서 `/webflux/**` 경로로 라우팅 (`infra/nginx/nginx.conf` 참고)

자세한 엔드포인트는 WebFlux API 문서를 참고하세요.  
- [docs/api/API.md](../../docs/api/API.md)

---

## 2. 디렉터리 / 패키지 구조

### 2-1. 디렉터리 구조 (요약)

```text
app-webflux/
 ├── build.gradle
 ├── Dockerfile
 └── src
     ├── main
     │   ├── java/com/devon/techblog
     │   │   ├── presentation  # WebFlux 컨트롤러 (AI, 요약, 리뷰, 헬스 체크)
     │   │   ├── service       # ChatService, OpenAiChatService 등 비즈니스 로직
     │   │   ├── strategy      # PromptStrategy 및 프롬프트 구현체
     │   │   ├── dto           # 요청 DTO
     │   │   ├── config        # WebFlux, Security, WebClient, CORS 설정
     │   │   └── util          # OpenAI 스트림 파서 등 유틸
     │   └── resources
     │       ├── application.yml
     │       └── static/index.html
     └── test
         └── java/com/devon/techblog
             └── ... WebFlux 관련 테스트
```

## 3. 아키텍처 개요

패키지 기준으로 역할을 나누어 설계합니다.

- `presentation`  
  - `AiController`, `GenerateTextController`, `SummarizeController`, `ReviewController`, `HealthController`  
  - HTTP 엔드포인트 정의, 요청 DTO 바인딩, 응답 타입 결정 (String / SSE)
- `service`  
  - `ChatService`, `OpenAiChatService`  
  - 전략 선택, OpenAI 호출, 응답 스트림 파이프라인 구성
- `strategy`  
  - `PromptStrategy` 인터페이스 기반 전략 패턴  
  - `DefaultPromptStrategy`, `SummarizePromptStrategy`, `ReviewPromptStrategy`, `GenerateTextPromptStrategy`  
  - 각 기능별 시스템 프롬프트(요약/리뷰/텍스트 생성) 캡슐화
- `dto`  
  - `ChatRequest`, `SummarizeRequest`, `ReviewRequest`, `GenerateTextRequest` 등 요청 DTO
- `config`  
  - `TechBlogWebFluxApplication`: WebFlux 애플리케이션 엔트리포인트  
  - `WebClientConfig`: OpenAI 호출용 WebClient 설정  
  - `SecurityConfig`: 최소한의 WebFlux 보안 설정  
  - `CorsConfig`, `CorsProperties`: CORS 정책 분리
- `util`  
  - `OpenAiStreamParser`: OpenAI streaming 응답(JSON line)을 텍스트 스트림으로 변환

---

## 4. 주요 의존성

`app-webflux/build.gradle` 기준 핵심 의존성입니다.

- Web / Reactive
  - `spring-boot-starter-webflux`
  - `spring-boot-starter-validation`
  - `spring-boot-starter-security`
- Database
  - `spring-boot-starter-data-r2dbc`
  - `io.asyncer:r2dbc-mysql`
- 공통
  - Lombok, Spring Boot Configuration Processor
- Test
  - `spring-boot-starter-test`
  - `reactor-test`
  - `spring-security-test`

환경 변수 및 설정은 `application.yml` 에서 관리합니다.

- 서버 포트: `APP_WEBFLUX_SERVER_PORT` (기본 8081)
- CORS: `CORS_ALLOWED_ORIGIN` (기본 `http://localhost:3000`)
- OpenAI:
  - `OPENAI_API_KEY` (필수)
  - 기본 모델: `openai.model: gpt-3.5-turbo`

자세한 설정 값은 `app-webflux/src/main/resources/application.yml` 을 참고하세요.

---

## 5. 컨벤션

### 4-1. 컨트롤러

- 모든 컨트롤러는 `@RestController` + WebFlux 타입(`Mono<T>`, `Flux<T>`)을 사용합니다.
- 스트리밍 엔드포인트는 `produces = MediaType.TEXT_EVENT_STREAM_VALUE` 로 SSE를 명시합니다.
- 요청 DTO는 `@RequestBody` + `@Valid` 를 사용하고, 도메인/응답 포맷은 단순 String 또는 DTO로 유지합니다.

### 4-2. 서비스 & 전략

- 서비스는 최대한 **I/O 조합 및 전략 선택**에 집중하고, 프롬프트 내용은 `strategy` 패키지에 위임합니다.
- 새로운 AI 기능을 추가할 때는
  1. `PromptStrategy` 구현체 추가
  2. 해당 전략을 사용하는 Service 메서드 추가
  3. Controller에서 해당 기능용 엔드포인트 추가
  - 이 패턴으로 기능을 확장합니다.

### 4-3. 설정 & 보안

- CORS 설정은 `CorsProperties` (`@ConfigurationProperties`) + `CorsConfig` 조합으로 관리합니다.
- OpenAI 설정은 `OpenAiProperties` (`@ConfigurationProperties(prefix = "openai")`) 로 바인딩합니다.
- 테스트용 보안 설정은 `TestSecurityConfig` 를 통해 최소 권한으로 WebFlux 테스트가 가능하도록 구성합니다.

### 4-4. 테스트

- 컨트롤러 테스트는 `@WebFluxTest` 를 사용하여 슬라이스 테스트로 분리합니다.
- Reactor 타입 응답에 대해서는 `StepVerifier` 나 `WebTestClient` 로 검증합니다.
- 보안이 걸린 엔드포인트는 `spring-security-test` 의 헬퍼를 활용합니다.

---

## 6. 관련 문서

이 모듈과 직접 관련된 문서는 아래를 참고할 수 있습니다.

- WebFlux API 명세: [docs/api/API.md](../../docs/api/API.md)
- 전체 아키텍처/레이어 구조: [docs/architecture/CLEAN_ARCHITECTURE.md](../../docs/architecture/CLEAN_ARCHITECTURE.md)
- Nginx 라우팅 설정 (`/webflux/**`): [infra/nginx/nginx.conf](../../infra/nginx/nginx.conf)
