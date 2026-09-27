# 상품 목록 조회 규칙

## 현재 적용

- 상품 목록 API는 공개 `GET /api/v1/items`다.
- 회원 인증과 `userId`는 사용하지 않는다.
- `categoryId`, `sort`, `cursor`, `limit`을 선택적 Query Parameter로 받는다.
- 조회 대상은 활성 상품과 활성 도서이며, 카테고리 필터를 사용할 때 활성 카테고리와 활성 연결만 포함한다.
- `sort`는 `createdAt`과 `POPULARITY`를 지원한다. 생략하면 `createdAt`이며, `createdAt`은 `(createdAt DESC, id DESC)` 순으로 정렬한다.
- `POPULARITY`는 전체 기간의 결제된 주문 상품 수량 합계 DESC, 활성 리뷰 수 DESC, 활성 리뷰 평균 평점 DESC, 상품 ID DESC 순으로 정렬한다. `order_item.status = PAID`인 활성 주문 상품만 판매 수량에 포함하고, 삭제된 주문·주문 상품·리뷰는 집계에서 제외한다. 인기 데이터가 없는 상품은 판매 수량·리뷰 수·평점을 0으로 취급한다.
- 정렬 입력은 대소문자를 구분하며 허용 값 외에는 `E400`을 반환한다. 재고는 정렬 점수에 반영하지 않는다.
- `createdAt` cursor는 마지막 상품 ID인 양의 정수 문자열이며, 해당 상품의 `createdAt`을 사용해 복합 커서 조건을 적용한다. `POPULARITY` cursor는 불투명한 Base64 URL-safe 값이며 판매 수량·리뷰 수·평균 평점·상품 ID를 포함한다. 두 정렬 간 cursor 형식은 호환되지 않는다.
- `limit`이 없으면 20개를 사용한다. 다음 페이지가 있으면 응답의 `nextCursor`에 마지막 상품의 cursor를 반환한다. 집계 값이 페이지 조회 사이에 변하면 인기순 페이지 경계는 최선 노력으로 유지된다.
- HTTP 응답 외피는 프로젝트의 `ApiResponse` 계약(`success`, `data`)을 따른다.
- 목록 응답의 기존 `orderCount`, `reviewCount`, `reviewRate` 필드는 이번 정렬 기능에서 변경하지 않고 0을 반환한다. 인기 정렬에 사용하는 집계는 정렬과 cursor 계산에만 사용한다.
- 상품 API의 외부 응답 필드인 `itemId`, `itemName`은 현재 도메인 `Product`의 식별자와 이름에 매핑한다.

## ERD·문서 비교와 확인 필요

- ERD와 도메인 정의서의 연결 대상이 `items`/`products`, 식별자 컬럼이 `item_id`/`product_id`로 일치하지 않는다. 현재 프로젝트의 실제 테이블과 도메인에 맞춰 `products.id`를 사용했다.
- 공통 삭제 생명주기는 `deleted_at IS NULL`로 판별한다. V8 마이그레이션은 상품·도서·카테고리·연결 테이블의 삭제 전용 `status`를 없애고, `product_category`의 활성 연결 UNIQUE 제약은 `active_flag`로 유지한다.
- API 명세서의 응답 예시는 `result`·`error` 외피를 사용하지만 프로젝트 공통 `ApiResponse`는 `success`·`data`를 사용한다. 기존 프로젝트 계약을 우선했다.
- 기존 API 명세서의 상품 목록 설명에는 `sort` 허용 값·기본값과 인기 집계·정렬 cursor 계약이 없었다. `limit` 최댓값도 정의되어 있지 않다.
- API 명세서는 `thumbnailUrl`을 NOT NULL로 정의하지만 실제 `products.thumbnail_url`은 NULL을 허용한다. 이미지 대체 정책은 확인 필요다.
