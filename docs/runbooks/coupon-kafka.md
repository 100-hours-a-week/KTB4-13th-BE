# Kafka 쿠폰 발급 운영

쿠폰 발급 요청은 기본으로 Redis Lua로 재고 차감과 미발행 요청 기록을 함께 처리한 뒤 `202`를 반환합니다. 발행 작업은 Kafka 확인 뒤 Redis 기록을 ACK하고, 소비자는 MySQL 발급 저장을 커밋한 뒤 Kafka 오프셋을 반영합니다.

## 전환과 역전환

1. 배포 전에 Redis와 Kafka 연결을 확인합니다. Kafka 브로커는 `KAFKA_BOOTSTRAP_SERVERS`로 지정하고, 토픽 생성 전 `coupon.kafka.replicas`와 `coupon.kafka.min-in-sync-replicas`를 운영 구성에 맞춥니다. 로컬 기본값은 각각 1입니다.
2. 대상 쿠폰의 신규 요청을 멈춥니다. 기존 Streams를 사용 중이면 `coupon:issuance:stream`의 `coupon-db` 그룹을 비운 뒤 DB 발급 수량·보유 쿠폰 행·Redis 잔량을 대조합니다.
3. 모든 발급 인스턴스를 새 버전으로 전환합니다. 발급 API는 `202`를 반환하며, 쿠폰함은 소비자가 DB에 커밋한 뒤 갱신됩니다. 접수를 재개하고 Redis 미발행 기록과 Kafka 소비 지연을 확인합니다.
4. 이전 버전으로 역전환하려면 요청을 다시 멈추고 Redis 발행 대기와 Kafka 소비 지연을 해소합니다. 실패 토픽의 미해결 건과 DB 발급·Redis 잔량을 대조한 뒤 이전 버전을 배포합니다.

## 처리 지연과 장애

- Redis에서 `XINFO GROUPS coupon:issuance:kafka-outbox`로 `coupon-kafka-relay`의 `lag`(미전달)와 `pending`(미확인)을 확인합니다. 두 값이 0이어도 Kafka에 처리할 요청이 남을 수 있으므로 소비자 그룹 `book-coupon-issuance-redis`의 지연도 확인합니다.
- 소비 재시도를 소진한 요청은 `book.coupon-issuance.redis.v1.DLT`에 남습니다. 원인을 해결한 뒤 발급 여부와 예약을 확인합니다. 선별 재처리는 #560에서 다룹니다.
- Redis 복제 전에 `202`를 반환한 요청은 주 노드 장애 뒤 유실될 수 있습니다. 접수 유실을 허용할 수 없는 운영 환경에서는 활성화 전에 내구성 보완과 장애 전환 검증이 필요합니다.
- Redis Stream은 ACK 후에도 항목을 보관합니다. 운영 트래픽을 받기 전에 미처리 요청을 보존할 수 있는 정리 기준과 메모리 한도를 정합니다.
