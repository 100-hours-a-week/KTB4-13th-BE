# 다중 필터 조회의 QueryDSL 적용 기준

새 조회는 Spring Data JPA로 시작합니다. 선택 조건, 조인, 정렬, 커서가 서로 영향을 주어 조회 계약을 한 JPQL 문장으로 유지하기 어려울 때 QueryDSL `QueryRepository`를 검토합니다. 구현이 달라져도 Port와 Adapter의 호출 계약은 유지합니다. [아키텍처 기준](../.docs/ARCHITECTURE.md)

## 현재 조회 비교

| 조회 | 실제 구현과 테스트 근거 | 판단 |
| --- | --- | --- |
| 상품 목록 | 카테고리는 `EXISTS`, 출간일은 선택 범위 조건으로 적용합니다. 생성일순은 활성 상품과 도서를 조회하며, 인기순은 스냅샷에서 순위를 제한한 뒤 상품을 읽어 순서를 복원합니다. 통합 테스트는 필터 조합, 커서, 스냅샷 갱신을 검증합니다. [`ProductQueryRepository`](../src/main/java/com/book/core/product/infrastructure/persistence/repository/ProductQueryRepository.java), [`ProductRepositoryIntegrationTest`](../src/test/java/com/book/core/product/infrastructure/persistence/repository/ProductRepositoryIntegrationTest.java) | QueryDSL 유지. 정렬별 쿼리와 조건 조합을 한곳에서 관리합니다. |
| 결제 이력 주문 목록 | JPQL은 사용자·삭제 여부·결제 이력을 공통 조건으로 적용합니다. 상태·결제일 범위·커서는 `IS NULL OR`로 선택 적용하고, 정렬은 결제 시각과 ID 내림차순으로 고정합니다. Adapter가 커서를 파라미터로 풀어 전달합니다. 통합 테스트는 날짜 경계, 동률 커서와 주문·주문상품 조회 SQL 2회를 검증합니다. [`OrderJpaRepository`](../src/main/java/com/book/core/order/infrastructure/persistence/repository/OrderJpaRepository.java), [`OrderRepositoryAdapter`](../src/main/java/com/book/core/order/infrastructure/persistence/repository/OrderRepositoryAdapter.java), [`GetOrdersIntegrationTest`](../src/test/java/com/book/core/order/application/GetOrdersIntegrationTest.java) | 현재 JPQL 유지. 선택 조건과 정렬 조합이 늘거나, 실제 MySQL에서 선택 조건 때문에 스캔 비용이 커지는 경우 우선 비교할 후보입니다. |
| 사용 가능 쿠폰 | 쿠폰 조인과 사용자·발급 상태·삭제 여부·만료 조건을 공통으로 쓰고, 특정 쿠폰에 한정할 때 ID 목록을 추가합니다. 통합 테스트는 체크아웃 후보가 할인액과 사용 횟수로 미리 걸러지지 않는 계약을 확인합니다. [`OwnedCouponQueryRepository`](../src/main/java/com/book/core/coupon/infrastructure/persistence/repository/OwnedCouponQueryRepository.java), [`OwnedCouponRepositoryIntegrationTest`](../src/test/java/com/book/core/coupon/infrastructure/persistence/repository/OwnedCouponRepositoryIntegrationTest.java) | QueryDSL 유지. 두 조회가 같은 조건을 재사용합니다. |
| 대상별 리뷰 | 대상 유형·ID·삭제 여부로 좁히고, 결과에 필요한 필드를 프로젝션합니다. ID 역순으로 `offset`부터 `limit + 1`건을 읽어 다음 페이지 존재 여부를 판정합니다. 통합 테스트는 대상·삭제·페이지 조건을 검증합니다. [`ReviewQueryRepository`](../src/main/java/com/book/core/review/infrastructure/persistence/repository/ReviewQueryRepository.java), [`ReviewRepositoryIntegrationTest`](../src/test/java/com/book/core/review/infrastructure/persistence/repository/ReviewRepositoryIntegrationTest.java) | QueryDSL 유지. 고정 필터라도 프로젝션과 조회 결과 조립을 함께 관리합니다. |
| 최근 알림 | 사용자와 활성 여부를 고정 조건으로 쓰고 ID 역순 최대 20건을 파생 쿼리로 조회합니다. 통합 테스트는 최근 20건과 미읽음 집계의 계약을 확인합니다. [`NotificationJpaRepository`](../src/main/java/com/book/core/notification/infrastructure/persistence/repository/NotificationJpaRepository.java), [`NotificationRepositoryIntegrationTest`](../src/test/java/com/book/core/notification/infrastructure/persistence/repository/NotificationRepositoryIntegrationTest.java) | 파생 쿼리 유지. 조건과 순서가 메서드 이름에 드러납니다. |

## 새 조회나 변경을 검토할 때

1. 조건과 정렬이 고정이면 파생 쿼리를 사용합니다. 고정 조인이나 서브쿼리를 선언형으로 읽기 쉬우면 JPQL을 사용합니다.
2. 선택 조건에 따라 조인, 프로젝션, 정렬, 커서 조건까지 달라지거나 여러 조회에서 같은 조건을 조합해야 하면 QueryDSL을 검토합니다. 선택 조건의 개수만으로 판단하지 않습니다.
3. 성능이 전환 이유라면 대표적인 필터 조합과 값 분포에서 실제 생성 SQL, 실행계획, 스캔 행 수, 정렬, SQL 호출 수를 비교합니다. 첫 페이지와 커서 다음 페이지를 각각 확인하고, 조건 누락·동률·조인 중복이 생기지 않는지 통합 테스트로 고정합니다.

이번 검토에서는 코드와 기존 테스트가 확인하는 동작을 대조했습니다. 주문 목록의 MySQL 실행계획과 전환 전후 성능은 측정하지 않았습니다.
