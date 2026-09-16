---
name: github-pr
description: 현재 브랜치와 연결된 Issue를 확인해 이 프로젝트의 Git 규칙에 맞는 GitHub PR을 작성하거나 생성·수정한다. PR 초안, 등록, 설명 갱신 요청에 사용한다.
---

# GitHub Pull Request

## 범위 확인

저장소 루트의 [Git 규칙](../../../.docs/.convetions/GIT_CONVETIONS.md)과
`.github/PULL_REQUEST_TEMPLATE.md`를 읽는다.
`git remote -v`, 현재 브랜치, `git status --short`, 기존 PR을 확인한다.
base는 규칙의 `main`으로 두고 최신 원격 참조를 확보한 후
`git log origin/main..HEAD`, `git diff origin/main...HEAD`로 PR 전체 변경을 파악한다.

Issue 번호는 사용자 입력과 브랜치에서 확인한다. 누락·불일치는 질문하고 실제 Issue를 읽어 완료 조건과 범위를 확인한다.
커밋되지 않은 변경은 PR에 포함된 것으로 설명하지 않는다. 현재 브랜치가 `main`이면 PR용 작업 브랜치를 먼저 정한다.

## 작성과 등록

1. 제목은 `<type>: <한국어 요약>`으로 작성한다. 커밋과 달리 `(#이슈번호)`를 제목에 붙이지 않는다.
2. PR 본문의 기준은 `.github/PULL_REQUEST_TEMPLATE.md`다. 문제와 변경 후 동작을 요점부터 설명하고, Related Issue에는 실제 Issue를 닫는 `Closes #번호` 또는 `Fixes #번호`를 쓴다. Verification에는 검증 결과를, Out of Scope에는 범위 외 항목을 적는다. Sub-issue Progress와 Notes는 해당할 때만 채운다.
3. 실행한 검증과 미실행·실패 항목을 구분한다. Issue 완료 조건과 변경 범위를 대조하고, 400줄을 크게 넘으면 독립적으로 분리할 수 있는지 검토한다. 줄 수 권장을 절대 제한으로 취급하지 않는다.
4. 비밀값과 로컬 `.docs/` 원문을 게시하지 않는다. PR에 들어갈 모든 커밋의 첫 줄이 `.github/workflows/git-conventions.yml`의 규칙을 충족하는지 확인한다. 규칙에 맞지 않는 이력을 승인 없이 재작성하지 않는다.
5. 명시적인 PR 생성·수정 요청이면 실행한다. 초안 요청이면 제목과 본문을 완성해 반환한다. Push는 세션에서 승인된 경우에만 수행하며, 원격 브랜치가 없고 Push 승인이 없다면 완성한 PR 초안과 대상 브랜치를 제시한 뒤 승인받는다. 이미 승인된 작업은 다시 묻지 않는다.
6. 기존 PR이 있으면 새로 중복 생성하지 않는다. CLI 본문은 임시 UTF-8 파일과 `gh pr create --body-file` 또는 `gh pr edit --body-file`을 사용한다. 일반 Push만 사용하고 강제 Push·Merge·자동 Merge 활성화는 별도 명시적 요청이 있을 때만 수행한다.

## 완료와 후속 확인

PR URL, base/head 브랜치, 연결 Issue, 검증 결과를 보고한다.
로컬 테스트 성공과 GitHub 상태 검사 성공을 구분한다. 제목·커밋 검사 통과만으로 코드 검증이나 Merge 승인을 대신하지 않는다.

Squash Merge까지 요청받았다면 최종 커밋 제목에도 실제 Issue 번호를 붙인다.
GitHub가 자동으로 붙이는 PR 번호와 Issue 번호는 다를 수 있으며, PR 검사에서는 이후 생성될 Squash 커밋 메시지까지 검증하지 않는다.
