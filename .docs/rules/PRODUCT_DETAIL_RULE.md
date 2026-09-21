# 상품 상세 조회 규칙

## 현재 적용

- 상품 상세 조회 API는 공개 `GET /api/v1/products/{productId}`다.
- 상품 상세 API의 경로와 응답 식별자는 모두 `productId`를 사용한다.
- 활성 상품과 상품이 참조하는 활성 도서만 조회한다. 활성 상태는 `status = ACTIVE`다.
- 상품을 찾을 수 없거나 활성 상태가 아니면 E404 `상품을 찾을 수 없습니다.`를 반환한다.
- 상품은 `products.book_id`로 `books.id`를 참조한다.
- HTTP 응답 외피는 프로젝트의 `ApiResponse` 계약(`success`, `data`)을 따른다.
- 현재 Flyway에 리뷰·쿠폰 테이블이 없으므로 상세 응답의 `reviewCount`와 `reviewRate`는 0, `coupons`는 빈 목록으로 반환한다.

## 프로젝트 결정

- API 명세서의 경로와 Path Params·응답 식별자 표기가 일치하지 않지만, 프로젝트 API에서는 모두 `productId`로 통일한다.
- ERD의 `books`·`products`에는 `deleted_at`이 있으나 프로젝트 공통 생명주기는 `status`를 사용한다. 이번 기능은 `status = ACTIVE`만 조회한다.
- ERD의 `status` 길이는 `VARCHAR(10)`이지만 현재 Flyway의 상태 컬럼은 `VARCHAR(16)`이다. 이번 마이그레이션은 기존 Flyway 형식을 따른다.

## 확인 필요

- API 응답의 `coupons` 항목은 `coupon`과 `owned_coupon`의 필드를 함께 요구하지만 공개 API에 `userId`가 없고 현재 쿠폰 저장·조회 기능이 없다. 쿠폰 상태·사용 횟수·대상 연결을 확정한 뒤 조회를 추가해야 한다.
- ERD와 도메인 정의서의 `product_category` 연결 대상 표기가 `items`/`products`, `item_id`/`product_id`로 일치하지 않는다. 상품 상세 조회에서는 카테고리 연결을 조회하지 않는다.
- API 명세서는 상품 상세 응답의 `thumbnailUrl`을 NOT NULL로 정의하지만 ERD의 `products.thumbnail_url`과 `books.cover_image_url`은 NULL을 허용한다. 현재 구현의 `null` 반환과 이미지 fallback 정책은 문서 간 불일치로, 응답 계약과 이미지 정책을 확정해야 한다.
- API 명세서의 E400 메시지(`요청이 올바르지 않습니다.`)와 기존 `ErrorCode.INVALID_REQUEST` 메시지(`요청 형식이 올바르지 않습니다.`)가 다르므로 기존 공통 오류 계약을 우선 적용했다.
