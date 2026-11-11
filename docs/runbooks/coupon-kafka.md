# Kafka 쿠폰 발급 운영

`coupon-kafka` 프로필에서 MySQL이 발급 가능 수량과 미완료 예약을 관리합니다. 쿠폰 행을 잠근 트랜잭션에서 예약과 발행 요청을 같이 저장하고 커밋 뒤 `202`를 반환합니다. Redis 정책 캐시와 매진 확인은 앞단에 있습니다. Redis의 기존 재고 값은 Kafka 모드에서 발급 차감에 사용하지 않습니다. 대사 시 DB 예약을 포함한 기대 재고와 Redis 관측값을 함께 확인합니다.

## 설정

| 항목 | 기본값 및 의미 |
| --- | --- |
| `KAFKA_BOOTSTRAP_SERVERS` | Book 공통 브로커 주소 |
| `coupon.kafka.partitions`, `coupon.kafka.workers` | 토픽 3 파티션, 발급 소비자 1개 |
| `coupon.kafka.replicas`, `coupon.kafka.min-in-sync-replicas` | 로컬 단일 브로커는 1/1. 운영 복제 구성에 맞춰 토픽 생성 전에 지정 |
| 생산자 | Book 공통 `acks=all`, 멱등 발행, 재시도 3회. 로컬 단일 복제본은 브로커 장애를 견디지 못함 |
| 소비자 | DB 커밋 뒤 레코드 단위 오프셋 반영. 공통 1초 간격 최대 3회 재시도 후 DLT |
| `coupon.kafka.relay-delay-ms` | 발행 작업 주기 100ms, 회차당 최대 64건씩 선점 |
| `coupon.waiting-room.coupon-ids` | 대기실을 적용하는 쿠폰 목록. 기본은 빈 목록 |
| `coupon.waiting-room.admissions-per-second`, `pass-ttl-ms` | 초당 250명 입장, 입장권 30초 |
| `coupon.waiting-room.requests-per-second`, `request-burst` | 대기실 적용 쿠폰의 사용자별 초당 5건 충전, 최대 10건 |

## 전환

1. 대상 쿠폰의 기존 발급 유입을 멈추고 Streams의 미전달·미확인 메시지를 모두 반영한 뒤 DB와 보유 쿠폰 수량을 확인합니다.
2. 동일 쿠폰에 대해 모든 발급 인스턴스를 함께 전환합니다. Streams/동기 발급과 Kafka 발급을 같은 쿠폰에 동시에 운영하면 재고 기준이 달라집니다.
3. Kafka 모드에서 다시 유입을 열고 오래된 미발행 예약, 소비 지연, 실패 건을 확인합니다. 역전환 시에도 먼저 미완료 예약을 해소하고 Redis 재고를 DB 기준으로 다시 맞춥니다.

## 실패 건 확인과 선별 재처리

권한이 있는 운영자가 승인된 DB 운영 경로에서 실패 건을 확인합니다.

```sql
SELECT id, coupon_id, generation, failed_at, failure_reason
FROM coupon_issue_request
WHERE failed_at IS NOT NULL AND completed_at IS NULL
ORDER BY failed_at;
```

원인을 해소한 뒤 확인한 ID만 재처리 요청합니다. 발행 작업은 같은 예약 ID의 다음 세대 이벤트를 발행하고 브로커 확인 후 요청을 갱신합니다. 기존 예약을 사용하므로 재고를 추가 차감하지 않습니다. 재발행이 확인돼도 발급 완료는 `completed_at`으로 확인합니다.

```sql
UPDATE coupon_issue_request
SET replay_requested = TRUE
WHERE id = ? AND failed_at IS NOT NULL AND completed_at IS NULL;
```

실패한 예약은 재처리할 수 있도록 재고를 유지합니다. 영구 취소와 재고 반환은 운영 정책이 확정된 뒤, 발급 미반영을 확인하고 별도 작업으로 처리해야 합니다. 재시도와 재처리 과정에서 접수 건수를 기준으로 대사합니다.
