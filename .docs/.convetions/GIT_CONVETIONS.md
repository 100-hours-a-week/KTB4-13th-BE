# Git Convention

## 1. Purpose

Define the roles of Issues, branches, commits, and pull requests, and how they connect.
Requirements start in Issues, implementation is recorded in branches and commits, and review and integration take place through pull requests.

Follow the project skills for [Issues](../../.agents/skills/github-issue/SKILL.md), [commits](../../.agents/skills/github-commit/SKILL.md), and [pull requests](../../.agents/skills/github-pr/SKILL.md).
Use [the label configuration](../../.github/labels.yml) as the local reference and check the target repository's actual labels before applying them. Write Issue and PR content and manually authored commit summaries in Korean unless the user requests another language.

## 2. Basic Workflow

```text
Issue
  -> Branch
  -> Commit
  -> Pull Request
  -> Review
  -> Merge
  -> Issue closed
```

- Give each Issue one purpose and clear acceptance criteria.
- Keep each branch and pull request focused on one Issue whenever possible.
- Include only one logical change in each commit.
- Include the Issue number in the branch name and pull request to preserve the connection between the Issue, branch, commits, and pull request.
- Confirm the Issue as the implementation target before creating a branch linked to it.

## 3. Issues

An Issue defines a requirement, bug, or documentation task to implement.
Clarify the problem and acceptance criteria before changing code.

### 3.1 Required Issue Content

- Title: State the task type and main purpose briefly.
- Background: Explain why the work is needed.
- Current behavior: Describe the current state for bugs or improvements.
- Expected behavior: Describe the outcome users can observe.
- Scope: State what this Issue will change.
- Out of scope: State what this Issue will not change.
- Acceptance criteria: Define criteria that can be verified through tests or observable results.
- Dependencies: Link any prerequisite Issues or decisions.

### 3.2 Issue Titles

Start the title with one of the following types.

| Type | Purpose |
| --- | --- |
| `feat` | Add a feature or user behavior |
| `fix` | Fix a bug |
| `refactor` | Improve structure without changing behavior |
| `test` | Add or update tests |
| `docs` | Add or update documentation |
| `chore` | Change build configuration, settings, or tooling |

Examples:

```text
feat: 예약 충돌 응답 추가
fix: 만료된 토큰 재발급 차단
docs: Git 컨벤션 문서화
```

When triage is requested, use the following roles only if the corresponding labels exist in the target repository:

| Role | Meaning |
| --- | --- |
| `needs-triage` | Awaiting initial assessment |
| `needs-info` | Awaiting information needed to proceed |
| `ready-for-agent` | Scope and acceptance criteria are ready for agent implementation |
| `ready-for-human` | Requires human implementation |
| `wontfix` | Will not be implemented |

These roles are not provisioned by this document. Follow the Issue skill for missing labels and authorization to create labels or change triage status.

### 3.3 Linking Issues and Branches

Creating an Issue and creating a branch are separate steps. Creating an Issue does not automatically create a branch.

- Link a branch to an Issue to indicate that work is in progress.
- Follow [4.1 Branch Names](#41-branch-names) when naming branches.
- Follow the [Issue skill](../../.agents/skills/github-issue/SKILL.md) for Issue and branch procedures. Creating an Issue alone does not authorize branch creation, commits, or pushes.

## 4. Branches

A branch provides an isolated workspace for implementing and verifying one Issue.
Use `main` as the default branch. Do not work on it or push to it directly.

### 4.1 Branch Names

```text
<type>/<issue-number>-<kebab-case-summary>
```

Use the same types as Issues: `feat`, `fix`, `refactor`, `test`, `docs`, and `chore`.

Examples:

```text
feat/123-reservation-conflict
fix/124-refresh-token
docs/125-git-convention
```

- Do not omit the Issue number.
- Write a short English summary in lowercase kebab-case.
- Do not mix unrelated tasks in one branch.
- Check the latest state of `main` before creating a branch.

## 5. Commits

A commit is the smallest unit that explains the change history.
Each commit must contain one logical change that can be understood and reverted on its own.

### 5.1 Commit Messages

```text
<type>: <short summary>(#<issue-number>)
```

Use one of `feat`, `fix`, `refactor`, `test`, `docs`, or `chore` as the type.
Write a short, imperative, action-oriented summary without a trailing period.
Korean summaries are allowed, but must describe the change clearly.

Examples:

```text
feat: 예약 생성 API 추가(#123)
fix: 만료된 토큰 재발급 차단(#124)
refactor: 컨트롤러 입력 변환 경계 확대(#125)
test: 예약 충돌 응답 검증 추가(#126)
docs: Git 컨벤션 문서 추가(#127)
```

### 5.2 Commit Rules

- Do not use messages such as `WIP` or meaningless `update` and `change`.
- Do not combine changes with different purposes in one commit.
- Do not combine behavior changes with large formatting changes in one commit.
- Include relevant tests alongside changes that require testing.
- Do not commit secrets, personal settings, or build outputs.
- Check the relevant verification results after committing.

## 6. Pull Requests

A pull request is the unit of review for integrating a branch into `main`.
Follow the linked Issue's scope and acceptance criteria rather than introducing new requirements in the PR.

### 6.1 PR Titles

Use the same types as commit messages.

```text
<type>: <short summary>
```

Examples:

```text
feat: 예약 충돌 응답 추가
docs: Git 컨벤션 문서화
```

### 6.2 PR Body

Use [`.github/PULL_REQUEST_TEMPLATE.md`](../../.github/PULL_REQUEST_TEMPLATE.md) as the single source for PR body sections and instructions. Fill it in Korean and record actual verification results, including checks that failed or were not run. Omit optional sections when they do not apply.

Use an Issue-closing keyword such as `Closes #<issue-number>` or `Fixes #<issue-number>` in the PR body. These keywords close the Issue after merge when the PR targets the repository's default branch. Linking a branch to an Issue alone does not close it automatically.

### 6.3 Before Opening a PR

- Are all acceptance criteria of the linked Issue satisfied?
- Does the change scope match the Issue?
- Have the relevant tests and checks been run?
- Have observable results such as API behavior, persisted state, permissions, and transactions been verified?
- Are secrets and unnecessary files excluded?
- Do the documentation and code agree?
- Can the reviewer understand the reason for the change and how it was verified?

### 6.4 Review and Merge

- Keep PRs small enough to review; a maximum of 400 changed lines is recommended.
- Do not merge while review comments remain unresolved or required checks are failing.
- Update the relevant files and verification results when addressing review feedback.
- Link the Issue in the PR body so it can be closed after merge.
- Commit, push, create PRs, and merge only within the user's explicit authorization. A request to perform the action is authorization for that scope; do not ask again for an already authorized action. Drafting alone does not authorize publication.

## 7. Change Scope Principles

- Change only the minimum files needed to meet the requirements.
- Do not include unrelated refactoring, bulk formatting, or additional dependencies.
- Record decisions that are difficult to reverse in documentation or an ADR before implementation.
- When existing rules conflict, identify the differences and affected scope before overwriting them.
