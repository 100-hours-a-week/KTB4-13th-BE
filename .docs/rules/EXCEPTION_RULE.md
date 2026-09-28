# 예외 처리 규칙

예외는 발생한 계층의 책임과 HTTP 오류 계약을 분리해 관리합니다.
비즈니스 오류와 기술 오류를 같은 방식으로 감싸지 않습니다.

## 1. 공통 오류 계약

- 공통 오류 코드는 `common/exception/ErrorCode`에서 관리합니다.
- HTTP 상태·공개 코드·메시지·로그 수준도 `common/exception/ErrorCode`에서 관리합니다.
- 애플리케이션과 Domain에서 의도적으로 발생시키는 비즈니스 오류는 `CoreException`으로 표현합니다.
- 기능별 `ResponseCode`, `CartItemException` 같은 별도 오류 체계는 만들지 않습니다.
- 오류 응답에는 비밀번호·토큰·키·외부 응답 원문 등 민감한 값을 담지 않습니다.

## 2. 계층별 책임
### API 경계

Bean Validation으로 요청 형식·필수값·범위를 검증합니다. `GlobalExceptionHandler`는 검증 실패를 공통 `INVALID_REQUEST` 응답으로 변환합니다.

### Application·Domain

업무 전제조건과 Domain 불변식을 위반하면 적절한 `ErrorCode`를 가진 `CoreException`을 발생시킵니다.
예를 들어 장바구니 상품 수가 한도를 초과하면 `CART_ITEM_LIMIT_EXCEEDED`, 상품 재고가 요청 수량보다 부족하면 `INSUFFICIENT_PRODUCT_STOCK`을 사용합니다.

### Repository·Infrastructure

`DataAccessException`, `PersistenceException` 같은 저장소 기술 예외를 모든 Repository에서 일괄 catch하지 않습니다.
기술 예외는 기본적으로 Service·UseCase와 전역 예외 처리기로 전파합니다.

다음 경우에만 별도 변환을 검토합니다.

- 기술 예외가 명확한 비즈니스 의미를 가질 때
- 재시도·대체·충돌 처리처럼 호출자가 다른 동작을 해야 할 때
- 외부 SDK 예외를 애플리케이션의 안정적인 Port 계약으로 바꿔야 할 때

단순히 저장 실패라는 이유만으로 `STORAGE_FAILURE` 같은 포괄 오류를 추가하지 않습니다.

### 전역 HTTP 처리

`common/exception/GlobalExceptionHandler`가 `CoreException`, 요청 검증 예외, 예상하지 못한 예외를 HTTP 응답으로 변환합니다.
예상하지 못한 예외의 상세 메시지와 Stack Trace를 클라이언트에 노출하지 않습니다.

## 3. 응답 계약

- 성공 응답은 `ApiResponse<T>`를 사용합니다.
- 오류 응답은 `ErrorResponse`의 `success`, `code`, `message`, `traceId`, 선택적 `data`를 사용합니다.
- `data`에는 클라이언트에 공개해도 안전한 부가 정보만 담습니다.
- 로그에는 원인 파악에 필요한 서버 내부 정보를 남길 수 있지만 인증정보와 외부 응답 원문은 남기지 않습니다.

## 4. 테스트 기준

- Domain·Application 테스트는 의도한 `CoreException`의 `ErrorCode`를 확인합니다.
- Controller 테스트는 HTTP 상태·오류 코드·민감 정보 비노출을 확인합니다.
- Repository 테스트는 기술 예외를 임의의 비즈니스 오류로 바꾸지 않고 전파하는지 확인합니다.
- 전역 예외 처리 테스트는 공통 응답 형식과 `traceId`를 확인합니다.
