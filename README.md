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
