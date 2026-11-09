# Book 쿠폰 발급 Redis Streams 로컬 실험

[이슈 #530](https://github.com/100-hours-a-week/KTB4-13th-BE/issues/530)

## 기준과 참고 자료

- Book 동기 기준은 원격 `main`의 `ec7800306f85d38d951a8cd56daee26f170d3f1b`입니다. 시작 브랜치 `feat/210-coupon-stock-reconciliation`의 `b7bc6553`은 공통 조상 이후 대사 구현과 QueryDSL 변경 두 커밋이 있었고, 원격 `main`에는 대사 구현의 병합 커밋 `ec780030`이 있었습니다. 연결 브랜치 `feat/530-coupon-redis-streams`는 이 원격 기준에서 시작했습니다.
- 참고 Part 3-1은 검증, Redis 차감, MySQL 수량 갱신과 발급 저장을 한 요청에서 처리합니다. Part 3-2c는 Redis Lua로 재고와 중복을 확인하고 Kafka 이벤트를 보냅니다. 소비자가 MySQL 트랜잭션에서 발급을 기록합니다. [동기 브랜치](https://github.com/apieceofcoding/coupon-java/tree/5a7a77555857762e1f012e9e998edfd6a6aa72c0) · [Kafka 브랜치](https://github.com/apieceofcoding/coupon-java/tree/ee6973c5601eac17db9982edee347d2aa889763a)
- 올리브영은 Pub/Sub를 여러 워커가 구독할 때 같은 이벤트가 여러 번 처리됐고, 워커를 한 곳으로 지정한 뒤에도 메시지가 전달되지 않아 미발급이 발생했다고 설명합니다. 이후 발급 데이터를 워커별 Redis List에 넣었고, 후속 글에서 행사 트래픽 문제와 기능 확장 요구를 RabbitMQ 도입 배경으로 들었습니다. Book에서는 Streams의 pending 회수와 재전달을 시험했습니다. 올리브영의 Pub/Sub 중복 구독이나 Redis List 운영 장애는 재현하지 않았습니다. [올리브영 Redis 사례](https://oliveyoung.tech/2023-08-07/async-process-of-coupon-issuance-using-redis/) · [올리브영 RabbitMQ 전환 사례](https://oliveyoung.tech/2023-09-18/oliveyoung-coupon-rabbit/)

## 구현과 응답 계약

- 기본 모드의 `POST /api/v1/coupons/{couponId}/issue`는 MySQL 저장이 끝나면 `200`을 반환합니다. 실험 모드 `local,coupon-streams`는 Redis 접수 시 `202`를 반환합니다. 기존 쿠폰함 조회는 커밋된 행을 읽으므로 접수와 쿠폰함 노출 사이에 시간이 생깁니다.
- 두 모드 모두 매진 신호, 사용자별 제한, 정책 조회, 기간과 기존 발급 검증을 거칩니다. 실험에서는 Redis에 접수된 사용자도 DB 반영 전 중복으로 판정합니다. 같은 요청의 재시도는 중복이나 매진으로 끝날 수 있고 동일한 `202`를 재생하지 않습니다.
- Redis 단일 노드에서 Lua가 재고 차감, 사용자 예약, 마지막 재고의 매진 신호, `XADD`를 한 번에 처리합니다. Lua에서 이후 명령이 실패해도 앞선 변경은 자동 롤백되지 않으므로, 처리 가능한 오류에서는 이미 변경한 재고·예약·매진 신호를 스크립트 안에서 되돌립니다. [Redis Lua 문서](https://redis.io/docs/latest/develop/programmability/eval-intro/)
- `coupon-streams-worker` 프로필을 더하면 워커가 하나의 Consumer Group에서 메시지를 읽습니다. 기존 DB 수량 제한과 사용자·쿠폰 고유 제약을 이용해 발급 수량과 보유 쿠폰을 같은 트랜잭션에서 저장합니다. DB 커밋 뒤 `XACK`하며, 커밋 실패 시에는 pending 상태로 남겨 `XAUTOCLAIM`으로 회수합니다. 이미 커밋된 발급은 수량이나 행을 다시 늘리지 않습니다. [XACK](https://redis.io/docs/latest/commands/xack/) · [XAUTOCLAIM](https://redis.io/docs/latest/commands/xautoclaim/)

## 재현 명령

Book 저장소 루트에서 Java 25, k6, Python 3, Docker Desktop을 사용합니다. 예시는 처음 만드는 테스트 컨테이너만 사용하고 로컬 환경 파일을 읽지 않습니다.

```bash
set -euo pipefail
out="$(mktemp -d /tmp/book-coupon-streams.XXXXXX)"
mkdir "$out/baseline"
git archive ec7800306f85d38d951a8cd56daee26f170d3f1b | tar -x -C "$out/baseline"
(cd "$out/baseline" && ./gradlew bootJar --offline)
cp "$out/baseline/build/libs/book.jar" "$out/sync.jar"
./gradlew bootJar --offline
cp build/libs/book.jar "$out/streams.jar"

suffix="${out##*.}"
export DB_CONTAINER="book-streams-db-$suffix" REDIS_CONTAINER="book-streams-redis-$suffix"
export DOCKER_CONTEXT=desktop-linux
docker --context "$DOCKER_CONTEXT" run -d --name "$DB_CONTAINER" \
  --label "book.test=streams-$suffix" \
  -e MYSQL_ROOT_PASSWORD=streams_test_root -e MYSQL_DATABASE=book \
  -e MYSQL_USER=book -e MYSQL_PASSWORD=streams_test -p 127.0.0.1::3306 mysql:8.4.8
docker --context "$DOCKER_CONTEXT" run -d --name "$REDIS_CONTAINER" \
  --label "book.test=streams-$suffix" -p 127.0.0.1::6379 redis:8.0 \
  redis-server --appendonly yes
trap 'docker --context "$DOCKER_CONTEXT" stop "$DB_CONTAINER" "$REDIS_CONTAINER"; docker --context "$DOCKER_CONTEXT" rm -v "$DB_CONTAINER" "$REDIS_CONTAINER"' EXIT
export DB_PORT="$(docker --context "$DOCKER_CONTEXT" port "$DB_CONTAINER" 3306/tcp | awk -F: '{print $NF}')"
export REDIS_PORT="$(docker --context "$DOCKER_CONTEXT" port "$REDIS_CONTAINER" 6379/tcp | awk -F: '{print $NF}')"
export JWT_SECRET="$(openssl rand -base64 64 | tr -d '\n')"

python3 experiments/coupon-streams/run.py "$out" | tee "$out/experiment.log"
```

기존 Flyway의 V66에 필요한 책 목록은 저장소의 테스트 fixture 마이그레이션으로 채웁니다. `run.py`는 격리 컨테이너의 라벨과 포트를 확인하고, 완료 후 자신이 시작한 앱 프로세스를 종료합니다. `perf.json`, `faults.json`, k6 로그와 원본 집계 JSON은 출력 디렉터리에 남습니다. `run.py "$out" perf`로 성능만 먼저 실행하고 같은 컨테이너에서 `run.py "$out" faults`로 장애 검증을 이어갈 수 있습니다.

## 측정 방법

- MySQL 8.4.8, Redis 8.0(AOF 활성), Hikari 최대 10, 동일 호스트와 컨테이너, 동일 JVM 옵션, 동일 요청 사용자 집합을 사용했습니다. 모드마다 신규 쿠폰으로 워밍업은 초당 200건 × 5초, 본 측정은 **초당 500건 × 15초**, 본 측정 쿠폰은 재고 20,000장입니다.
- API p99는 k6 HTTP 왕복 지연입니다. Hikari 평균 대기는 획득 시간 `sum/count`의 구간 증가분입니다. 연결 대기 최대와 큐 최대는 약 0.4초 간격 표본입니다. 큐 크기는 그룹의 미전달 `lag`와 미확인 `pending`의 합입니다.
- Streams 커밋 지연은 Redis 이벤트 ID의 접수 시각에서 워커의 DB 트랜잭션 반환 직후까지입니다. Prometheus 누적 버킷의 본 측정 전후 차이로 p99가 속한 구간을 구합니다. 동기 방식은 DB 커밋 뒤에만 성공 응답하므로 별도 접수→완료 구간은 없습니다. 해소 시간은 k6 종료 관측부터 `lag=pending=0`을 확인할 때까지입니다.
- Redis CPU는 테스트 Redis 프로세스의 `used_cpu_sys + used_cpu_user` 증가분이며, 명령 평균은 Redis 자체의 `INFO commandstats`를 사용했습니다. Streams 측정에는 큐 해소 구간이 포함됩니다.

## 본 측정 결과

| 항목 | Book 동기 | Streams, 워커 1개 |
| --- | ---: | ---: |
| 워밍업 완료 / 시작 누락 | 1,001 / 0 | 1,000 / 0 |
| 워밍업 API p99 | 892.54ms | 458.32ms |
| 본 측정 완료 / 시작 누락 | 7,025 / 476 | 7,500 / 0 |
| 본 측정 API p99 | 1,701.13ms, 저장 완료 | 73.75ms, 접수 완료 |
| 접수→DB 커밋 p99 구간 | 별도 접수 없음 | 25초 초과~30초 이하 |
| 요청 구간 Hikari 평균 획득 대기 | 217.54ms | 0.49ms |
| 큐 해소까지 포함한 Hikari 평균 획득 대기 | 217.54ms | 0.37ms |
| Hikari 연결 대기 표본 최대 | 191 | 0 |
| 적체 표본 최대 / k6 종료 뒤 해소 | 해당 없음 | 5,637건 / 24.83초 |
| Redis EVALSHA 서버 실행 평균 | 15.83µs | 32.87µs |
| Redis CPU 사용량 증가 | 1.20 CPU초 | 2.50 CPU초 |
| DB 발급 / 보유 행 / 서로 다른 사용자 | 7,025 / 7,025 / 7,025 | 7,500 / 7,500 / 7,500 |
| Redis 잔량 | 12,975 | 12,500 |

두 모드의 예상 밖 응답은 0건입니다. 동기 모드의 476건은 k6가 시작하지 못한 반복입니다. 같은 목표 도착률로 시험했지만 실제 완료량이 다르므로, 두 p99를 동일 처리량에서의 저장 성능 개선으로 해석할 수 없습니다. 워밍업 후 본 측정 각각 한 회차입니다.

Streams는 접수 API와 연결 획득 대기를 줄였으나, 단일 워커가 요청이 들어오는 속도만큼 DB에 반영하지 못했습니다. Streams의 `XADD`·`XACK` 서버 평균은 각각 6.92µs·5.61µs였고, Redis CPU 증가분에는 큐 해소 시간도 포함됩니다. 이 부하에서는 **Redis가 적체의 병목이라는 증거를 찾지 못했습니다.** Redis 내부의 평균 실행 시간으로 꼬리 지연, AOF 지연, 네트워크 대기까지 판단할 수 없으므로, Redis 포화 한계는 별도 측정이 필요합니다.

## 장애 검증

| 상황 | 확인한 동작 | 최종 DB 발급 / 보유 행 / Redis 잔량 |
| --- | --- | ---: |
| `XADD` 실패 | Stream 자리에 잘못된 Redis 타입을 넣어 실패시켰습니다. HTTP 500 뒤 재고 1장, 예약 없음, 매진 신호 없음이었고 정상 복구 후 같은 요청을 한 번 접수했습니다. | 1 / 1 / 0 |
| 클라이언트 응답 유실 | 워커가 꺼진 상태에서 응답을 읽기 전에 연결을 닫고 같은 사용자로 재요청했습니다. DB 반영 전에도 `E4001`을 반환했고, 추가 Stream 기록과 재고 차감은 없었습니다. | 1 / 1 / 1 |
| 워커 중단·재시작 | 워커가 중단된 상태에서 접수를 받았고 DB는 0건이었습니다. 워커를 다시 띄우자 대기 메시지가 저장됐습니다. | 1 / 1 / 0 |
| 일시적인 DB 저장 실패 | 테스트 CHECK 제약으로 보유 쿠폰 INSERT를 실패시켰습니다. 수량 갱신도 롤백되고 메시지는 pending에 남았습니다. 제약 제거 후 `XAUTOCLAIM`으로 저장했습니다. | 1 / 1 / 0 |
| 커밋 직후 ACK 전 중단 | 격리 Redis ACL에서 `XACK`을 잠시 차단했습니다. DB 1건과 pending 1건을 확인한 뒤 워커를 강제 종료했습니다. 재시작한 소비자가 회수·ACK하고 DB 행과 수량을 한 번으로 유지했습니다. | 1 / 1 / 0 |

각 최종 검증에서 DB 발급 수량, 보유 쿠폰 수, 고유 사용자 수가 일치했고 `총수량 = DB 발급 + Redis 잔량`도 만족했습니다. 검증한 접수에서 미발급, 중복 및 초과 발급은 0건이었습니다.

## 판단과 남은 조건

- **로컬 실험은 Redis Streams를 유지합니다.** 접수 속도와 워커 장애 후 재처리를 검증했습니다. 운영 채택은 예상 피크에서 큐가 목표 시간 안에 해소되고, Redis 재고 처리와 큐가 함께 동작해도 API 지연·메모리 여유가 유지되며, ACK 완료분 보존 기간과 반복 실패 처리 방법을 정할 수 있을 때 검토합니다.
- **500건/초에서 접수와 즉시 발급의 차이가 큽니다.** 목표가 수초 안에 쿠폰함에 반영하는 것이라면 이번 워커 구성으로 충족했다고 볼 수 없습니다. 아래 후속 실험에서 동일 쿠폰 행의 DB 쓰기 대기와 워커 동시 처리량을 비교했습니다. 브로커를 바꿔도 동일 DB 쓰기 용량이 늘어나는 것은 아닙니다.
- **별도 브로커 비교 조건**은 큐 적체가 Redis 재고 경로의 지연·메모리에도 영향을 주거나, 메시지 장애를 Redis 장애에서 분리해야 하거나, 보존·재생·재처리 운영 요건을 Streams로 감당하기 어려울 때입니다. 발급 작업 큐와 작업자 제어는 RabbitMQ, 장기 재생이나 여러 소비자 그룹은 Kafka의 비교 기준입니다. 별도 브로커를 쓰면 Redis 예약과 브로커 기록 사이의 유실을 별도 설계해야 합니다. [RabbitMQ ACK 및 발행 확인](https://www.rabbitmq.com/docs/confirms) · [Kafka 전달 의미](https://kafka.apache.org/42/design/design/#message-delivery-semantics)
- 동기·비동기 비교와 장애 검증은 단일 Redis 노드와 워커 한 개를 사용했습니다. Redis 장애·복제 승격, 메모리 고갈, 영구 실패의 격리와 장기 보존은 미검증입니다. 현재 재고 키와 공용 Stream 키를 함께 쓰는 Lua는 Redis Cluster의 같은 슬롯 요건을 고려해야 합니다. 동기·비동기 발급 노드가 한 재고를 섞어 처리하는 운영 전환도 검증 범위 밖입니다. [Redis Cluster 사양](https://redis.io/docs/latest/operate/oss_and_stack/reference/cluster-spec/) · [Redis 영속성](https://redis.io/docs/latest/management/persistence/)

## 코드와 실행 검증

- Book 루트에서 `./gradlew check --offline --continue`를 실행해 테스트 1,162건, 통합 테스트 217건, Spotless와 줄 길이 검사가 통과했습니다. 기본 커버리지 집계는 기존 Git 무시 대상 로컬 로깅 실험 클래스가 포함되어 78.74%였습니다. 파일을 그대로 둔 상태에서 커버리지 집계에서 해당 클래스만 제외해 같은 테스트 결과로 확인한 값은 80.65%입니다.
- 재현 명령의 `git archive` 기반 기준 JAR 빌드도 확인했습니다. 이전 회차의 DB 사용자 ID를 재사용하면 장애 주입용 CHECK 추가가 기존 데이터와 충돌하여, 재검증은 별도 사용자와 쿠폰 ID로 수행했습니다.
- 실험 앱 프로세스와 실험 전용 컨테이너·볼륨을 정리했습니다. 기존 Book Compose 프로젝트는 그대로 실행 중입니다.

## 워커 1·2·4개 후속 실험

참고 Part 3 스크립트에서 **고정 도착률 → 같은 JVM에서 워밍업과 본 측정 → 워커 완료 대기 → DB 수량 검증** 순서를 가져왔습니다. 참고 스크립트는 재고 5,000장에 초당 5,000건을 30초 동안 보내 매진 응답까지 포함합니다. Book에서는 DB 쓰기 구간을 비교하기 위해 본 측정 재고를 20,000장으로 두고, JWT가 다른 사용자로 초당 500건을 15초 동안 보냅니다. 워밍업은 이전 Book 실험과 같이 초당 200건을 5초 동안 보냈습니다. 회차마다 테스트 쿠폰을 새로 만들어 기존 데이터 초기화 없이 비교합니다. [참고 러너](https://github.com/apieceofcoding/coupon-java/blob/ee6973c5601eac17db9982edee347d2aa889763a/scripts/load/part-3/run.sh) · [참고 k6](https://github.com/apieceofcoding/coupon-java/blob/ee6973c5601eac17db9982edee347d2aa889763a/scripts/load/part-3/issue_burst.js) · [참고 DB 검증](https://github.com/apieceofcoding/coupon-java/blob/ee6973c5601eac17db9982edee347d2aa889763a/scripts/load/part-3/verify_burst.sh)

위 재현 명령에서 마지막 실행 줄을 아래처럼 바꿉니다. 이 비교에는 `streams.jar`만 필요합니다. 각 조건은 동일 JAR의 JVM 한 개와 Hikari 풀 10개를 사용합니다. 워커 수는 `coupon.streams.workers`로 설정하며, 각 워커는 같은 그룹 안에서 독립된 소비자 ID와 회수 커서를 가집니다.

```bash
python3 experiments/coupon-streams/run.py "$out" workers | tee "$out/workers.log"
```

본 실험은 한 쿠폰에 워커 1·2·4개를 비교합니다. 대조군은 워커 4개와 총 재고·요청률을 유지한 채, 요청을 네 쿠폰에 고르게 보냅니다. Redis 그룹의 미전달·미확인이 모두 0이 된 뒤 최종 수량을 확인합니다.

MySQL의 `Innodb_row_lock_waits`와 `Innodb_row_lock_time`은 본 측정 시작부터 큐 해소까지의 **격리 서버 전체** 증가분입니다. 락이 걸린 대상을 확인하기 위해 `performance_schema.data_lock_waits`와 `data_locks`를 연결해 본 측정 쿠폰의 `coupon.PRIMARY` 행을 기다린 샘플을 별도로 집계합니다. 짧은 대기는 샘플에 찍히지 않을 수 있습니다. [MySQL 행 락 대기 테이블](https://dev.mysql.com/doc/refman/8.4/en/performance-schema-data-lock-waits-table.html) · [MySQL 상태 변수](https://dev.mysql.com/doc/refman/8.4/en/server-status-variables.html#statvar_Innodb_row_lock_time)

### 워커별 본 측정 결과

| 지표 | 워커 1개 · 쿠폰 1개 | 워커 2개 · 쿠폰 1개 | 워커 4개 · 쿠폰 1개 | 워커 4개 · 쿠폰 4개 대조군 |
| --- | ---: | ---: | ---: | ---: |
| 접수 / k6 시작 누락 | 7,501 / 0 | 7,501 / 0 | 7,283 / 218 | 7,485 / 15 |
| 접수 API p99 | 40.71ms | 290.88ms | 1,306.55ms | 684.66ms |
| 접수 → DB 커밋 p99 구간 | (20, 25]초 | (8, 10]초 | (6, 8]초 | (5, 6]초 |
| 큐 최대 표본 / k6 종료 뒤 해소 | 5,640건 / 23.22초 | 3,600건 / 8.95초 | 3,922건 / 6.41초 | 2,630건 / 2.97초 |
| InnoDB 행 락 대기 횟수 / 누적 시간 | 0회 / 0ms | 3,254회 / 1,621ms | 7,089회 / 30,088ms | 1,611회 / 2,734ms |
| 대상 쿠폰 PK 대기 관측 표본 / 전체 | 0 / 95 | 0 / 53 | 35 / 44 | 6 / 34 |
| 대상 쿠폰 PK 동시 대기 표본 최대 | 0 | 0 | 3 | 1 |
| Hikari 평균 획득 대기 / 대기 표본 최대 | 0.16ms / 0 | 2.25ms / 0 | 35.38ms / 191 | 18.28ms / 188 |
| DB 발급 / 보유 쿠폰 / Redis 잔량 | 7,501 / 7,501 / 12,499 | 7,501 / 7,501 / 12,499 | 7,283 / 7,283 / 12,717 | 7,485 / 7,485 / 12,515 |
| 이미 저장된 메시지 재처리 / 워커 처리 실패 | 0 / 0 | 0 / 0 | 2 / 1 | 19 / 24 |

- 1개에서 2개로 늘리면 시작 누락 없이 커밋 p99 구간과 큐 해소 시간이 줄었습니다. 4개에서는 커밋이 더 빨랐지만, 접수 API의 지연과 시작 누락이 늘었습니다. 네 조건 모두 접수한 건은 전부 저장됐고 큐는 비었습니다.
- 4개가 같은 쿠폰에 쓸 때 MySQL은 해당 `coupon.PRIMARY` 레코드에서 최대 세 세션이 기다리는 모습을 보여줬습니다. 4개 워커로 쓰기 대상을 네 행에 분산하자 격리 DB의 누적 행 락 대기가 30.09초에서 2.73초로 줄고, 큐 해소도 6.41초에서 2.97초로 줄었습니다. **같은 쿠폰 행의 갱신 경합은 워커 4개에서 확인한 병목입니다.** 대조군에도 연결 대기와 시작 누락이 남았습니다.
- InnoDB 누적 락 시간은 세션 간 겹치는 대기를 합한 값입니다. 워커 2개에서 누적 대기는 있었으나 표본에서 대상 행을 기다리는 세션을 포착하지 못했습니다. 4개 조건의 커밋 지연은 **실제 접수 건 기준**입니다.
- 4개 조건에서는 DB 고유 제약 충돌이 관측됐습니다. 재처리 뒤 DB 발급·보유 행·고유 사용자 수는 접수와 일치했습니다. 1초 유휴 회수 기준이 처리 중인 배치와 겹치는지는 이번 계측에서 개별 메시지 단위로 추적하지 않았습니다.
- 각 조건은 본 측정 1회입니다. 고정 부하에서 누락 없이 큐를 줄인 같은 쿠폰 구성은 워커 2개였습니다. 워커 4개를 기본값으로 올릴 근거는 아직 부족합니다.

후속 실험에서도 Gradle 일반 테스트 1,162건, 통합 테스트 217건, Spotless와 줄 길이 검사를 통과했습니다. 기본 커버리지 78.74%와 기존 Git 무시 로깅 실험 클래스만 집계에서 제외한 80.65%는 앞선 실험과 같았습니다. 후속 실험의 전용 앱·DB·Redis도 정리했습니다.
