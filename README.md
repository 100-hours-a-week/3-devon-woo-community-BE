# Devon Tech Blog Backend

기술 블로그·개발 커뮤니티를 위한 REST API 서버입니다.  
게시글, 댓글, 회원, 이미지 업로드를 중심으로 실제 서비스 운영을 가정해 설계했습니다.

---

## 1. 서비스 개요

- 기술 블로그 + 커뮤니티 백엔드
- 도메인: 게시글, 댓글, 회원, 이미지, 좋아요, 조회수
- 구조: `application / domain / infra` 3계층
- 목표: 읽기 많은 서비스에서 안정적으로 동작하는 콘텐츠 서비스 백엔드

---

## 2. 기술 스택

- **Language & Framework**
  - Java 21
  - Spring Boot 3 (Web, Validation, Data JPA, Security, OAuth2 Client)
- **Database & Persistence**
  - MySQL, H2(테스트)
  - Spring Data JPA, QueryDSL
- **Infra**
  - Redis (조회수·캐시)
  - Cloudinary (이미지 업로드)
- **Auth & Security**
  - Spring Security, JWT
  - OAuth2(Google)
- **API & 품질**
  - springdoc-openapi(Swagger UI)
  - JUnit 5, Spring Test, Spring Security Test, JaCoCo

---

## 3. 주요 기능

- 게시글
  - 작성, 수정, 삭제, 단건/목록 조회
  - 페이징, 정렬, 요약/상세 분리
- 댓글
  - 게시글 기준 목록 조회
  - 작성, 수정, 삭제
- 좋아요
  - 게시글 좋아요/좋아요 취소
  - 중복 요청 방지
- 조회수
  - Redis 기반 조회수 증가 처리
  - 조회수 정책을 별도 모듈로 분리
- 회원/인증
  - 회원 가입 및 프로필
  - JWT 토큰 발급/갱신, OAuth2(Google) 로그인
  - `@CurrentUser` 로 현재 사용자 주입
- 이미지
  - Cloudinary를 통한 이미지 업로드 및 URL 제공

---

## 4. 아키텍처 & 테스트

- 아키텍처
  - `application`: 컨트롤러, 서비스, DTO 등 유스케이스 중심 계층
  - `domain`: 엔티티와 정책(Policy)으로 비즈니스 규칙 캡슐화
  - `infra`: DB, Redis, Cloudinary 등 외부 시스템 연동
- 테스트
  - 단위 테스트: 도메인 정책 검증
  - 슬라이스/통합 테스트: 웹·DB·보안 흐름 검증
  - JaCoCo로 핵심 도메인 커버리지 관리 (DTO/설정 등은 제외)

---

## 5. 실행 방법

```bash
# 의존성 설치 & 빌드
./gradlew build

# 로컬 실행 (MySQL, Redis, Cloudinary 환경 변수 필요)
./gradlew bootRun

# 테스트 실행
./gradlew test           # 전체 테스트
./gradlew unitTest       # 단위 테스트만
./gradlew integrationTest # 통합 테스트만
```

보다 자세한 의사결정과 설계 배경은 `docs/` 및 `Tech/` 디렉터리의 ADR·설계 문서를 참고할 수 있습니다.
