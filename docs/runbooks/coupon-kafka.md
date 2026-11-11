# Kafka 쿠폰 발급 운영

`coupon-kafka` 프로필은 Redis Lua로 재고 차감과 미발행 요청 기록을 함께 처리한 뒤 `202`를 반환합니다. 발행 작업은 Kafka 확인 뒤 Redis 기록을 ACK하고, 소비자는 MySQL 발급 저장을 커밋한 뒤 Kafka 오프셋을 반영합니다.

## 전환과 역전환

1. 대상 쿠폰의 발급 요청을 잠시 멈춥니다. Streams의 미전달·미확인 작업이 모두 MySQL에 반영됐는지 확인하고 DB 발급 수량·보유 쿠폰 행·Redis 잔량을 대조합니다.
2. 모든 발급 인스턴스에서 `coupon-kafka`를 켜고 기존 `coupon-streams` 및 `coupon-streams-worker`를 끕니다. Kafka 브로커는 `KAFKA_BOOTSTRAP_SERVERS`로 지정합니다. 토픽 생성 전 `coupon.kafka.replicas`와 `coupon.kafka.min-in-sync-replicas`를 운영 브로커 구성에 맞춥니다. 로컬 기본값은 각각 1입니다.
3. 발급 요청을 재개하고 Redis 미발행 기록, Kafka 소비 지연과 실패 토픽을 확인합니다. 역전환 전에는 발급 요청을 다시 멈추고 두 큐의 미처리 요청과 실패 건을 확인한 뒤 DB 발급과 Redis 잔량을 대조합니다.

## 처리 지연과 장애

- Redis에서 `XINFO GROUPS coupon:issuance:kafka-outbox`로 `coupon-kafka-relay`의 `lag`(미전달)와 `pending`(미확인)을 확인합니다. 두 값이 0이어도 Kafka에 처리할 요청이 남을 수 있으므로 소비자 그룹 `book-coupon-issuance-redis`의 지연도 확인합니다.
- 소비 재시도를 소진한 요청은 `book.coupon-issuance.redis.v1.DLT`에 남습니다. 원인을 해결한 뒤 발급 여부와 예약을 확인합니다. 선별 재처리는 #560에서 다룹니다.
- Redis 복제 전에 `202`를 반환한 요청은 주 노드 장애 뒤 유실될 수 있습니다. 접수 유실을 허용할 수 없는 운영 환경에서는 활성화 전에 내구성 보완과 장애 전환 검증이 필요합니다.
- Redis Stream은 ACK 후에도 항목을 보관합니다. 운영 트래픽을 받기 전에 미처리 요청을 보존할 수 있는 정리 기준과 메모리 한도를 정합니다.
