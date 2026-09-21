# 상품 카테고리 규칙

## 현재 적용

- 카테고리 목록 API는 공개 `GET /api/v1/categories`다.
- 요청 파라미터는 API 명세서에 정의되어 있지 않다.
- 응답 카테고리는 `id`, `name`, `path`를 제공한다.
- 카테고리 계층은 `book_category.path` 문자열로 표현하며 별도 `parent_id`는 사용하지 않는다.
- 활성 카테고리는 `book_category.status = ACTIVE`인 행이다.
- HTTP 응답 외피는 프로젝트의 `ApiResponse` 계약(`success`, `data`)을 따른다.

## 확인 필요

- API 명세서 예시의 `result`·`error` 외피와 프로젝트 `ApiResponse` 계약이 다르다.
  현재 구현은 프로젝트 공통 계약을 우선한다.
- API 명세서와 도메인 정의서에는 카테고리 필터·정렬·페이지네이션 조건이
  구체적으로 정의되어 있지 않다.
- `BaseEntity` 상속으로 추가된 `book_category.status`가 현재 ERD에 표시되어 있지 않다. ERD 반영 여부는 확인 필요하다.
- ERD의 `product_category`는 `items`/`products`, `item_id`/`product_id` 표기가
  일치하지 않는다. 카테고리 목록 API에서는 해당 연결 테이블을 조회하지 않는다.
