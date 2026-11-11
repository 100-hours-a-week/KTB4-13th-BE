# Kafka 쿠폰 발급 운영

`coupon-kafka` 프로필에서 MySQL이 발급 가능 수량과 미완료 예약을 관리합니다. 쿠폰 행을 잠근 트랜잭션에서 예약과 발행 요청을 같이 저장하고 커밋 뒤 `202`를 반환합니다. Redis 정책 캐시와 매진 확인은 앞단에 있습니다. Redis의 기존 재고 값은 Kafka 모드에서 발급 차감에 사용하지 않습니다.

## 설정

| 항목 | 기본값 및 의미 |
| --- | --- |
| `KAFKA_BOOTSTRAP_SERVERS` | Book 공통 브로커 주소 |
| `coupon.kafka.partitions`, `coupon.kafka.workers` | 토픽 3 파티션, 발급 소비자 1개 |
| `coupon.kafka.replicas`, `coupon.kafka.min-in-sync-replicas` | 로컬 단일 브로커는 1/1. 운영 복제 구성에 맞춰 토픽 생성 전에 지정 |
| 생산자 | Book 공통 `acks=all`, 멱등 발행, 재시도 3회. 로컬 단일 복제본은 브로커 장애를 견디지 못함 |
| 소비자 | DB 커밋 뒤 레코드 단위 오프셋 반영. 공통 1초 간격 최대 3회 재시도 후 DLT |
| `coupon.kafka.relay-delay-ms` | 발행 작업 주기 100ms, 회차당 최대 64건씩 선점 |

## 전환

1. 대상 쿠폰의 기존 발급 유입을 멈추고 Streams의 미전달·미확인 메시지를 모두 반영한 뒤 DB와 보유 쿠폰 수량을 확인합니다.
2. 동일 쿠폰에 대해 모든 발급 인스턴스를 함께 전환합니다. Streams/동기 발급과 Kafka 발급을 같은 쿠폰에 동시에 운영하면 재고 기준이 달라집니다.
3. Kafka 모드에서 다시 유입을 열고 오래된 미발행 예약, 소비 지연, 실패 건을 확인합니다. 역전환 시에도 먼저 미완료 예약을 해소하고 Redis 재고를 DB 기준으로 다시 맞춥니다.

## 발급 지연과 실패 확인

발행되지 않은 요청과 발급 완료를 기다리는 요청은 MySQL 접수 기록에서 확인합니다.

```sql
SELECT id, coupon_id, created_at, published_at
FROM coupon_issue_request
WHERE completed_at IS NULL
ORDER BY created_at;
```

소비 재시도를 소진하면 Book 공통 Kafka 처리기가 `book.coupon-issuance.v1.DLT`에 요청 ID를 남깁니다. 해당 요청의 예약은 유지되므로 다른 요청이 그 재고를 가져갈 수 없습니다. 운영자 선별 재처리는 #560에서 다룹니다. Kafka 모드에서 Redis 재고는 발급 수량의 기준이 아니므로 역전환 전에 DB와 Redis 재고를 확인해야 합니다.
