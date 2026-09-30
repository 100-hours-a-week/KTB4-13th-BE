# 인기순 조회 수정 전후 기록 — Issue #203

쿼리 측정 기준: 2026-09-30, `main`의 `46dd28a`. 최종 결합 검증 기준: `main`의 `1c3c3b0`(인기순 쿼리/스키마 동일). 관련 작업: BE #106, 기존 정렬 계약 확인: BE #154.

## As-Is와 원인

`ProductQueryRepository.findByPopularity()`는 products, books 전체 데이터와 스냅샷을 조인한 뒤
`COALESCE(s.sales_quantity, 0) DESC, p.id DESC`로 정렬했다. V15 색인은
`(sales_quantity DESC, review_count DESC, review_rate DESC, product_id DESC)`이므로 현재 판매 수량/ID 순서와 맞지 않았다.

실제 MySQL 실행계획은 필터 없는 첫/다음 페이지에서 **books 261만 행 스캔 → products와 snapshot 조회 → filesort → LIMIT 21**이었다.
제보의 “products부터 읽는다”와 실제 옵티마이저의 조인 순서는 달랐지만, LIMIT 전에 전체 후보를 합치는 비용은 확인했다.
분류가 있으면 분류 색인으로 후보를 좁혔다. 모든 요청이 261만 행을 정렬한다는 설명은 성립하지 않는다.

약 261만 상품, 수십~수백 초 응답, 커넥션 10개 점유는 사용자가 전달한 제보이다.
제보자는 DB에서 직접 실행하지 않았다고 밝혔다. 이 문서의 합성 SQL 실측은 운영 API 지연이나 커넥션 점유를 증명하지 않는다.

스냅샷 갱신은 이미 `RefreshProductPopularitySnapshotUseCase`의 `@Transactional` 안에서 DELETE와 INSERT SELECT를 수행한다.
기존 실패 롤백 테스트도 있다. DELETE와 INSERT 사이 모든 판매 수량이 0으로 보인다는 가설은 현재 구현의 결함으로 확인되지 않았다.
이번 측정 MySQL의 격리 수준은 REPEATABLE-READ이다. 운영 격리 수준은 확인하지 않았다.

## To-Be

- 스냅샷의 판매 수량과 product_id로 정렬하고 커서를 적용한다. NOT NULL 지표에 COALESCE를 사용하지 않는다.
- 활성 상품/도서와 출간일 조건은 scalar subquery로, 활성 분류/연결은 기존 EXISTS 조건으로 LIMIT 전에 적용한다.
- 먼저 제한한 ID/지표만 읽고, 선택된 상품/도서를 두 번째 조회에서 fetch join한다. 결과는 첫 조회의 랭킹 순서로 조립한다.
- 두 조회는 기존 `GetProductsUseCase.execute()` 트랜잭션 안에서 실행된다. 빈 후보면 두 번째 조회를 실행하지 않는다.
- V15를 수정하지 않고 V22로 기존 정렬 색인을 `(sales_quantity DESC, product_id DESC)`로 교체한다.
- **사용자 승인 정책:** 스냅샷 갱신 이후 등록된 상품은 다음 성공 갱신부터 인기순에 포함된다. 생성일순 조회에는 바로 포함된다.
- 전체 기간 PAID 수량 DESC/productId DESC와 두 필드 cursor 형식은 유지한다. 리뷰 지표는 순서에 추가하지 않는다.

단순 INNER JOIN과 새 색인만 적용한 중간안도 실제로 측정했다. MySQL이 books부터 조인해 정렬하는 계획을 유지했고,
첫 페이지 중앙값은 11,871.42ms였다. 이 안을 최종 수정으로 채택하지 않았다. 원시는 [inner-join-attempt.json](inner-join-attempt.json)에 보관한다.

## 측정 환경과 방법

- 격리된 로컬 `mysql:8.4.8`, Colima VM 8 CPU / 10,403,426,304 bytes RAM, InnoDB buffer pool 512MiB.
- 실제 V1–V21 마이그레이션 적용. products/books/snapshots 각 2,610,000행, 주문/리뷰 0행.
- 모든 상품/도서가 활성이다. 상품 ID가 1,000의 배수일 때만 `ID % 17 + 1` 판매 수량, 나머지는 0.
- 큰 분류 261,000행, 작은 분류 26행. 출간일은 ID 짝수 2026-01-01, 홀수 2020-01-01.
- `LIMIT 21`: API가 기본 20개와 다음 페이지 존재 확인용 1개를 요청하는 크기.
- 시나리오별 warm-up 1회 후 3회 실행한 중앙값. SQL 클라이언트 프로세스/`docker exec` IPC 비용을 포함한다.
- After는 랭킹과 상품/도서 hydration **두 SQL 호출을 모두 포함**한다. Before는 한 호출이다.
- EXPLAIN과 EXPLAIN ANALYZE는 별도 실행했다. ANALYZE의 최상위 시간은 대표 1회이며 중앙값과 구분한다.
- Before/After의 5개 시나리오에서 반환 ID와 순서를 assertion으로 비교했다. 새 상품 제외 차이는 별도 통합 테스트로 확인한다.

## Before / After

| 시나리오 | Before 중앙값(ms) | After 중앙값(ms) | 관찰 |
| --- | ---: | ---: | --- |
| 첫 페이지 | 11,366.03 | 143.94 | 약 79배 단축, 전체 조인/정렬 제거 |
| 커서 다음 페이지 | 11,881.29 | 140.43 | 약 85배 단축, 복합 색인 range scan |
| 큰 분류(261,000개) | 1,494.37 | 1,522.82 | 개선 없음, 분류 후보 전체 정렬 유지 |
| 작은 분류(26개) | 66.43 | 139.48 | CLI 호출 증가, DB 실행은 1ms 미만 |
| 출간일 범위 | 7,295.59 | 129.07 | 약 57배 단축, 이 데이터는 상위 후보가 범위에 포함 |

첫 페이지 EXPLAIN ANALYZE의 실제 입력은 Before 2,610,000행, After 스냅샷 색인 21행이다.
대표 랭킹 SQL DB 시간은 첫 페이지 13,282ms → 0.363ms, 커서 페이지 13,765ms → 0.832ms이다.
After의 이 DB 시간은 첫 단계만 포함한다. 위 중앙값 표는 두 단계와 CLI/IPC를 포함하므로 둘을 같은 지표로 비교하면 안 된다.
작은 분류의 대표 DB 랭킹 시간은 0.557ms → 0.397ms이다. 운영 API가 이 속도를 달성했다고 해석하지 않는다.

원시 실행 시간, SQL, 반환 ID, 실행계획은 [before.json](before.json), [after.json](after.json)에 있다.

## 갱신 비용과 남은 문제

실제 repository의 INSERT SELECT 문자열을 읽어, DELETE와 함께 한 트랜잭션으로 커밋했다.
동일한 합성 순위를 복원하며 각 색인에서 한 번씩 실행했다. 색인 변경 DDL/순위 복원은 아래 시간에 포함하지 않았다.

| 전체 갱신 | Before(ms) | After(ms) |
| --- | ---: | ---: |
| 261만 행 DELETE + INSERT SELECT + COMMIT, 각 1회 | 25,527.68 | 25,140.96 |

원시 결과: [refresh.json](refresh.json). 차이는 반복 측정으로 검증하지 않았으므로 갱신 개선으로 주장하지 않는다.
주문/리뷰가 없는 환경에서조차 약 25초가 걸렸다. 이번 변경은 전체 재작성 비용을 해결하지 않는다.

큰 분류의 filesort는 남는다. 작은 분류를 무조건 랭킹 순서로 훑도록 강제하면 희소 분류에서 더 느려질 수 있어 강제 힌트/분류별 정책은 추가하지 않았다.
출간일이나 활성 조건이 드물면 충분한 결과를 모을 때까지 많은 랭킹 후보를 훑을 수 있다.
판매 수량/분류/출간일/삭제 비율이 다른 운영 분포와 다른 optimizer 설정에서는 실행계획을 다시 확인해야 한다.
`subquery_to_derived` 설정 등 MySQL 옵티마이저 변경도 재검증 대상이다.

증분 갱신, 실행 중 동시 요청의 락 대기/커넥션 풀 포화, HTTP 부하 테스트, 운영/스테이징 실측은 이번에 수행하지 않았다.
증분 갱신은 취소/삭제/리뷰 변경을 빠짐없이 추적하는 후속 계약이 필요하다.
V22 적용은 261만 행 색인 DDL을 수반하므로 운영 적용 시간과 메타데이터 락 영향도 운영에서 별도 확인해야 한다.

## 재현

새 작업 전용 컨테이너에서 실행한다. 기존 데이터베이스는 seed 단계에서 거부한다.
SQL에 한글 seed가 있으므로 UTF-8 클라이언트를 사용한다. 로컬 합성 DB에만 인증 없는 설정을 사용한다.

```bash
docker run -d --name popularity-bench -e MYSQL_ALLOW_EMPTY_PASSWORD=yes \
  -e MYSQL_DATABASE=popularity_bench mysql:8.4.8 --innodb-buffer-pool-size=512M
# MySQL이 준비된 뒤 저장소 루트에서 실행
python3 performance/popularity/benchmark.py --container popularity-bench --seed
python3 performance/popularity/benchmark.py --container popularity-bench --phase before --output /tmp/before.json
python3 performance/popularity/benchmark.py --container popularity-bench --phase after --baseline /tmp/before.json --output /tmp/after.json
python3 performance/popularity/benchmark.py --container popularity-bench --refresh --output /tmp/refresh.json
docker stop popularity-bench
```

`--refresh`는 쿼리 비교를 마친 뒤 실행한다. 두 색인으로 전체 갱신을 각각 수행하고 최종적으로 V22 형태의 색인과 합성 판매 순위를 복원한다.

## 검증

- **로컬 코드 검증:** macOS 26.6.2 arm64, Amazon Corretto OpenJDK 25.0.4.1, Gradle Wrapper 9.7.1. Docker/Testcontainers는 Colima Docker 29.5.2 (8 CPU, 10,403,426,304 bytes RAM)를 사용했고, 통합 테스트 DB 이미지는 MySQL 8.4.8이다.
- **GitHub Actions:** `ubuntu-latest`, Eclipse Temurin 25, Gradle Wrapper 9.7.1. CI의 `Gradle check`가 Testcontainers MySQL 8.4.8을 포함해 통과했다.
- **성능 측정:** 같은 Colima Docker 자원에서 별도 MySQL 8.4.8 컨테이너를 사용했다. DB buffer pool은 512MiB이며, 합성 데이터/주문·리뷰 조건은 위 측정 환경 절에 기록했다. 운영 API 측정과는 별개다.
- 신규 상품의 갱신 전 제외/갱신 후 0 판매량 포함/0 판매량 ID 커서 테스트를 추가했다.
- 기존 활성 조건, 분류, 출간일, 인기순 커서, refresh 실패 롤백 테스트를 유지했다.
- Standards 리뷰: 기준 위반/차단 발견 0건. Spec 리뷰: 확정 결함/범위 확장 0건. 리뷰는 실행 증거와 별개다.
- `./gradlew check --no-daemon` 성공: 단위/Controller/아키텍처 546개, MySQL 통합 88개, 실패/생략 0개. JaCoCo coverage/Java 줄 길이/Spotless 포함.
- 별도 Spotless 검사, `git diff --check`, benchmark Python compile 성공.

## 참고

[MySQL scalar subquery](https://dev.mysql.com/doc/refman/8.4/en/scalar-subqueries.html)는 결과가 없을 때 NULL을 반환하므로,
활성 상품을 못 찾는 후보는 ID 동등 조건을 만족하지 않는다.
[MySQL semijoin 최적화](https://dev.mysql.com/doc/refman/8.4/en/semijoins-antijoins.html)는 EXISTS 후보의 조인 순서를 바꿀 수 있다.
따라서 FROM에 스냅샷을 먼저 적었다는 사실만으로 랭킹 색인 순서 읽기를 보장하지 않는다.
