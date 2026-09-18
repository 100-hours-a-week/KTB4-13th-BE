# 장바구니 상품 추가 규칙

이 문서는 `feat/10-cart-add`에서 구현한 장바구니 상품 추가 범위의 계약과 근거를 기록합니다.
라인 번호는 이 브랜치에서 Spotless 적용 후의 기준입니다.

## 1. 범위

이번 PR은 `POST /api/v1/cart/items`만 다룹니다.

- 조회, 수량 변경, 단건 삭제, 다건 삭제 API와 테스트는 포함하지 않습니다.
- 상품 기능과 `CartProductClient`가 아직 없으므로 상품 존재·재고 확인은 하지 않습니다.
- `userId`는 회원 기능이 완성될 때까지 요청 파라미터로 받습니다.
- 결제 후 장바구니 차감과 `Idempotency-Key` 저장·재생 재처리는 주문·결제 연동 범위로 남깁니다.

## 2. API 계약

`POST /api/v1/cart/items?userId={userId}`

```json
{
  "productId": 20,
  "quantity": 3
}
```

| 항목 | 규칙 | 구현 위치 |
| --- | --- | --- |
| `userId` | 양수 필수 | `CartController.java:28-30` |
| `productId` | null이 아니고 양수 | `CartAddRequest.java:8-10` |
| `quantity` | 필수, 1~500 | `CartAddRequest.java:8-10` |
| 성공 응답 | `200`, `{ "success": true }` | `CartController.java:26-31` |
| 잘못된 요청 | 공통 `E400` 응답 | `CartAddCommand.java:7-13`, `CartItem.java:56-65` |
| 장바구니 상품 수 초과 | 최대 30개 초과 시 `E8000` | `Cart.java:27`, `Cart.java:68-70`, `ErrorCode.java:3-7`, `ErrorType.java:6-13` |

## 3. 비즈니스·도메인 규칙

### 3.1 같은 상품은 요청 수량으로 대체한다

같은 `userId`의 장바구니에 동일한 `productId`가 있으면 기존 수량에 더하지 않고 요청 수량으로 바꿉니다.
따라서 `2`개가 담긴 상품에 `quantity: 3`을 반복 요청해도 최종 수량은 `3`개입니다.

- 규칙: `Cart.add()`가 기존 상품을 찾으면 `CartItem.replaceQuantity()`를 호출합니다.
- 위치: `src/main/java/com/book/core/cart/domain/Cart.java:53-65`, `CartItem.java:62-65`
- 이유: 클라이언트 재시도나 중복 요청이 누적 수량을 만들지 않도록 추가 요청의 결과를 요청 상태로 수렴시킵니다.
- 한계: 요청 식별자를 저장하는 강한 의미의 멱등성은 아닙니다. 동일 상품의 최종 수량을 대체하는 현재 장바구니 정책입니다.

### 3.2 상품별 수량은 1~500개다

HTTP 경계에서는 Bean Validation으로 빠르게 거부하고, Command와 Domain에서도 같은 불변식을 다시 확인합니다.

- API 입력: `CartAddRequest.java:8-10`
- Application 입력: `CartAddCommand.java:7-13`
- Domain 불변식: `CartItem.java:56-65`
- 이유: HTTP 요청 외의 UseCase 호출도 유효하지 않은 수량을 만들 수 없게 합니다. 이미 담긴 상품의 잘못된 대체 요청도 기존 상태를 바꾸기 전에 거부됩니다.
- DB 보강: `src/main/resources/db/migration/V2__create_carts.sql:15-21`

### 3.3 장바구니에는 서로 다른 상품을 최대 30개까지 담는다

새 상품을 추가할 때만 개수를 검사합니다. 이미 담긴 상품의 수량 대체는 상품 수를 늘리지 않으므로 30개 상태에서도 허용합니다.

- 규칙: `Cart.java:27`, `Cart.java:63-70`
- 오류: `ErrorType.CART_ITEM_LIMIT_EXCEEDED` (`E8000`)
- 이유: 상품 수 제한은 장바구니의 상태 불변식이므로 Controller나 Repository가 아니라 Domain이 판단해야 합니다.

### 3.4 장바구니 생성과 추가 저장은 하나의 트랜잭션으로 처리한다

UseCase는 장바구니를 조회·생성한 뒤 Domain에 추가를 위임하고, 결과 항목만 저장합니다.

1. `CartAddUseCase.execute()`가 쓰기 트랜잭션을 시작합니다.
2. `CartRepository.findOrCreate()`가 사용자별 장바구니를 없으면 생성하고 일반 조회로 읽습니다.
3. 저장소가 기존 항목을 읽어 `Cart.add()`가 동일 상품을 식별할 수 있게 합니다.
4. 신규 항목은 insert하고 기존 항목은 같은 항목 ID로 수량을 대체합니다.

- UseCase: `src/main/java/com/book/core/cart/application/usecase/CartAddUseCase.java:14-19`
- Port: `src/main/java/com/book/core/cart/application/port/CartRepository.java:6-9`
- 생성·조회: `CartJpaRepository.java:11-20`, `CartRepositoryImpl.java:20-29`
- 항목 저장: `CartRepositoryImpl.java:31-52`
- 이유: 이번 범위에서는 별도 잠금 없이 사용자·상품 조합의 DB 유일 제약으로 중복 데이터를 막습니다. 같은 사용자에 대한 동시 추가의 직렬화와 재시도 정책은 후속 작업으로 남깁니다.

### 3.5 DB 제약으로 핵심 불변식을 한 번 더 보장한다

- 사용자당 장바구니 1개: `uk_carts_user_id`
- 장바구니·상품 조합 1개: `uk_cart_item_cart_product`
- 항목 수량 1~500: `ck_cart_item_quantity`
- 장바구니 없는 항목 방지: `fk_cart_item_cart`
- 위치: `src/main/resources/db/migration/V2__create_carts.sql:1-24`
- 이유: 애플리케이션 경로 외의 직접 DB 접근이나 동시성 경로에서도 데이터 무결성을 유지해야 합니다.

## 4. 구현 위치와 책임

| 클래스 | 책임 | 코드 위치 |
| --- | --- | --- |
| `CartController` | `userId`와 요청을 Command로 변환하고 UseCase 호출 | `src/main/java/com/book/core/cart/api/CartController.java:23-32` |
| `CartAddRequest` | HTTP 필드 검증 | `src/main/java/com/book/core/cart/api/request/CartAddRequest.java:8-10` |
| `CartControllerSpec` | springdoc API 계약과 대체 정책 문서화 | `src/main/java/com/book/core/cart/api/spec/CartControllerSpec.java:18-33` |
| `CartAddCommand` | HTTP 밖에서도 사용할 추가 전제조건 검증 | `src/main/java/com/book/core/cart/application/command/CartAddCommand.java:7-13` |
| `CartAddUseCase` | 트랜잭션 안에서 Port와 Domain을 조율 | `src/main/java/com/book/core/cart/application/usecase/CartAddUseCase.java:11-19` |
| `Cart` | 동일 상품 대체, 상품 수 제한, 항목 상태 관리 | `src/main/java/com/book/core/cart/domain/Cart.java:26-70` |
| `CartItem` | 수량 불변식과 수량 대체 값 생성 | `src/main/java/com/book/core/cart/domain/CartItem.java:23-66` |
| `CartRepository` | add에 필요한 장바구니 생성·항목 저장 최소 계약 | `src/main/java/com/book/core/cart/application/port/CartRepository.java:6-9` |
| `CartRepositoryImpl` | 기존 항목 로드, 저장, 공통 저장소 오류 변환 | `src/main/java/com/book/core/cart/infrastructure/persistence/repository/CartRepositoryImpl.java:16-66` |

조회·수정·삭제를 위한 Port 메서드, UseCase, Controller, DTO, 테스트는 이 브랜치에 만들지 않았습니다. 저장소 내부의 기존 항목 조회는 add의 대체 여부를 판단하기 위한 구현 세부 계약입니다.

## 5. 테스트 케이스

| 테스트 | 검증 케이스 | 위치 |
| --- | --- | --- |
| `CartTest` | 새 상품 추가, 동일 상품 수량 대체, 30개 초과 거부, 잘못된 대체 수량 시 기존 상태 유지 | `src/test/java/com/book/core/cart/domain/CartTest.java:12-56` |
| `CartAddUseCaseTest` | 첫 요청의 장바구니 생성·저장, 반복 추가의 수량 대체, Command ID·수량 전제조건 | `src/test/java/com/book/core/cart/application/CartAddUseCaseTest.java:11-41` |
| `CartControllerTest` | quantity 누락·잘못된 userId·productId·quantity의 400 및 UseCase 미호출, 유효 수량 전달 | `src/test/java/com/book/core/cart/api/CartControllerTest.java:22-84` |
| `CartRepositoryIntegrationTest` | 실제 MySQL에서 장바구니 1개·항목 1개 유지, 수량 대체, DB 수량 제약 | `src/test/java/com/book/core/cart/infrastructure/persistence/repository/CartRepositoryIntegrationTest.java:25-67` |
| `ArchitectureTest` | Cart·CartItem이 JPA Domain Entity이고 계층 의존 규칙을 지킴 | `src/test/java/com/book/ArchitectureTest.java:1-83` |
| `BookApplicationTest` | CartAddUseCase와 CartRepository가 Spring Context에 한 개씩 등록됨 | `src/test/java/com/book/BookApplicationTest.java:36-43` |

기존 `SampleRepositoryIntegrationTest`의 Flyway 성공 마이그레이션 개수 기대값은 V2 추가에 맞춰 `1`에서 `2`로 갱신했습니다.

## 6. 검증 명령

- `./gradlew spotlessApply`
- `./gradlew test`
- `TESTCONTAINERS_RYUK_DISABLED=true ./gradlew integrationTest --tests 'com.book.core.cart.infrastructure.persistence.repository.CartRepositoryIntegrationTest'`

변경 후 전체 `integrationTest`와 `check`를 실행하고 결과를 PR 본문에 기록합니다.
