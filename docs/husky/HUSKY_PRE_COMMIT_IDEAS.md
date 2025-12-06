## Husky `pre-commit` 훅에서 할 수 있는 일들

> 현재 이 레포의 `pre-commit` 은 안내 메시지만 출력하고 있습니다.  
> 이 문서는 앞으로 `pre-commit` 훅에 추가할 수 있는 기능들을 아이디어 형태로 정리한 것입니다.

---

## 1. 코드 포매터 자동 실행

- **목표**: 스타일 관련 커밋을 줄이고, 팀 컨벤션을 자동으로 맞추기.
- 예시:
  - Java 포매터:
    - Gradle `spotless` 플러그인 사용 시: `./gradlew spotlessApply`
  - JavaScript/TypeScript 가 있다면:
    - `npx prettier --write .`
- 훅 예시 (아이디어):
  ```sh
  #!/usr/bin/env sh

  echo "코드 포매터를 실행합니다..."
  ./gradlew spotlessApply || exit 1
  ```

---

## 2. Lint / 정적 분석

- **목표**: 컴파일 이전에 기본적인 버그/코드 스멜 발견.
- 예시:
  - Java:
    - `./gradlew check` (Checkstyle/PMD/SpotBugs 등이 구성되어 있다면)
  - JS/TS:
    - `npx eslint .`
- 훅 예시 (아이디어):
  ```sh
  echo "정적 분석(Lint)을 실행합니다..."
  ./gradlew check || exit 1
  ```

---

## 3. 빠른 단위 테스트 (`unitTest`)만 실행

- **목표**: `pre-push` 에서 전체 테스트를 돌리기 전에,  
  빠른 단위 테스트만 `pre-commit` 에서 먼저 걸러내기.
- 현재 Gradle에 이미 `unitTest` 태스크가 정의되어 있음:
  - `includeTags 'unit'` 을 사용하는 JUnit 태그 기반 테스트.
- 훅 예시 (아이디어):
  ```sh
  echo "단위 테스트(unitTest)를 실행합니다..."
  ./gradlew unitTest || exit 1
  ```

---

## 4. 변경된 파일만 대상 포매터/린트 실행

- **목표**: 커밋에 포함된 파일만 대상으로 검사해서 속도 향상.
- 아이디어:
  - `git diff --cached --name-only` 으로 **staged 파일 목록**만 가져오기.
  - 확장자 필터링 후, 해당 파일에만 포매터/린터 적용.
- 간단한 패턴 예시:
  ```sh
  CHANGED_FILES=$(git diff --cached --name-only -- '*.java')

  if [ -n "$CHANGED_FILES" ]; then
    echo "변경된 Java 파일에 대해서만 포매터/체크 실행..."
    # 예: ./gradlew spotlessApply --files "$CHANGED_FILES"
  fi
  ```

---

## 5. TODO/FIXME 남기지 않기

- **목표**: 중요한 TODO/FIXME 주석이 실제 배포 코드에 남지 않도록 방지.
- 아이디어:
  - 커밋되는 파일에 `TODO`, `FIXME` 문자열이 있는지 검사.
  - 특정 디렉터리(예: `src/main`)만 대상으로 제한 가능.
- 예시:
  ```sh
  echo "커밋 대상에 TODO/FIXME 가 남아있는지 검사합니다..."

  if git diff --cached --name-only -- 'src/main/**' | xargs grep -nE 'TODO|FIXME' >/dev/null 2>&1; then
    echo "⛔ TODO/FIXME 주석이 남아 있습니다. 정리 후 다시 커밋해주세요."
    exit 1
  fi
  ```

---

## 6. 민감 정보(Secret) 유출 방지

- **목표**: 액세스 키, 비밀번호, 토큰 등이 실수로 커밋되는 것을 방지.
- 간단 버전:
  - 정규식으로 `API_KEY`, `SECRET`, `PASSWORD` 등 키워드 탐지.
- 고급 버전:
  - `git-secrets`, `trufflehog` 같은 전용 도구 연동.
- 예시 (간단 키워드 검출 아이디어):
  ```sh
  echo "민감한 키워드(API_KEY, SECRET 등) 유출 여부를 검사합니다..."

  if git diff --cached | grep -E 'API_KEY|SECRET|PASSWORD' >/dev/null 2>&1; then
    echo "⛔ 민감 정보로 의심되는 문자열이 감지되었습니다."
    echo "   커밋 전에 관련 내용을 제거하거나 환경 변수/설정 파일로 분리해주세요."
    exit 1
  fi
  ```

---

## 7. 브랜치 이름 검사 (선택)

- **목표**: 브랜치 네이밍 컨벤션 강제 (예: `feature/`, `bugfix/` 등).
- 실제로는 `pre-commit` 보다는 `prepare-commit-msg` 또는 CI에서 하는 경우가 많지만,  
  commit 전에 브랜치 이름을 확인하는 것도 가능.
- 예시 아이디어:
  ```sh
  BRANCH_NAME=$(git rev-parse --abbrev-ref HEAD)

  case "$BRANCH_NAME" in
    feature/*|bugfix/*|hotfix/*)
      # 허용 패턴
      ;;
    *)
      echo "⛔ 브랜치 이름 컨벤션을 지켜주세요."
      echo "   예) feature/..., bugfix/..., hotfix/..."
      exit 1
      ;;
  esac
  ```

---

## 8. 실제 적용 시 추천 조합 (예시)

프로젝트 운영 스타일에 따라 다르겠지만, 예를 들면:

1. `pre-commit`
   - 변경된 파일 기준 포매터 실행
   - TODO/FIXME 검사
   - 빠른 단위 테스트(`unitTest`) 실행
2. `prepare-commit-msg`
   - 현재처럼 커밋 메시지 타입 + 이모지 강제
3. `pre-push`
   - 전체 테스트 + JaCoCo 커버리지 검증 (현재 설정 유지)

이 문서는 어디까지나 “할 수 있는 일”을 모아둔 아이디어 정리이며,  
실제 어떤 기능을 `pre-commit` 에 넣을지는 팀 속도와 개발 경험을 고려해 선택하면 됩니다.

