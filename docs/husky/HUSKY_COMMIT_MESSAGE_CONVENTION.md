## Husky 기반 커밋 메시지 규칙 & `prepare-commit-msg` 훅 가이드

> 이 문서는 **Husky의 `prepare-commit-msg` 훅을 사용해 커밋 메시지 포맷을 강제하는 방법**을 정리한 가이드입니다.  
> 실제 훅 파일 생성/수정은 직접 진행하면 되며, 여기서는 절차와 예시 코드를 문서화합니다.

---

## 1. 목표

- 커밋 메시지 첫 줄에 **정해진 소문자 타입 + 이모지** 포맷을 강제.
- 사용 가능한 타입과 이모지:
  - `✨ feat`
  - `🐛 fix`
  - `⚡ perf`
  - `♻️ refactor`
  - `🧪 test`
  - `📝 docs`
  - `🎨 style`
  - `🔧 chore`
  - `🚀 delopy`
  - `⏪ revert`   

- `Merge`, `Revert` 등 Git 기본 메시지는 예외 처리.
- Mac/Windows 공통으로 동작 (Git Bash / WSL / 유닉스 셸 가정).

---

## 2. 준비 사항

이 문서는 이미 다음이 완료되었다고 가정합니다.

- Husky 설치 및 초기화
  - `npm install husky --save-dev`
  - `npx husky init`
- 레포 루트에 `.husky` 디렉터리가 존재.

자세한 Husky 기본 설정은 `docs/HUSKY_GIT_HOOKS_GUIDE.md` 를 참고합니다.

---

## 3. `prepare-commit-msg` 훅 파일 생성

Husky v9 기준으로 `npx husky add` 명령은 **deprecated** 입니다.  
따라서 훅 파일을 **직접 생성**하는 방식을 사용합니다.

레포 루트에서:

```bash
mkdir -p .husky
touch .husky/prepare-commit-msg
```

이후 `.husky/prepare-commit-msg` 파일 내용을 **아래 예시 스크립트**로 교체합니다.

> 이미 파일이 있다면 `touch` 는 생략하고, 내용만 수정하면 됩니다.

---

## 4. 커밋 메시지 포맷 강제 스크립트 예시

아래는 `prepare-commit-msg` 훅에 넣을 예시 코드입니다.  
타입은 내부적으로 **소문자**로 정규화하여 처리합니다.

```bash
#!/usr/bin/env bash
. "$(dirname -- "$0")/_/husky.sh"

COMMIT_MESSAGE_FILE_PATH=$1
first_line=$(head -n1 "$COMMIT_MESSAGE_FILE_PATH")
remaining_lines=$(tail -n +2 "$COMMIT_MESSAGE_FILE_PATH")

# Git이 자동 생성하는 메시지는 예외 처리
if [[ $first_line =~ ^(Merge|Revert|Amend|Reset|Rebase|Tag) ]]; then
  exit 0
fi

type=$(echo "$first_line" | grep -o "^[A-Za-z]*")
normalized_type="$(echo "$type" | tr 'A-Z' 'a-z')"

emoji=""
case "$normalized_type" in
  feat)   emoji="✨" ;;
  fix)    emoji="🐛" ;;
  perf)   emoji="⚡" ;;
  refactor) emoji="♻️" ;;
  test)   emoji="🧪" ;;
  docs)   emoji="📝" ;;
  style)  emoji="🎨" ;;
  chore)  emoji="🔧" ;;
  hotfix) emoji="🚑" ;;
  build)  emoji="🏗️" ;;
  infra)  emoji="🏭" ;;
  env)    emoji="🌱" ;;
  *)      emoji="" ;;
esac

# 유효하지 않은 타입이면 커밋을 막고 가이드를 출력
if [[ -z "$emoji" || -z "$normalized_type" ]]; then
  echo "⛔ 올바르지 않은 커밋 타입입니다."
  echo "   사용 가능한 타입:"
  echo "     feat, fix, perf, refactor, test, docs, style, chore, delopy, revert"
  echo "   예) feat: 기능 추가 설명"
  exit 1
fi

# 첫 번째 단어를 '이모지 + 타입(소문자)' 로 변환
first_line=$(echo "$first_line" | sed "s/^$type/$emoji $normalized_type/")

# (선택) 브랜치 이름이 필요하면 아래 변수 사용 가능
branch_name=$(git rev-parse --abbrev-ref HEAD)
# 현재 예시는 branch_name 은 사용하지 않고, 필요시 규칙 확장에 활용.

echo "$first_line" > "$COMMIT_MESSAGE_FILE_PATH"
echo "$remaining_lines" >> "$COMMIT_MESSAGE_FILE_PATH"
```

---

## 5. 커밋 메시지 작성 규칙

- 첫 줄은 반드시 **소문자 타입 + 콜론 + 설명** 형식으로 작성합니다.
  - 예:  
    - `feat: 게시글 좋아요 기능 추가`  
    - `fix: 로그인 실패 예외 처리 수정`  
    - `docs: Husky 가이드 문서 추가`
- 실제 커밋 메시지 저장 시, 훅이 자동으로 다음과 같이 바꿉니다.
  - 입력: `feat: 게시글 좋아요 기능 추가`  
    - 저장: `✨ feat: 게시글 좋아요 기능 추가`
  - 입력: `FIX: 로그인 실패 예외 처리 수정` (대문자로 입력해도)
    - 저장: `🐛 fix: 로그인 실패 예외 처리 수정` (소문자로 정규화)
- 허용되지 않은 타입으로 시작하면 커밋이 취소되고, 터미널에 안내 문구가 출력됩니다.

---

## 6. Mac / Windows 환경 참고 사항

- 스크립트는 `bash` 를 사용하므로:
  - Mac: 기본 bash/zsh 환경에서 그대로 동작.
  - Windows: Git Bash 또는 WSL 환경 사용을 권장.
- 줄 끝(EOL) 문제 방지를 위해:
  - `.husky/prepare-commit-msg` 파일은 LF(Line Feed) 사용 권장.
- 실행 권한:
  - Mac/Linux:  
    ```bash
    chmod +x .husky/prepare-commit-msg
    ```
  - Windows의 Git Bash/WSL에서도 동일 명령 사용 가능.

---

## 7. 실제 적용 시 체크리스트

1. `docs/HUSKY_GIT_HOOKS_GUIDE.md` 에 따라 Husky 기본 설정 완료.
2. 레포 루트에서 `npx husky add .husky/prepare-commit-msg ""` 실행.
3. `.husky/prepare-commit-msg` 내용 → 본 문서 4번의 스크립트로 교체.
4. (필요 시) `chmod +x .husky/prepare-commit-msg` 로 실행 권한 부여.
5. `git commit` 테스트:
   - 허용된 타입으로 시작하면 이모지 + 타입으로 자동 변환.
   - 허용되지 않은 타입이면 커밋이 거절되는지 확인.

위 과정을 따르면, Husky `prepare-commit-msg` 훅을 통해 **커밋 메시지 포맷을 일관되게 강제**할 수 있습니다.
