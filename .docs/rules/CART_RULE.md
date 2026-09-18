# 장바구니 상품 추가 규칙

이 문서는 장바구니 상품 추가 기능의 확정 정책, 책임 경계, 테스트 기준을 기록합니다.
구현 결과와 실행한 검증 결과는 PR 본문에 별도로 기록합니다.
4절의 구현 흐름은 이후 기능을 추가할 때 따를 표준 구조이며, 세부 공통 규칙은 [아키텍처 문서](../ARCHITECTURE.md)와 `AGENTS.md`를 기준으로 합니다.

## 1. 범위

이번 범위는 `POST /api/v1/cart/items`입니다.

- `userId`는 회원 기능이 완성될 때까지 요청 파라미터로 받습니다.
- 상품 기능과 `CartProductClient`가 없으므로 상품 존재·재고 확인은 수행하지 않습니다.
- 조회·수량 변경·단건 삭제·다건 삭제 API와 해당 테스트는 별도 작업으로 둡니다.
- 결제 후 장바구니 차감은 주문·결제 연동 작업으로 남깁니다.
- `Idempotency-Key` 저장·재생 응답은 주문·결제 연동 시 별도 정책으로 결정합니다.

## 2. API 계약

`POST /api/v1/cart/items?userId={userId}`

```json
{
  "cartId": 1,
  "productId": 20,
  "quantity": 3
}
```

| 항목 | 규칙 | 책임 |
| --- | --- | --- |
| `userId` | 양수인 `Long` 필수 | `CartController`의 요청 검증 |
| `cartId` | null이 아니고 양수인 `Long` | `AddCartItemRequest`에서 검증하지만 현재 Command에는 포함하지 않음 |
| `productId` | null이 아니고 양수인 `Long` | `AddCartItemRequest` |
| `quantity` | null이 아니고 1~500인 `Integer` | `AddCartItemRequest`와 Domain |
| 성공 응답 | `200`, `ApiResponse<Void>` | `CartController` |
| 잘못된 HTTP 요청 | 공통 `E400` 응답 | Bean Validation과 `GlobalExceptionHandler` |
| 장바구니 상품 한도 초과 | `E8000` (`CART_ITEM_LIMIT_EXCEEDED`) | `Cart` Domain과 `ErrorCode` |

## 3. 비즈니스·도메인 규칙

### 3.1 같은 상품은 요청 수량으로 대체한다

같은 사용자의 장바구니에 동일한 상품이 있으면 기존 수량에 더하지 않고 요청한 수량으로 대체합니다.
예를 들어 기존 수량이 2개일 때 `quantity: 3`을 요청하면 최종 수량은 3개입니다.

이 정책은 동일 상품에 대한 재시도 결과를 요청 상태로 수렴시키지만, 요청 식별자를 저장하는 강한 의미의 멱등성은 아닙니다.

### 3.2 수량은 1~500개다

- HTTP 형식·범위는 `AddCartItemRequest`에서 검증합니다.
- HTTP 외부에서 호출되는 Application 계약은 UseCase가 저장 상태와 필요한 전제조건을 확인합니다.
- 최종 Domain 불변식은 `CartItem`이 보장합니다.
- 데이터베이스의 `CHECK` 제약도 같은 범위를 보강합니다.

잘못된 수량은 기존 장바구니 상태를 변경하기 전에 거부합니다.

### 3.3 장바구니에는 서로 다른 상품을 최대 30개까지 담는다

새 상품을 추가할 때 장바구니의 상품 수가 30개 이상이면 추가하지 않고 `CART_ITEM_LIMIT_EXCEEDED`를 발생시킵니다.
이미 담긴 상품의 수량 대체는 상품 수를 늘리지 않으므로 허용합니다.

상품 수 제한은 장바구니 상태 불변식이므로 Controller나 Repository가 아니라 `Cart`가 판단합니다.

### 3.4 하나의 UseCase 트랜잭션으로 처리한다

처리 흐름은 다음과 같습니다.

1. `CartService`가 `AddCartItemCommand`를 받아 `AddCartItemUseCase` 호출을 조율합니다.
2. `AddCartItemUseCase`가 쓰기 트랜잭션 안에서 `CartRepositoryPort`로 사용자 장바구니를 조회합니다. 장바구니가 없으면 `CART_NOT_FOUND`를 발생시킵니다.
3. `CartItemRepositoryPort`가 상품 항목을 조회하고, `Cart`와 `CartItem`이 상태를 변경합니다.
4. `CartRepositoryAdapter`와 `CartItemRepositoryAdapter`가 Spring Data JPA Repository를 사용합니다.

현재 `CartService`는 `addCartItem`에서 하나의 UseCase를 호출하며, 추후 CRUD와 추가 기능의 실행 순서를 조율하는 확장 지점으로 유지합니다.

현재 범위에서는 별도 잠금 로직을 사용하지 않습니다. 사용자·상품 조합의 DB 유일 제약은 중복 항목을 막지만, 동시 요청의 직렬화나 재시도 응답까지 보장하지는 않습니다.

### 3.5 데이터베이스 제약으로 핵심 불변식을 보강한다

- 사용자당 장바구니 1개: `uk_carts_user_id`
- 장바구니·상품 조합 1개: `uk_cart_item_cart_product`
- 항목 수량 1~500: `ck_cart_item_quantity`
- 장바구니 없는 항목 방지: `fk_cart_item_cart`

## 4. 구현 위치와 책임

```text
CartController
    → CartCommandConverter
    → CartService (orchestration layer)
    → AddCartItemUseCase
    → CartRepositoryPort / CartItemRepositoryPort
    ← CartRepositoryAdapter / CartItemRepositoryAdapter
        → CartJpaRepository / CartItemJpaRepository
```

| 구성 요소 | 책임 |
| --- | --- |
| `CartController` | HTTP 입력 검증, Service 호출, HTTP 응답 반환 |
| `CartCommandConverter` | `userId`와 `AddCartItemRequest`를 `AddCartItemCommand`로 변환 |
| `CartService` | 향후 CRUD와 추가 기능을 포함한 기능 흐름의 orchestration layer |
| `AddCartItemUseCase` | 트랜잭션 안에서 Port와 Domain 조율 |
| `Cart` | 동일 상품 대체와 상품 수 제한 등 장바구니 상태 전이 |
| `CartItem` | 상품 수량 불변식과 수량 상태 변경 |
| `CartRepositoryPort` | Application이 사용하는 저장 계약 |
| `CartRepositoryAdapter`, `CartItemRepositoryAdapter` | Port와 Spring Data JPA Repository 조합 |
| `CartJpaRepository`, `CartItemJpaRepository` | Spring Data JPA 접근 |

Domain 모델이 JPA Entity를 겸하므로 업무 Domain용 별도 Entity나 `CartAddRequestEntity`를 만들지 않습니다.
`Cart`와 `CartItem`은 공통 `id`, 상태, 생성·수정 시간 매핑을 `common/domain/BaseEntity`에서 상속합니다. 각 장바구니 Entity에 `@Id`와 `IDENTITY` 생성 전략을 중복 선언하지 않으며, 새 객체는 ID 없이 생성합니다.
현재 Cart 저장 구현은 `CartRepositoryAdapter`, `CartItemRepositoryAdapter`가 Port와 Spring Data JPA Repository를 조합합니다. 복잡한 조인·통계·벌크 연산·프로젝션이 실제로 필요할 때만 Querydsl용 QueryRepository를 추가합니다.

저장소의 `DataAccessException`·`PersistenceException`은 일괄적으로 `CoreException`으로 바꾸지 않고 전파합니다. 알려진 비즈니스 오류만 `CoreException`과 `ErrorCode`로 표현하며, 기술 예외의 HTTP 응답 변환은 `GlobalExceptionHandler`가 담당합니다.

## 5. 테스트 기준

| 테스트 | 검증할 케이스 |
| --- | --- |
| `CartTest` | 신규 상품 추가, 30개 초과 시 `CART_ITEM_LIMIT_EXCEEDED`, 잘못된 수량 입력 |
| `CartAddUseCaseTest` | 장바구니 없음 오류, 기존 장바구니의 상품 추가·수량 대체, Port 호출 흐름 |
| `CartControllerTest` | 요청 검증, Command 변환, 유효 요청의 HTTP 응답, 잘못된 요청의 Service 미호출 |
| `CartRepositoryIntegrationTest` | 실제 MySQL의 장바구니·항목 유일성, 수량 대체, DB 제약 |
| `ArchitectureTest` | Domain·Application·API의 의존 방향과 JPA Domain Entity 규칙 |

현재 브랜치에서 실행한 검증 결과는 PR 본문에 기록하며, 컴파일·테스트가 통과하기 전까지 기능 완료로 표시하지 않습니다.

## 6. 검증 명령

```bash
./gradlew spotlessApply
./gradlew test
TESTCONTAINERS_RYUK_DISABLED=true ./gradlew integrationTest --tests 'com.book.core.cart.infrastructure.persistence.repository.CartRepositoryIntegrationTest'
./gradlew check
```
