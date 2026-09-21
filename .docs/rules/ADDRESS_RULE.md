# 주소지 도메인 규칙

이 문서는 주소지 등록·조회·수정 기능의 정책과 상태 전이를 기록합니다.
삭제·복구 규칙은 이번 구현 범위에 포함하지 않습니다.

## 1. 범위와 생명주기

- 주소지는 요청 회원의 소유로 저장합니다.
- 등록 시 상태는 `EntityStatus.ACTIVE`입니다.
- 활성 주소지는 `EntityStatus.ACTIVE`인 주소지입니다.
- `deleted_at` 컬럼과 관련 마이그레이션은 추가하지 않습니다.
- 주소지 삭제·복구·삭제 후 기본 배송지 승격은 구현하지 않습니다.
- 향후 삭제가 필요하면 프로젝트의 `EntityStatus.DELETED`와 `Address.delete()` 규칙을 재사용합니다.
- 주문 시점의 `order_addresses` 스냅샷은 생성하거나 수정하지 않습니다.

## 2. 입력 정규화

- `label`, `postalCode`, `address`는 앞뒤 공백을 제거한 뒤 검증·저장합니다.
- `detailAddress`는 생략·`null`·공백만 있는 값이면 `NULL`로 저장합니다.
- `detailAddress`에 값이 있으면 앞뒤 공백을 제거해 저장합니다.
- 정규화 후 필수 값이 비어 있으면 `E400 INVALID_REQUEST`로 거부합니다.

## 3. 등록 규칙

- 회원당 활성 주소지는 최대 3개입니다.
- 활성 주소지가 3개인 회원의 네 번째 등록 요청은 `ErrorCode.ADDRESS_LIMIT_EXCEEDED`로 거부합니다. 공개 오류 코드는 `E400`입니다.
- 첫 번째 주소는 요청의 `isDefault` 값과 관계없이 기본 배송지입니다.
- 기존 주소가 있으면 `isDefault=true`일 때만 기존 기본 배송지를 해제합니다.
- `isDefault=false`이면 기존 기본 배송지를 유지합니다.
- 동일 회원의 정규화된 주소·상세주소·별칭 조합은 중복 등록하지 않습니다. 중복 요청은 `ErrorCode.DUPLICATE_ADDRESS`로 거부하며 공개 오류 코드는 `E400`입니다.
- 별칭이 다르면 같은 주소도 등록할 수 있습니다.

## 4. 트랜잭션과 동시성

- 활성 주소 개수 확인, 기존 기본 배송지 해제, 신규 주소 저장은 하나의 트랜잭션에서 처리합니다.
- 현재 회원·인증 테이블이 없어 회원 행 잠금을 적용하지 않습니다.
- 회원 행이 추가되면 회원 행 잠금 등 프로젝트가 정한 동시성 제어를 재사용해야 합니다.

## 5. 수정 규칙

- 수정 대상은 요청 `userId`가 소유한 `addressId` 경로의 주소지입니다.
- `EntityStatus.ACTIVE`인 주소지만 수정할 수 있습니다.
- 주소지 수정은 부분 수정입니다. 요청에 포함되지 않은 필드는 기존 값을 유지합니다.
- 수정 가능한 필드는 `label`, `postalCode`, `address`, `detailAddress`, `isDefaultAddress`입니다. `addressId`는 경로에서만 받고 `userId`는 요청 본문에 포함하지 않습니다.
- `label`, `postalCode`, `address`는 전달된 경우 앞뒤 공백을 제거한 뒤 검증·저장합니다. 정규화 후 비어 있으면 `E400 INVALID_REQUEST`로 거부합니다.
- `detailAddress`는 생략하면 기존 값을 유지합니다. `null`·공백만 있는 값은 `NULL`로 저장하고, 값이 있으면 앞뒤 공백을 제거해 저장합니다.
- `isDefaultAddress`는 생략하면 기존 값을 유지합니다.
- `isDefaultAddress=true`이면 기존 기본 배송지를 해제하고 수정 대상 주소지를 기본 배송지로 변경합니다.
- 기본 배송지가 아닌 주소지의 `isDefaultAddress=false` 요청은 기존 기본 배송지를 유지합니다.
- 현재 기본 배송지의 `isDefaultAddress=false` 요청은 `E400 INVALID_REQUEST`로 거부합니다.
- 같은 회원의 다른 활성 주소지와 정규화된 `label`·`address`·`detailAddress`가 같으면 `E400 DUPLICATE_ADDRESS`로 거부합니다. 수정 대상 자기 자신은 중복 판정에서 제외하며 `postalCode`는 사용하지 않습니다.
- 수정 대상이 없거나 비활성 상태이거나 요청 `userId`가 소유하지 않으면 `E403 FORBIDDEN`으로 처리합니다.

## 6. 수정 트랜잭션과 시간 필드

- 주소지 조회, 소유·활성 상태 확인, 중복 확인, 기본 배송지 전환과 수정 저장은 하나의 쓰기 트랜잭션에서 처리합니다.
- `created_at`은 유지하고 `updated_at`만 갱신합니다.
- 시간 필드는 `BaseEntity`·`BaseTimeEntity`의 기존 JPA 매핑을 사용하며 수정 요청에서 직접 받지 않습니다.

## 7. 인증 연계 전제

- 현재 인증·인가와 회원 기능은 별도 작업 중이므로 주소지 수정 기능에서 인증 Resolver, 회원 조회, Authorization 검증을 새로 구현하지 않습니다.
- 구현 단계에서는 기존 주소지 등록·장바구니 Controller와 같은 방식으로 `userId`를 전달받아 주소지 소유 조건에 사용합니다.
- Notion 명세의 `Authorization` 헤더와 실제 인증 회원 연계는 인증·인가 기능이 연결될 때 대체합니다.
- 인증 계층의 `E401 UNAUTHORIZED` 처리는 이 기능의 도메인 로직 범위에 포함하지 않습니다.

## 8. 수정 API 응답·오류 계약

- 성공 응답은 프로젝트의 `ApiResponse` envelope을 사용하고 `data.addressId`에 수정한 주소지 ID를 반환합니다.
- 요청 형식·수정 정책 위반은 `E400`, 인증 실패는 명세상 `E401`(현재 인증 계층에 위임), 수정 대상의 미존재·비활성·소유권 불일치는 `E403`, 예상하지 못한 오류는 `E500`으로 응답합니다.
- `deleted_at` 컬럼과 삭제 기능, `order_addresses` 스냅샷은 추가하거나 수정하지 않습니다.
