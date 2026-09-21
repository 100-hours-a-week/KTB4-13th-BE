# 상품 목록 조회 규칙

## 현재 적용

- 상품 목록 API는 공개 `GET /api/v1/items`다.
- 회원 인증과 `userId`는 사용하지 않는다.
- `categoryId`, `sort`, `cursor`, `limit`을 선택적 Query Parameter로 받는다.
- 조회 대상은 활성 상품과 활성 도서이며, 카테고리 필터를 사용할 때 활성 카테고리와 활성 연결만 포함한다.
- 정렬은 API 명세서의 `createdAt DESC, id DESC`를 고정 적용한다.
- `cursor`는 마지막 상품 식별자인 양의 정수 문자열로 해석하며, 다음 조회는 `product.id < cursor` 조건을 사용한다.
- `limit`이 없으면 임시로 20개를 사용한다. 응답의 다음 커서는 실제 다음 데이터가 있을 때 마지막 `itemId`를 문자열로 반환한다.
- HTTP 응답 외피는 프로젝트의 `ApiResponse` 계약(`success`, `data`)을 따른다.
- 리뷰·주문 테이블이 현재 Flyway에 없으므로 `orderCount`, `reviewCount`, `reviewRate`는 0으로 반환한다.
- 상품 API의 외부 응답 필드인 `itemId`, `itemName`은 현재 도메인 `Product`의 식별자와 이름에 매핑한다.

## ERD·문서 비교와 확인 필요

- ERD와 도메인 정의서의 연결 대상이 `items`/`products`, 식별자 컬럼이 `item_id`/`product_id`로 일치하지 않는다. 현재 프로젝트의 실제 테이블과 도메인에 맞춰 `products.id`를 사용했다.
- ERD는 `deleted_at`과 `active_flag`를 제시하지만 프로젝트 Flyway와 `BaseEntity`는 `status`를 사용한다. 연결 테이블도 프로젝트 규칙에 맞춰 `status = ACTIVE`를 적용했다.
- API 명세서의 응답 예시는 `result`·`error` 외피를 사용하지만 프로젝트 공통 `ApiResponse`는 `success`·`data`를 사용한다. 기존 프로젝트 계약을 우선했다.
- API 명세서의 `sort` 설명은 상품 식별자로 되어 있고 유효한 값·기본값은 정의되어 있지 않다. 현재는 입력을 받되 명세서의 고정 정렬만 적용한다.
- API 명세서에 `limit` 기본값·최댓값과 cursor 형식이 정의되어 있지 않다. 현재 구현의 기본값 20과 ID 문자열 cursor는 확인 필요 항목이다.
- API 명세서는 `thumbnailUrl`을 NOT NULL로 정의하지만 실제 `products.thumbnail_url`은 NULL을 허용한다. 이미지 대체 정책은 확인 필요다.
