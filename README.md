# book

Spring Boot와 Java 기반의 단일 모듈 프로젝트입니다.
기본 패키지는 `com.book`이며 주소지 등록·조회와 장바구니 기능을 제공합니다.

## 구성

- Java 25, Spring Boot 4.1.1, Gradle Wrapper 9.7.1
- MySQL 8.4, Flyway, Spring Data JPA
- springdoc-openapi와 Swagger UI
- Lombok 생성자 주입, Spotless(PJF), JaCoCo
- JUnit 6, AssertJ, Mockito, Testcontainers, ArchUnit

## 패키지 구조

```text
src/main/java/com/book/
├── BookApplication.java
├── core/
│   └── {feature}/
│       ├── api/                # Controller, request, response, spec
│       │   └── converter/       # HTTP 입력·출력 변환
│       ├── application/        # command, result, service, usecase, port
│       ├── domain/             # JPA 매핑을 포함한 업무 모델
│       └── infrastructure/     # persistence, 필요 시 client
├── common/
│   ├── config/                 # 공유 설정
│   ├── domain/                 # 공통 영속 생명주기
│   ├── exception/              # CoreException, ErrorCode와 전역 HTTP 예외 처리
│   ├── logging/                # 요청 추적
│   └── response/               # success/data 공통 응답 계약
```

`core`는 Gradle 모듈이 아니라 업무 기능을 모은 패키지입니다.
신규·수정 기능은 `Controller → CommandConverter → FeatureService → ActionUseCase → Domain/Port` 흐름을 따릅니다.
`FeatureService`는 CRUD와 추가 기능의 실행 순서를 조율하는 orchestration layer이고, `ActionUseCase`는 트랜잭션과 업무 흐름을 담당합니다.
저장소는 `RepositoryPort → RepositoryAdapter → JpaRepository` 순서로 연결합니다.
현재 `CartService`와 `AddressService`가 이 구조의 기준입니다.
Domain 모델이 JPA Entity를 겸하며, API DTO와 영속 전용 기술 모델은 별도로 유지합니다.
Spring 빈의 필수 의존성은 `private final`과 `@RequiredArgsConstructor`로 주입합니다.

## 로컬 실행

Java 25와 로컬 MySQL이 필요합니다. MySQL에 `book` 데이터베이스와
접근 가능한 사용자를 준비하고 `.env.example`을 참고해 실행 환경에
`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`를 설정합니다.
`.env` 파일은 자동으로 읽지 않으며 실제 인증정보를 커밋하지 않습니다.

```bash
./gradlew bootRun
```

`bootRun`과 Docker 이미지의 실행 JVM은 기본 시간대로 `Asia/Seoul`을 사용합니다.
따라서 `LocalDate.now()`와 `LocalDateTime.now()`도 서울 기준 날짜와 시간을 반환합니다.
IDE에서 직접 실행할 때는 VM 옵션에 `-Duser.timezone=Asia/Seoul`을 지정합니다.
JAR를 직접 실행할 때도 같은 옵션을 사용합니다.

```bash
java -Duser.timezone=Asia/Seoul -jar build/libs/book.jar
```

기본 프로필은 `local`입니다. `src/main/resources/persistence.yml`에서 DB 설정을 관리하며
Flyway가 테이블을 생성하고 Hibernate가 매핑을 검증합니다.

Redis 연결은 `REDIS_HOST`(기본값 `localhost`), `REDIS_PORT`(기본값 `6379`),
`REDIS_PASSWORD`(기본값 없음) 환경변수로 설정합니다. 문자열 키와 값을 다룰 때는
Spring Boot가 자동 구성하는 `StringRedisTemplate`을 주입받아 사용합니다. 아직 Redis를 사용하는
기능이나 캐시 만료 정책은 구현하지 않았습니다.
실제 Redis를 사용하기 전에는 Redis 상태가 서비스 health에 영향을 주지 않도록 Redis health 검사를
비활성화합니다. Redis 운영을 시작할 때는 환경변수 `MANAGEMENT_HEALTH_REDIS_ENABLED=true`로
검사를 활성화하고 장애 시 서비스 health 정책을 함께 검토합니다.

```bash
curl -i 'http://localhost:8080/api/v1/user-addresses?userId=42' \
  -H 'Content-Type: application/json' \
  -d '{"label":"집","postalCode":"12345","address":"서울시 강남구","detailAddress":"101호","isDefault":true}'
```

주소지 등록은 200을 반환하며, 잘못된 요청은 400입니다. 주소지 조회는 인증된 사용자의 활성 주소지를 반환합니다.
성공 응답은 `success`, `data`를 사용하고, 오류 응답은 `success`, `code`, `message`, `traceId`를 포함합니다. `CoreException`에 안전한 부가 정보가 있으면 `data`가 추가됩니다.
쓰기·읽기 전용 트랜잭션은 각 UseCase의 public 메서드가 담당합니다.

- Swagger UI: `/swagger-ui/index.html`
- OpenAPI 명세: `/v3/api-docs`

### 추천 프로필 Kafka 연결

온보딩 완료와 선호 수정으로 생긴 갱신 작업을 MySQL에 먼저 저장합니다.
Kafka 연결을 켜면 `book.profile-sync.v1`에서 작업 ID가 소비된 뒤 AI 프로필 갱신을 시작합니다.
메시지 본문에는 무작위 작업 ID만 넣고, AI 요청에 필요한 데이터는 작업자가 MySQL에서 읽습니다.
같은 사용자의 대기 중 변경은 최신 작업 하나로 합칩니다.

로컬에서만 사용하는 단일 브로커는 다음과 같이 시작합니다.

```bash
docker compose -f compose.kafka.yml up -d
# 브로커가 기동된 뒤 실행합니다.
for topic in book.profile-sync.v1 book.profile-sync.v1.DLT; do
  docker compose -f compose.kafka.yml exec kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 \
    --create --if-not-exists --topic "$topic" --partitions 1 --replication-factor 1 \
    --config retention.ms=86400000
done
PROFILE_SYNC_KAFKA_ENABLED=true KAFKA_BOOTSTRAP_SERVERS=localhost:9092 ./gradlew bootRun
```

브로커 연결과 발행·직렬화 기본값, 프로필 Kafka 활성화 여부는 `application.yaml`에서 관리합니다.
`application-local.yml`, `application-dev.yml`, `application-prod.yml`이 환경별 소비자 그룹을 설정합니다.
같은 환경의 인스턴스는 동일한 그룹을 사용하고, 필요하면 `PROFILE_SYNC_KAFKA_GROUP_ID`로 덮어쓸 수 있습니다.
프로필 리스너가 오프셋 처리 방식을 정하고, 공통 `KafkaConfig`가 소비 재시도와 실패 토픽 발행을 구성합니다.
연결은 기본적으로 꺼져 있으며, 이 경우 MySQL 작업자가 대기 작업을 직접 처리합니다.
운영에서는 원본 토픽과 같은 파티션 수로 실패 토픽을 만들고, 복제·접근 권한·보관 정책을 설정합니다.
그 뒤 같은 DB를 사용하는 인스턴스에 동일하게 연결을 켭니다.
브로커 장애 시 온보딩 저장은 성공하고, 갱신 작업은 DB에 남아 약 1분 후 다시 발행할 수 있습니다.
소비 중 DB 반영이 실패하면 1초 간격으로 세 번 재시도합니다.
그래도 실패하면 같은 파티션의 `book.profile-sync.v1.DLT`에 기록합니다.
실패 토픽은 운영 확인에 쓰고, 자동 복구는 MySQL에 남은 작업을 다시 발행해 처리합니다.

## 검증

```bash
./gradlew clean check
./gradlew bootJar
./gradlew spotlessApply
./gradlew test
./gradlew integrationTest
./gradlew test --tests '*GetAddressesUseCaseTest'
```

`test`는 Docker가 필요하지 않은 단위·Controller·아키텍처 테스트를 검증합니다.
`integrationTest`는 Testcontainers MySQL과 전체 Application Context를 검증하며 Docker가 필요합니다.
`check`는 두 테스트 작업을 모두 실행하고, Docker가 없을 때 통합 테스트를 자동으로 생략하지 않습니다.
테스트는 `test` 프로필과 일회용 `mysql:8.4.8` 컨테이너를 사용하므로 로컬 DB 인증정보가 필요 없습니다.

Colima를 사용할 때 Testcontainers가 소켓을 찾지 못하면 해당 실행에만
`DOCKER_HOST=unix://$HOME/.colima/default/docker.sock`과
`TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock`을 설정합니다.

- 실행 JAR: `build/libs/book.jar`
- 테스트 보고서: `build/reports/tests/test/index.html`
- 커버리지: `build/reports/jacoco/test/html/index.html`

## 멀티모듈 원본

전환 전 커밋 `3ddbb6059b54f1522b72bdad6b5555a071e09a57`은
[백업 저장소](https://github.com/023-dev/ver2-book/tree/3ddbb6059b54f1522b72bdad6b5555a071e09a57)에 보관합니다.
백업 저장소 접근에는 권한이 필요합니다.

### AI 추천 프로필 Kafka 직접 전달

기본값은 기존 HTTP PUT·DELETE입니다. AI Consumer와 삭제·순서 경합을 해결하고 운영 설정을 확인한 후에만 `PROFILE_SYNC_AI_KAFKA_ENABLED=true`로 전환합니다. 이 설정은 #544의 내부 작업 토픽 설정 `PROFILE_SYNC_KAFKA_ENABLED`와 별개입니다. 내부 토픽 사용 여부와 관계없이 MySQL 작업자가 AI 전용 토픽에 발행합니다. 야간 등 큐 외 기존 HTTP 호출은 그대로입니다.

- 토픽: `book.ai-profile.v1` (실패 토픽 예: `book.ai-profile.v1.DLT`)
- Key: 사용자 ID의 UTF-8 10진 문자열
- Consumer Group: 환경별로 다른 `book-ai-profile-v1-{환경}`을 사용하고 같은 환경의 AI 인스턴스는 공유합니다.
- `KAFKA_BOOTSTRAP_SERVERS`에 연결 가능한 브로커 목록을 설정합니다. 운영 TLS/SASL, ACL, 토픽 복제·보관 및 메시지 크기 제한은 운영 환경에 맞게 설정합니다.

```json
{
  "schema_version": 2,
  "operation": "UPSERT",
  "user_id": 42,
  "job_revision": "2d32c6a2-322d-46c3-b3d3-6c8680b6a723",
  "profile": {
    "user_id": 42,
    "idempotency_key": "existing-http-request-key",
    "onboarding": {"reading_times": ["잠들기 전"], "criteria": [], "categories": ["소설"], "tags": ["SF"], "liked_isbns": []},
    "memories": [],
    "history": {"purchased_isbns": [], "library_isbns": [], "reviews": []}
  }
}
```

삭제 메시지는 `{"schema_version":2,"operation":"DELETE","user_id":42,"job_revision":"2d32c6a2-322d-46c3-b3d3-6c8680b6a723"}`이며 `profile`을 포함하지 않습니다. AI는 기존 v1 UPSERT와 v2 UPSERT·DELETE를 지원해야 합니다.

`schema_version`은 메시지 계약 버전, `job_revision`은 BE 작업 UUID입니다. `profile`은 기존 PUT 본문이며 AI `ProfileRequest`로 검증합니다. 기억은 기존 계약대로 `type`, `value`, `strength`, `applies_to`, `keyword`, `created_at`, `vector`, `dim`을 포함합니다. AI Consumer는 사용자 ID와 key의 일치 여부를 검증하고 기존 갱신·삭제 로직을 재사용해야 합니다. 자동 offset commit을 끄고 DB 저장 성공 후에만 commit합니다. 일시 실패는 backoff로 재시도하고, 재시도 소진 시 DLT 발행 성공을 확인한 다음 원본 offset을 진행합니다. DLT 발행 실패 시 원본을 넘기지 않습니다.

BE의 작업 완료는 Kafka 브로커 발행 확인이지 AI 처리 완료가 아닙니다. 발행 성공 응답 유실이나 AI DB 저장 후 offset commit 전 종료 시 중복될 수 있습니다. `job_revision`은 UUID로 순서를 나타내지 않으며, 재시도 시 같은 revision에 최신 데이터를 다시 읽을 수 있어 이를 단독 중복 제거 키로 사용해서는 안 됩니다. 사용자 key와 파티션별 순차 처리만으로 지연·재시도에 따른 최신 상태를 보장하지 못합니다. 현재 AI의 `idempotency_key`도 중복 제거에 사용되지 않습니다. AI 측 버전 검증·멱등성은 아직 구현되지 않았습니다.

MySQL 작업자 락 연결이 끊기면 이전 작업자의 발행이 계속되어 DELETE 뒤에 오래된 UPSERT가 도착할 수 있습니다. 동의 철회나 탈퇴 후 지연된 메시지가 AI 프로필을 재생성할 수 있습니다. 발행 순서 보장과 Consumer 경합 대책을 검증하기 전에는 Kafka 경로를 활성화하지 않습니다. AI Consumer를 먼저 배포하고, HTTP 롤백 전 Consumer lag와 처리 중 작업을 확인합니다. 메시지에는 개인정보와 벡터가 들어갈 수 있으므로 ACL·보관 기간·DLT 접근을 제한하고 본문을 로그에 남기지 않습니다. 큰 본문은 브로커와 Producer·Consumer의 크기 제한을 초과할 수 있습니다. 초과 시 BE 작업은 재시도되며, 제한 조정 또는 입력 축소를 운영자가 해결해야 합니다.

로컬에서 `docker compose -f compose.kafka.yml up -d`로 별도 브로커를 기동한 뒤 다음 토픽을 생성합니다. 기존 README의 내부 작업 토픽도 사용하는 경우 해당 생성 절차를 함께 실행합니다.

```bash
for topic in book.ai-profile.v1 book.ai-profile.v1.DLT; do
  docker compose -f compose.kafka.yml exec kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 \
    --create --if-not-exists --topic "$topic" --partitions 1 --replication-factor 1 --config retention.ms=86400000
done
PROFILE_SYNC_AI_KAFKA_ENABLED=true KAFKA_BOOTSTRAP_SERVERS=localhost:9092 ./gradlew bootRun
```

AI Consumer를 별도 테스트 DB와 같은 브로커에 연결해 온보딩 완료·취향 수정 메시지와 저장 결과를 확인합니다. Docker 내부 AI에서는 `localhost`가 AI 컨테이너를 가리키므로 브로커의 advertised listener가 컨테이너에서 접근 가능한 주소인지 별도 확인합니다. 위 단일 브로커는 로컬 테스트용입니다.
