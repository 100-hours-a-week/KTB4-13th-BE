# 회원 도메인 규칙

이 문서는 서비스 내부 회원과 외부 소셜 계정 연결의 정책, 생명주기와 로그인 시 회원 식별 흐름을 기록합니다.
Kakao 토큰 검증과 서비스 토큰 발급·인가는 [인증 도메인 규칙](AUTH_RULE.md)을 기준으로 합니다.

## 1. 범위와 책임

- `User`는 서비스 내부 회원을 표현하고 닉네임과 회원 활성 상태를 관리합니다.
- `UserProvider`는 내부 회원과 외부 소셜 계정의 연결을 표현합니다.
- `User`와 `UserProvider`는 서로 다른 Domain 객체이며 각각 `users`, `user_providers` 테이블에 저장합니다.
- 현재 지원하는 `ProviderType`은 `KAKAO`입니다.
- Kakao 외부 인증, 서비스 JWT 발급·검증, Refresh Session 관리는 인증 도메인의 책임이며 회원 도메인이 소유하지 않습니다.

## 2. 회원과 소셜 계정 연결

### 2.1 내부 회원

- 신규 회원은 ID 없이 생성하고 저장 후 MySQL의 `IDENTITY` 값으로 ID를 부여받습니다.
- 닉네임은 앞뒤 공백을 제거한 뒤 2자 이상 20자 이하인지 검증합니다.
- 신규 회원의 닉네임은 서버의 `NicknameGenerator`가 한글 형용사·명사와 네 자리 숫자를 조합해 생성합니다.
- `deletedAt`이 `null`이면 활성 회원이고 값이 있으면 비활성 회원입니다.
- 현재 회원 탈퇴·복구 기능은 구현하지 않습니다.

### 2.2 외부 소셜 계정 연결

- `UserProvider`는 `userId`, `providerType`, `providerUserId`, 선택적 `providerEmail`을 가집니다.
- `providerUserId`는 외부 provider가 보장한 사용자 식별자이며 1자 이상 255자 이하입니다.
- `providerEmail`은 `null`을 허용하고 최대 254자이며 회원 식별 기준으로 사용하지 않습니다.
- `deletedAt`이 `null`이면 활성 연결이고 값이 있으면 비활성 연결입니다.
- `UserProvider`는 `User` 객체의 JPA 연관관계를 가지지 않고 `userId`로 내부 회원을 참조합니다. DB 외래 키가 참조 무결성을 보장합니다.

## 3. 사용자 식별과 신규 회원 생성

활성 소셜 계정 연결은 `providerType + providerUserId`로 식별합니다.
Kakao ID Token의 `sub`가 `providerUserId`로 전달되며 이메일은 식별에 사용하지 않습니다.
이 식별과 신규 등록 조율은 `IdentifyUserUseCase`가 담당합니다.

```text
회원 식별(providerType, providerUserId, providerEmail):
  1. 같은 providerType과 providerUserId를 가진 활성 UserProvider를 조회한다.
  2. 활성 UserProvider가 있으면:
       a. userId로 내부 User를 조회한다.
       b. User가 없거나 비활성 상태이면 내부 데이터 정합성 오류로 처리한다.
       c. 활성 User의 userId를 반환한다.
  3. 활성 UserProvider가 없으면:
       a. 서버에서 닉네임 후보를 생성한다.
       b. UserRegistrationUseCase가 User와 UserProvider를 하나의 트랜잭션에서 저장한다.
       c. 활성 닉네임이 충돌하면 새 닉네임을 생성해 새로운 트랜잭션으로 다시 시도하며 최대 5회까지 재시도한다.
       d. provider identity가 충돌하면 재시도하지 않고 충돌 오류로 처리한다.
       e. 닉네임 충돌이 5회를 초과하면 서버 오류로 종료한다.
```

## 4. 활성 데이터 중복 규칙

- 활성 회원의 닉네임은 중복될 수 없습니다.
- 활성 소셜 계정 연결의 `providerType + providerUserId` 조합은 중복될 수 없습니다.
- MySQL의 `active_flag`는 `deleted_at IS NULL`이면 `1`, 그 외에는 `NULL`인 generated column입니다.
- `active_flag`는 Domain이나 Entity가 생성·변경하는 상태값이 아닙니다.
- 활성 행만 UNIQUE 제약 대상이므로 soft delete 후에는 같은 닉네임과 provider identity를 다시 사용할 수 있습니다.
- `created_at`, `updated_at`은 MySQL이 생성·갱신하며 Domain이 직접 관리하지 않습니다.

## 5. 트랜잭션과 충돌 처리

- 신규 `User` 저장과 신규 `UserProvider` 저장은 `UserRegistrationUseCase`의 한 트랜잭션에서 처리합니다.
- `UserProvider` 저장이 실패하면 같은 시도에서 저장한 `User`도 rollback합니다.
- 닉네임 충돌 후 재시도는 실패한 트랜잭션 밖에서 수행하며 각 후보를 새로운 트랜잭션으로 저장합니다.
- 닉네임 충돌 재시도는 최대 5회로 제한하며, `IdentifyUserUseCase`가 재시도 여부를 판단합니다.
- 동일한 활성 provider identity의 동시 생성은 DB UNIQUE 제약으로 차단하고 `PROVIDER_IDENTITY_CONFLICT`로 처리합니다.

## 6. 오류와 제약사항

- 잘못된 회원 ID·닉네임·provider 정보는 각각 회원 입력 오류로 거부합니다.
- 활성 닉네임 충돌은 `ErrorCode.NICKNAME_CONFLICT`, 활성 provider identity 충돌은 `ErrorCode.PROVIDER_IDENTITY_CONFLICT`로 구분합니다.
- 닉네임 충돌 재시도가 5회를 모두 실패하면 `ErrorCode.NICKNAME_GENERATION_FAILED`로 처리합니다.
- 활성 `UserProvider`가 참조하는 `User`가 없거나 비활성 상태인 경우 사용자 입력 오류가 아니라 내부 데이터 정합성 오류로 처리합니다.
- 회원 탈퇴·복구, 소셜 계정 연결 해제·복구와 provider 추가는 현재 구현 범위에 포함하지 않습니다.
