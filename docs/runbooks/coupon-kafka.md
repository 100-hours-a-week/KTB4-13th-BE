# Kafka 쿠폰 발급 운영

쿠폰 발급 요청은 기본으로 Redis Lua로 재고 차감과 미발행 요청 기록을 함께 처리한 뒤 `202`를 반환합니다. 발행 작업은 Kafka 확인 뒤 Redis 기록을 ACK하고, 소비자는 MySQL 발급 저장을 커밋한 뒤 Kafka 오프셋을 반영합니다.

## 전환과 역전환

1. 배포 전에 Redis와 Kafka 연결을 확인합니다. Kafka 브로커는 `KAFKA_BOOTSTRAP_SERVERS`로 지정하고, 토픽 생성 전 `coupon.kafka.replicas`와 `coupon.kafka.min-in-sync-replicas`를 운영 구성에 맞춥니다. 로컬 기본값은 각각 1입니다.
2. 대상 쿠폰의 신규 요청을 멈춥니다. 기존 Streams를 사용 중이면 `coupon:issuance:stream`의 `coupon-db` 그룹을 비운 뒤 DB 발급 수량·보유 쿠폰 행·Redis 잔량을 대조합니다.
3. 모든 발급 인스턴스를 새 버전으로 전환합니다. 발급 API는 `202`를 반환하며, 쿠폰함은 소비자가 DB에 커밋한 뒤 갱신됩니다. 접수를 재개하고 Redis 미발행 기록과 Kafka 소비 지연을 확인합니다.
4. 이전 버전으로 역전환하려면 요청을 다시 멈추고 Redis 발행 대기와 Kafka 소비 지연을 해소합니다. 실패 토픽의 미해결 건과 DB 발급·Redis 잔량을 대조한 뒤 이전 버전을 배포합니다.

## 처리 지연과 장애

- Redis에서 `XINFO GROUPS coupon:issuance:kafka-outbox`로 `coupon-kafka-relay`의 `lag`(미전달)와 `pending`(미확인)을 확인합니다. 두 값이 0이어도 Kafka에 처리할 요청이 남을 수 있으므로 소비자 그룹 `book-coupon-issuance-redis`의 지연도 확인합니다.
- 소비 재시도를 소진한 요청은 `book.coupon-issuance.redis.v1.DLT`에 남습니다. 원인을 해결한 뒤 발급 여부와 예약을 확인합니다.
- Redis 복제 전에 `202`를 반환한 요청은 주 노드 장애 뒤 유실될 수 있습니다. 접수 유실을 허용할 수 없는 운영 환경에서는 활성화 전에 내구성 보완과 장애 전환 검증이 필요합니다.
- Redis Stream은 ACK 후에도 항목을 보관합니다. 운영 트래픽을 받기 전에 미처리 요청을 보존할 수 있는 정리 기준과 메모리 한도를 정합니다.

## 실패 건 선별 재발행

실패 토픽의 메시지 원문과 예외 유형은 `issuance_dlt_log`에 저장됩니다. DB 기록에 실패하면 소비자는 같은 메시지를 다시 읽습니다. 예외 원문과 스택은 실패 토픽에 싣지 않습니다.

권한 있는 운영자는 원인을 해결하고 미발급 상태를 확인한 뒤 필요한 ID만 선택합니다.

```sql
SELECT id, message_key, error_type, received_at, replay_requested_at, replayed_at
FROM issuance_dlt_log
WHERE replayed_at IS NULL
ORDER BY received_at DESC
LIMIT 100;

UPDATE issuance_dlt_log
SET replay_requested_at = CURRENT_TIMESTAMP(6)
WHERE id IN (123) AND replay_requested_at IS NULL AND replayed_at IS NULL AND payload IS NOT NULL;
```

재발행 작업은 기존 메시지 키와 원문을 사용하고, Kafka 확인 후 `replayed_at`을 기록합니다. 이 시각은 재발행 완료를 뜻하므로 최종 발급은 DB에서 확인합니다. 재발행 결과가 불확실하면 같은 메시지가 다시 전달될 수 있으며, 소비자는 기존 발급을 확인해 중복 반영을 막습니다. 다시 실패한 건은 새 실패 기록으로 남고 운영자가 선택할 때까지 대기합니다.

## 쿠폰 재고 대사

대사할 쿠폰 ID를 `COUPON_RECONCILE_COUPON_IDS=123,456`으로 지정하면 해당 인스턴스에서만 정기 대사를 실행합니다. 기본 간격은 60초이며 `coupon.reconcile.delay-ms`로 조정합니다. 같은 쿠폰을 여러 인스턴스에서 중복 조회하지 않도록 대상은 운영 담당 인스턴스에만 지정합니다.

Redis 예약 사용자 중 MySQL 보유 쿠폰 행에 없는 사용자를 미반영 예약으로 집계합니다. 삭제 처리된 보유 쿠폰도 이미 발급된 이력에 포함합니다. `전체 수량 - DB 발급 수량 - 미반영 예약 - Redis 잔량`이 0인지 확인하고, DB 발급 수량과 보유 쿠폰 행 수도 따로 대조합니다.

| 결과 | 의미 |
| --- | --- |
| `MATCHED` | DB 발급과 Redis 잔량이 일치하고 미반영 예약이 없습니다. |
| `PENDING` | 수량 차이가 Redis의 미반영 예약으로 설명됩니다. 실패 토픽에 남은 건도 포함될 수 있으므로 큐와 실패 기록을 확인합니다. |
| `DRIFT` | 미반영 예약을 제외해도 수량 차이가 남거나, DB 발급 수량·보유 행 또는 매진 신호에 불일치가 있습니다. |
| `REDIS_STOCK_MISSING` | Redis 잔량 키가 없습니다. 재고 0장으로 처리하지 않습니다. |
| `SNAPSHOT_CHANGED` | DB 조회 전후 Redis 스냅샷이 달라 수량 차이 판정을 보류했습니다. |

대사는 Redis와 DB를 조회만 합니다. 지정한 쿠폰의 예약 사용자와 DB 발급 이력을 읽으므로 필요한 쿠폰에 한해 사용합니다. 두 저장소에서 함께 사라진 접수까지 확인할 수는 없습니다.

## 행사 쿠폰 대기실

모든 발급 서버에 같은 설정을 배포한 뒤 행사 쿠폰의 진입을 엽니다. 아래 예시에서 쿠폰 123은 1초마다 최대 100명에게 30초 동안 유효한 입장권을 발급합니다. 설정에 없는 쿠폰은 기존 발급 경로를 사용합니다.

```yaml
coupon:
  waiting-room:
    replenish-rate: 5
    burst-capacity: 10
    rooms:
      123:
        admit-per-second: 100
        pass-ttl-ms: 30000
```

1. 인증 후 `POST /api/v1/waiting-room/{couponId}`로 줄에 들어갑니다. 같은 사용자가 다시 진입해도 줄의 위치가 유지됩니다.
2. `GET /api/v1/waiting-room/{couponId}`로 `admitted`, 1부터 시작하는 `position`, `estimatedWaitSeconds`를 확인합니다. 예상 대기 시간은 순번과 입장률로 계산하며, 클라이언트는 조회 간격을 최소 1초로 유지합니다.
3. 입장 후 발급 API를 호출합니다. 입장권이 없거나 만료되면 403이 반환됩니다. 만료된 사용자는 대기실에 다시 들어갈 수 있습니다. 202는 발급 접수를 뜻하며, 쿠폰함은 DB 저장 이후 반영됩니다.

대기실 진입·조회와 설정된 행사 쿠폰의 발급 요청은 인증 사용자별로 토큰 버킷을 공유합니다. 기본 설정은 초당 5건 충전, 순간 최대 10건입니다. 한도를 넘으면 429와 `Retry-After`를 반환합니다. 여러 앱 서버가 같은 Redis를 사용하므로 서버가 늘어도 사용자 한도와 쿠폰별 입장 정원은 늘어나지 않습니다. 입장 정원은 입장권을 발급하는 속도입니다. 유효한 입장권을 받은 사용자들이 동시에 발급을 요청할 수 있습니다.

부하를 확인할 때는 대기실 진입부터 입장까지의 시간, 발급 접수 p99, 429 및 시작 누락 건수, 최종 대사 결과를 함께 봅니다. 행사 종료 후 대기실 설정을 내리기 전에 쿠폰의 발급 종료 시각과 남은 요청을 확인합니다.
