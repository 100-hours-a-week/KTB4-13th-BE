# 인증 도메인 규칙

이 문서는 Kakao 로그인 인증, 서비스 토큰과 Refresh Session 정책, Access Token 기반 인가 규칙을 기록합니다.

회원 식별과 신규 회원 생성은 회원 도메인에서 담당하며, 인증 도메인은 외부 Provider 인증과 서비스 토큰 발급·검증에 집중합니다. 공통 오류 계약은 [예외 처리 규칙](EXCEPTION_RULE.md)을 기준으로 합니다.

## 1. 범위와 용어

- 로그인은 Kakao 인증을 시작해 내부 회원을 식별하고 서비스 토큰을 발급하는 유스케이스입니다.
- 인증(Authentication)은 외부 사용자가 누구인지 확인하고 내부 `userId`를 식별해 Access Token과 Refresh Token을 발급하는 책임입니다.
- 인가(Authorization)는 유효한 Access Token을 가진 요청만 보호 자원에 접근하도록 제한하는 책임입니다.
- 로그인 API는 인증 흐름의 진입점이며 회원 Domain과 Spring Security 인가 설정을 직접 소유하지 않습니다.
- Refresh Token 재발급은 유효한 Refresh Token을 기준으로 새로운 Access Token과 Refresh Token을 발급하고 기존 Refresh Session을 폐기하는 인증 흐름입니다.
- 현재 외부 인증 provider는 Kakao만 지원합니다.

## 2. Kakao 로그인 입력과 검증

- 로그인 API는 `POST /api/v1/auth/{providerType}/login`이며 현재 경로의 `providerType`은 `kakao`만 허용합니다.
- Frontend는 Kakao Authorization Code Flow를 시작하며 PKCE `codeVerifier`, `state`, `nonce`를 생성·관리합니다.
- Backend 로그인 요청은 `authorizationCode`, `codeVerifier`, `nonce`를 필수로 받습니다.
- `state`는 Backend 로그인 요청 계약에 포함하지 않으며 Frontend가 callback에서 검증합니다.
- Backend는 `authorizationCode`와 `codeVerifier`로 Kakao Token API를 호출해 ID Token을 받습니다.
- Token API 요청에는 `grant_type`, REST API Key, redirect URI, authorization code와 PKCE code verifier를 포함합니다.
- Kakao Client Secret은 선택 설정이며 값이 있을 때만 Token API 요청에 포함합니다.

Kakao ID Token은 다음 조건을 모두 만족해야 합니다.

- 서명 알고리즘은 RS256이며 Kakao JWKS 공개키로 서명을 검증합니다.
- issuer는 `https://kauth.kakao.com`입니다.
- audience는 설정된 Kakao REST API Key입니다.
- 만료 시간이 존재하고 현재 시각 기준으로 유효해야 합니다.
- `sub`가 존재하고 공백이 아니어야 합니다.
- Token의 `nonce`가 로그인 요청의 `nonce`와 일치해야 합니다.
- `email` claim은 선택 값이며 없으면 `null`로 처리합니다.

## 3. 로그인 인증 흐름

```text
Kakao 로그인(providerType, authorizationCode, codeVerifier, nonce):

1. Kakao Token API에 authorizationCode와 codeVerifier를 전달해 ID Token을 받는다.
2. ID Token의 서명, issuer, audience, 만료, sub와 nonce를 검증한다.
3. sub를 providerUserId로, 선택적 email을 providerEmail로 변환한다.
4. 회원 도메인에서 providerType과 providerUserId로 기존 회원을 조회하거나 신규 회원을 생성한다.
5. 내부 userId를 subject로 하는 Access Token과 Refresh Token을 발급한다.
6. 기존 활성 Refresh Session이 있으면 폐기하고 새 Refresh Session을 저장한다.
7. Access Token은 응답 body로, Refresh Token은 HttpOnly cookie로 반환한다.
```

## 4. 서비스 토큰 정책

- Access Token과 Refresh Token은 HS512로 서명한 자체 JWT입니다.
- 서명 키는 `JWT_SECRET`으로 설정한 Base64 값이며 디코딩 후 64바이트 이상이어야 합니다.
- 두 토큰의 subject는 내부 회원의 `userId` 문자열입니다.
- Access Token의 `tokenType` claim은 `ACCESS`, Refresh Token은 `REFRESH`입니다.
- 각 토큰은 UUID로 생성한 `jti`를 가집니다.
- Access Token 유효 기간은 1시간입니다.
- Refresh Token 유효 기간은 7일입니다.
- Access Token은 로그인 성공 또는 재발급 성공 응답 body의 `accessToken`으로 반환합니다.
- Refresh Token은 응답 body에 노출하지 않고 이름이 `refreshToken`인 HttpOnly cookie로 반환합니다.
- Refresh cookie의 path는 `/api/v1/auth`, SameSite는 `Lax`, Max-Age는 7일입니다.
- Refresh cookie의 Secure 여부는 `AUTH_COOKIE_SECURE` 설정으로 관리합니다.

## 5. Refresh Session 규칙

- Refresh Token 원문은 DB에 저장하지 않고 SHA-256 해시를 `token_hash`에 저장합니다.
- Refresh Session은 `userId`, `tokenHash`, `expiresAt`, 선택적 `revokedAt`을 가집니다.
- `expiresAt`은 Refresh Token 만료 시각을 기록하고 Session 활성 여부는 현재 `revokedAt`으로 판단합니다.
- `revokedAt`이 `null`이면 활성 Session이며 값이 있으면 폐기된 Session입니다.
- 사용자당 활성 Refresh Session은 하나만 허용합니다.
- 로그인 시 기존 활성 Session이 있으면 현재 시각으로 폐기한 뒤 새 Session을 저장합니다.
- 재발급 시 기존 활성 Session을 폐기하고 새 Refresh Session을 저장합니다.
- MySQL의 `active_flag`는 `revoked_at IS NULL`이면 `1`, 그 외에는 `NULL`인 generated column입니다.
- `active_flag`와 `user_id`의 UNIQUE 제약이 사용자당 단일 활성 Session을 보장합니다.
- Refresh Session의 `user_id`는 `users.id` 외래 키를 참조합니다.
- 재발급 시 동일 Refresh Token의 중복 사용을 방지하기 위해 활성 Session 조회 시 비관적 쓰기 잠금(`PESSIMISTIC_WRITE`)을 사용합니다.
- 재발급에서 기존 Session 폐기와 신규 Session 저장은 하나의 transaction으로 처리합니다.
- 신규 Session 저장에 실패하면 기존 Session 폐기도 rollback됩니다.

## 6. Refresh Token 재발급 규칙

- 재발급 API는 `POST /api/v1/auth/reissue`입니다.
- 재발급 요청은 request body를 사용하지 않고 `refreshToken` HttpOnly cookie를 사용합니다.
- 재발급 API는 Access Token 인증 대상이 아닙니다.
- 재발급 요청에 Access Token이 없어도 Refresh Token 검증을 통해 인증을 수행합니다.

Refresh Token은 다음 조건을 모두 만족해야 합니다.

- HS512 서명이 유효해야 합니다.
- 만료 시간이 존재하고 현재 시각 기준으로 유효해야 합니다.
- `tokenType` claim은 `REFRESH`여야 합니다.
- subject는 공백이 아니고 양수인 `Long` 형식의 내부 `userId`여야 합니다.

재발급 흐름은 다음과 같습니다.

```text
Refresh Token 재발급(refreshToken):

1. HttpOnly cookie에서 Refresh Token을 조회한다.
2. Refresh Token의 서명, 만료, tokenType과 subject를 검증한다.
3. Refresh Token 원문을 SHA-256으로 해시한다.
4. tokenHash와 일치하는 활성 Refresh Session을 비관적 쓰기 잠금으로 조회한다.
5. Refresh Token의 userId와 Refresh Session의 userId가 일치하는지 검증한다.
6. 새로운 Access Token과 Refresh Token을 발급한다.
7. 기존 Refresh Session을 현재 시각으로 폐기한다.
8. 새 Refresh Token의 hash와 만료 시각으로 신규 Refresh Session을 저장한다.
9. Access Token은 응답 body로, 새 Refresh Token은 HttpOnly cookie로 반환한다.
```

### Refresh Token Rotation

- 재발급 성공 시 기존 Refresh Token은 폐기하고 새로운 Refresh Token으로 교체합니다.
- 한 번 사용한 Refresh Token은 다시 사용할 수 없습니다.
- 동일한 Refresh Token으로 동시에 재발급 요청이 들어오면 활성 Refresh Session 조회 시 row lock을 획득합니다.
- 먼저 lock을 획득한 요청이 기존 Session을 폐기하고 신규 Session을 저장한 뒤 transaction을 commit합니다.
- 이후 대기 중이던 요청은 활성 Session을 찾을 수 없으므로 `INVALID_REFRESH_TOKEN` 오류를 반환합니다.
- DB UNIQUE 제약만으로 중복 재발급을 처리하지 않고, row lock을 통해 재발급 흐름 자체를 직렬화합니다.

### 재발급 오류

- Refresh Token cookie가 없으면 `ErrorCode.INVALID_REFRESH_TOKEN`의 401 오류로 처리합니다.
- Refresh Token 서명, 만료, tokenType 또는 subject 검증에 실패하면 `ErrorCode.INVALID_REFRESH_TOKEN`의 401 오류로 처리합니다.
- tokenHash에 해당하는 활성 Refresh Session이 없으면 `ErrorCode.INVALID_REFRESH_TOKEN`의 401 오류로 처리합니다.
- Refresh Token의 `userId`와 Refresh Session의 `userId`가 다르면 `ErrorCode.INVALID_REFRESH_TOKEN`의 401 오류로 처리합니다.

## 7. Access Token 인가 규칙

- Spring Security Resource Server가 Authorization header의 Bearer Access Token을 검증합니다.
- Access Token은 HS512 서명, 만료 시간, `tokenType=ACCESS`와 subject를 검증합니다.
- subject는 공백이 아니고 양수인 `Long` 형식이어야 하며 인증된 요청의 `userId`로 사용합니다.
- 로그인 API와 Refresh Token 재발급 API를 제외한 요청은 인증이 필요합니다.
- 서버는 HTTP Session을 만들지 않는 stateless 방식이며 form login과 HTTP Basic 인증을 사용하지 않습니다.
- 현재 Bearer Access Token 기반 요청을 기준으로 CSRF를 비활성화합니다.

### Access Token 인증 실패 응답

- Access Token이 없는 경우와 서명·만료·`tokenType`·subject 중 하나라도 유효하지 않은 경우를 모두 동일한 인증 실패로 처리합니다. 실패 사유의 구분은 내부 검증 단계에만 있으며 외부 응답 계약에는 노출하지 않습니다.
- Spring Security 인증 실패는 `JwtAuthenticationEntryPoint`가 처리하며, 다른 비즈니스 오류와 동일한 공통 오류 응답 계약을 사용합니다.
- 응답은 HTTP 401, `ErrorCode.UNAUTHORIZED`(공개 코드 `E401`)이며 `success=false`와 공통 `message`, `traceId`를 포함합니다.
- `data`는 값이 없으므로 공통 직렬화 정책에 따라 응답 본문에서 생략됩니다.

## 8. CORS 규칙

- 허용 origin은 `FRONTEND_ORIGIN` 설정 하나로 관리하며 기본값은 `http://localhost:5173`입니다.
- 허용 method는 GET, POST, PUT, PATCH, DELETE, OPTIONS입니다.
- 허용 header는 Authorization과 Content-Type입니다.
- Refresh Token cookie 전달을 위해 credential 요청을 허용합니다.
- credential 요청과 wildcard origin을 함께 사용하지 않습니다.

## 9. 오류와 외부 연동 제약

- 잘못되거나 만료된 authorization code는 `ErrorCode.INVALID_AUTHORIZATION_CODE`의 401 오류로 처리합니다.
- ID Token 형식·서명·claim·nonce 검증 실패는 `ErrorCode.INVALID_ID_TOKEN`의 401 오류로 처리합니다.
- Kakao JWKS 조회나 Kakao 서비스 연결 실패는 `ErrorCode.OAUTH_PROVIDER_UNAVAILABLE`의 502 오류로 처리합니다.
- Kakao 애플리케이션 설정 오류와 Token API 교환 실패는 각각 별도 500 인증 오류로 구분합니다.
- 서비스 JWT 발급 실패는 `ErrorCode.TOKEN_ISSUE_FAILURE`의 500 오류로 처리합니다.
- 유효하지 않은 Refresh Token은 `ErrorCode.INVALID_REFRESH_TOKEN`의 401 오류로 처리합니다.
- 토큰, authorization code, cookie 값과 외부 인증 응답 원문은 응답이나 로그에 노출하지 않습니다.

## 10. 추가 검토 사항

- Logout 정책은 이 문서의 범위에 포함하지 않습니다.
- 발급된 Access Token을 만료 전에 즉시 폐기하는 blacklist는 구현하지 않습니다.
- Refresh 재발급·logout처럼 cookie를 사용하는 endpoint에 대한 최종 CSRF 정책은 별도로 검토해야 합니다.
- 운영 환경이 cross-site 구성이면 Refresh cookie의 `SameSite=None`, `Secure=true` 적용 여부를 재검토해야 합니다.
