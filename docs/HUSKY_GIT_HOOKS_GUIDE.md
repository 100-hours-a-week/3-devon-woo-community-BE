## Husky를 통한 Git Hook 도입 가이드 (Mac/Windows)

> 이 문서는 **Husky 기반 Git commit/push hook 도입을 위한 가이드**입니다.  
> 실제 설정·설치는 직접 진행하면 되며, 이 문서는 그 전체 흐름과 명령어를 정리합니다.

---

## 1. 사전 조건 확인

- Node.js 및 npm (또는 yarn, pnpm) 설치
  - Mac
    - `brew install node` (Homebrew 사용 시)
  - Windows
    - https://nodejs.org 에서 LTS 버전 설치
- Git이 이미 설치된 상태 (이 레포는 Git 사용 중이라고 가정)

레포 루트(예: `3-devon-woo-community-BE`) 기준으로 이후 명령을 실행합니다.

```bash
cd /path/to/3-devon-woo-community-BE
```

---

## 2. Node 프로젝트 초기화 (없는 경우에만)

이미 `package.json` 이 있다면 이 단계는 건너뜁니다.

```bash
npm init -y
```

또는 yarn/pnpm:

```bash
yarn init -y
# 또는
pnpm init
```

---

## 3. Husky 설치

### 3.1 Husky 패키지 추가 (devDependency)

```bash
npm install husky --save-dev
```

또는

```bash
yarn add husky --dev
# 또는
pnpm add husky -D
```

### 3.2 Husky 활성화 (Git hook 디렉터리 생성)

```bash
npx husky init
```

위 명령은 다음과 같은 작업을 수행합니다.

- 프로젝트 루트에 `.husky/` 디렉터리를 생성
- 기본 `pre-commit` hook 파일을 추가
- `package.json` 에 `"prepare": "husky"` 스크립트 추가 (없다면)

Mac/Windows 모두 동일하게 동작합니다.

> 만약 `npx husky init` 이 동작하지 않으면:
> ```bash
> npx husky install
> ```
> 을 먼저 실행하고, `.husky` 안에 hook 파일을 수동으로 추가해도 됩니다.

---

## 4. Git Hook 개념 정리

- `pre-commit`: `git commit` 실행 직전에 동작.  
  예: 코드 포매터, 린트, 테스트 일부 실행.
- `pre-push`: `git push` 직전에 동작.  
  예: 테스트 전체 실행, 빌드 검증 등.

Husky는 `.husky/<hook-name>` 파일을 통해 위 훅들을 관리합니다.

---

## 5. Pre-commit Hook 설정 예시

### 5.1 기본 pre-commit 파일 구조

`npx husky init` 으로 생성된 `.husky/pre-commit` 파일을 열면 대략 다음과 유사합니다.

```bash
#!/usr/bin/env sh
. "$(dirname -- "$0")/_/husky.sh"

echo "pre-commit hook"
```

여기에 원하는 명령을 추가합니다.

### 5.2 예시: 포매터 + 린트 실행

아래는 예시일 뿐이며, 실제로 사용할 도구는 이 프로젝트 상황에 맞게 교체합니다.

```bash
#!/usr/bin/env sh
. "$(dirname -- "$0")/_/husky.sh"

echo "Running pre-commit checks..."

# 예시: Java 코드 스타일 검사 (Gradle)
./gradlew spotlessCheck || exit 1

# 예시: 기타 정적 분석이나 포매터 (Node 기반 예시)
# npx eslint . || exit 1
```

Mac/Windows 공통:

- Git Bash, WSL, 또는 Shell 환경이 있는 경우 위 스크립트 그대로 동작.
- Windows에서 `./gradlew` 실행이 안 되는 경우:
  - `gradlew.bat` 를 직접 호출하는 방식으로 수정 가능.

```bash
# Windows 전용 예시 (gradlew.bat 사용)
if command -v ./gradlew >/dev/null 2>&1; then
  ./gradlew spotlessCheck || exit 1
else
  gradlew.bat spotlessCheck || exit 1
fi
```

---

## 6. Pre-push Hook 설정 예시

### 6.1 pre-push 파일 생성

```bash
npx husky add .husky/pre-push "echo \"pre-push hook\""
```

위 명령으로 `.husky/pre-push` 파일이 생성됩니다. 이후 내용을 수정합니다.

### 6.2 예시: 테스트 및 빌드 실행

```bash
#!/usr/bin/env sh
. "$(dirname -- "$0")/_/husky.sh"

echo "Running pre-push checks..."

# 예시: 기본 테스트 실행
./gradlew test || exit 1

# 필요하다면 빌드까지
# ./gradlew build || exit 1
```

Windows 환경에서 `gradlew.bat` 를 사용하고 싶다면, pre-commit 예시와 동일하게 조건 분기 코드를 사용할 수 있습니다.

---

## 7. Mac/Windows 호환 팁

- **라인 엔딩(EOL)**  
  - Git 설정에서 `core.autocrlf` 를 적절히 설정 (`true`/`input` 등)하여 CRLF/LF 문제를 최소화합니다.
  - Husky 스크립트 파일은 가능하면 LF 사용을 권장.
- **실행 권한**  
  - Mac/Linux: `chmod +x .husky/*` 로 실행 권한을 부여해야 할 수 있습니다.
  - Windows: Git Bash/WSL 사용 시 동일하게 적용되지만, 일반 CMD/PowerShell만 사용하는 경우에는 `.sh` 스크립트 대신 `*.cmd` 또는 `*.bat` 파일을 별도로 만들어 호출하는 전략도 고려할 수 있습니다.
- **경로 문제**  
  - 스크립트 내부에서 경로를 사용할 때, 레포 루트를 기준으로 상대 경로를 사용하는 것이 안전합니다.

---

## 8. 팀 공유 및 문서화 포인트

- 이 문서(`docs/HUSKY_GIT_HOOKS_GUIDE.md`)를 팀 공용 레퍼런스로 사용합니다.
- 실제로 Husky를 도입한 후에는:
  - `.husky/` 폴더와 `package.json` 변경사항을 Git에 커밋.
  - 팀원들은 `npm install` 또는 `yarn install` 후, 자동으로 Husky가 동작하는지 확인.
- 프로젝트 컨벤션:
  - pre-commit에서 어떤 검사를 할지 (예: 포매터/린트/간단 테스트)
  - pre-push에서 어떤 검사를 할지 (예: 전체 테스트/빌드)
  를 추가로 정리해 `docs` 아래 별도 문서나 이 문서에 하위 섹션으로 확장할 수 있습니다.

---

## 9. 실제 적용 시 요약 체크리스트

1. Node.js/npm 설치 여부 확인 (Mac/Windows)
2. 레포 루트에서 `npm init -y` (필요 시)
3. `npm install husky --save-dev`
4. `npx husky init`
5. `.husky/pre-commit` 수정 → 원하는 검사 명령 추가
6. `npx husky add .husky/pre-push "<기본 커맨드>"` 후 `.husky/pre-push` 수정
7. Mac/Linux라면 `.husky/*` 에 실행 권한 확인 (`chmod +x .husky/*`)
8. 변경 사항 커밋 후, 실제로 `git commit` / `git push` 시 훅이 잘 동작하는지 확인

위 순서를 그대로 따라가면 Mac/Windows 모두에서 Husky 기반 Git commit/push hook을 무리 없이 도입할 수 있습니다.

