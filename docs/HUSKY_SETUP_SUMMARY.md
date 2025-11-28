## Husky 설정 요약 & 컨벤션 (현재 상태 기준)

이 문서는 **현재 레포에 적용된 Husky 설정과 컨벤션을 정리한 요약본**입니다.  
실제 동작 중인 훅 파일과 Gradle 설정을 기준으로 작성되었습니다.

---

## 1. Husky 기본 설정

- `package.json`
  - `devDependencies`:
    - `"husky": "^9.1.7"`
  - `scripts`:
    - `"prepare": "husky"`
- Husky 초기화 후, 프로젝트 루트에 `.husky/` 디렉터리가 존재하며 아래 훅이 사용 중입니다.
  - `.husky/pre-commit`
  - `.husky/prepare-commit-msg`
  - `.husky/pre-push`
- 모든 훅 스크립트는 **POSIX sh** 기준으로 작성:
  - 맨 위에 `#!/usr/bin/env sh`
  - Windows에서는 **Git Bash / WSL** 환경을 가정.

---

## 2. 훅별 역할 요약

### 2.1 `pre-commit`

- 파일: `.husky/pre-commit`
- 내용 (요약):
  - 빠른 단위 테스트 태스크(`unitTest`) 실행.
    - `./gradlew unitTest` 가 실패하면 커밋을 차단.
- 목적:
  - 태그 기반 `unitTest` 를 통해 **기본적인 테스트를 통과한 변경만 커밋**되도록 보장.

### 2.2 `prepare-commit-msg` (커밋 메시지 컨벤션 강제)

- 파일: `.husky/prepare-commit-msg`
- 역할:
  - 커밋 메시지 첫 줄을 검사해서 **정해진 타입 + 이모지 포맷**으로 강제.
  - 규칙에 맞지 않으면 커밋을 **실패**시키고 한글 안내 메시지를 출력.

#### 2.2.1 이미 포맷된 메시지 / Git 기본 메시지 예외

- 이미 이모지+타입으로 시작하는 경우 **그대로 통과**:
  - 예:  
    - `✨ feat: ...`  
    - `🐛 fix: ...`  
    - `⚡ perf: ...`  
    - `♻️ refactor: ...`  
    - `🧪 test: ...`  
    - `📝 docs: ...`  
    - `🎨 style: ...`  
    - `🔧 chore: ...`  
    - `🚀 delopy: ...`  
    - `⏪ revert: ...`
- Git이 자동 생성하는 메시지(머지, 리베이스 등)는 예외 처리:
  - `Merge*`, `Revert*`, `Amend*`, `Reset*`, `Rebase*`, `Tag*` 로 시작하면 훅을 그냥 종료.

#### 2.2.2 타입 → 이모지 매핑 규칙

- 커밋 메시지 첫 줄에서 **맨 앞 영문 단어**를 타입으로 인식:
  - `type=$(echo "$first_line" | grep -o "^[A-Za-z]*")`
  - `normalized_type=$(echo "$type" | tr 'A-Z' 'a-z')`
- 허용 타입 (소문자 기준)과 이모지:
  - `feat`   → `✨`
  - `fix`    → `🐛`
  - `perf`   → `⚡`
  - `refactor` → `♻️`
  - `test`   → `🧪`
  - `docs`   → `📝`
  - `style`  → `🎨`
  - `chore`  → `🔧`
  - `delopy` → `🚀`  (배포, deploy 오타 그대로 사용)
  - `revert` → `⏪`

- 위 목록에 없는 타입이면:
  - 에러 메시지 출력:
    - `⛔ 올바르지 않은 커밋 타입입니다.`
    - `사용 가능한 타입: feat, fix, perf, refactor, test, docs, style, chore, delopy, revert`
  - `exit 1` 로 커밋 실패.

#### 2.2.3 실제 메시지 변환 방식

- 입력 예시:
  - `feat: 게시글 좋아요 기능 추가`
  - `FIX: 로그인 실패 예외 처리 수정`
- 훅 내부 동작:
  - 타입(`feat`, `FIX`)만 추출 → 소문자로 정규화(`feat`, `fix`).
  - 이모지 매핑 후, 첫 단어를 `이모지 + 공백 + 소문자 타입`으로 치환:
    - `feat: ...` → `✨ feat: ...`
    - `FIX: ...` → `🐛 fix: ...`
- 나머지 줄(커밋 본문)은 그대로 유지됩니다.

---

## 3. `pre-push` (테스트 + 커버리지 검증)

- 파일: `.husky/pre-push`
- 역할:
  - 푸시 전에 **전체 테스트 + JaCoCo 커버리지 검증**을 실행.
  - 하나라도 실패하면 **푸시를 차단**.

### 3.1 동작 내용

- 실행 명령:
  - `./gradlew test jacocoTestCoverageVerification`
- 성공/실패 시 메시지:
  - 시작:
    - `🐶 Husky pre-push 훅이 실행됩니다.`
    - `✅ 전체 테스트 및 커버리지 검증을 수행합니다...`
  - 실패:
    - `❌ pre-push 훅 실패: 테스트 또는 커버리지 검증에 실패했습니다.`
    - `- 실패한 테스트를 수정하거나,`
    - `- 커버리지 기준을 만족한 뒤 다시 푸시해주세요.`
  - 성공:
    - `✅ pre-push 훅 통과: 테스트와 커버리지가 기준을 만족합니다.`

### 3.2 Gradle 테스트/커버리지 설정 (요약)

- 파일: `build.gradle`

- 병렬 테스트 실행:
  - `tasks.named('test')`, `unitTest`, `integrationTest` 모두에서:
    - `useJUnitPlatform()`
    - `maxParallelForks` 를 CPU 코어 수 기준으로 설정:
      - `cores = Runtime.runtime.availableProcessors()`
      - `maxParallelForks = cores > 1 ? cores.intdiv(2) : 1`

- JaCoCo 설정:
  - `jacoco` 플러그인 사용 (`toolVersion = "0.8.12"`).
  - `jacocoTestReport`:
    - `test` 에 의존 (`dependsOn test`)
    - XML/HTML 리포트 생성, 특정 클래스(예: `Q*`, `*Application`, `*Config`, `dto`, `exception`)는 커버리지 제외.
  - `jacocoTestCoverageVerification`:
    - 동일한 exclude 규칙 적용.
    - `violationRules`:
      - 최소 커버리지 기준(현재는 예시로 0.50 또는 0.70 등 프로젝트 설정에 따름).
    - 기준을 만족하지 못하면 task 실패 → `pre-push` 훅도 실패 → `git push` 차단.

---

## 4. 팀에서 따라야 할 Husky 컨벤션 요약

1. **커밋 메시지 포맷**
   - 첫 줄: `타입(소문자) + 콜론 + 한글/영문 설명`
     - 예) `feat: 댓글 작성 API 추가`, `fix: 로그인 버그 수정`
   - 허용 타입: `feat`, `fix`, `perf`, `refactor`, `test`, `docs`, `style`, `chore`, `delopy`, `revert`
   - 커밋 시 자동으로 `이모지 + 타입` 으로 변환됩니다.

2. **푸시 전 검증**
   - `git push` 시:
     - `./gradlew test jacocoTestCoverageVerification` 이 실행됩니다.
     - 테스트 실패 또는 커버리지 기준 미달이면 푸시가 거절됩니다.

3. **환경 가정**
   - Node.js + npm 설치 (Husky 사용을 위해).
   - Git으로 버전 관리 중.
   - macOS / Linux / Windows(Git Bash 또는 WSL)에서 모두 동작.

4. **훅 우회 (임시)**
   - 정말 필요할 때만 사용:
     - `HUSKY=0 git commit ...`
     - `HUSKY=0 git push ...`
   - 기본 원칙은 **훅을 활성화한 상태에서 작업**하는 것을 권장합니다.

이 문서 하나만 보면, 현재 프로젝트에 적용된 Husky 훅과 커밋/테스트 컨벤션을 한 번에 이해할 수 있습니다.
