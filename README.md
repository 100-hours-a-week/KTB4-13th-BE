# book

Spring Boot와 Java 기반의 최소 헥사고날 멀티모듈 예제입니다.
기본 패키지는 `com.book`이며 Sample 생성과 ID 단건 조회를 제공합니다.

## 구성

- Java 25, Spring Boot 4.1.1, Gradle Wrapper 9.7.1
- MySQL 8.4, Flyway, Spring Data JPA
- JUnit Jupiter 6.0.3, AssertJ, Mockito, Testcontainers 2.0.5

Spring Framework 7의 테스트 지원은 JUnit 6 이상을 요구합니다.
프로젝트와 `AGENTS.md`는 JUnit 6을 기준으로 합니다.

```text
api ──────────────────────→ core ──→ common
infrastructure:client ────→ core
infrastructure:persistence → core

api ⇢ runtimeOnly ⇢ infrastructure:client
api ⇢ runtimeOnly ⇢ infrastructure:persistence
```

`api`만 실행 가능한 Boot JAR를 생성합니다. Client 모듈은 외부 연동 요구사항이 없어
빌드 설정만 포함합니다. 기존 Groovy 빌드와 `src/`는 `legacy/`에 원본 그대로
보존했으며 현재 빌드에는 포함하지 않습니다. 기존 테스트의 계약은 새 API와 다르므로
새 모듈별 테스트에서 이번 요구사항을 검증합니다.

## 로컬 실행

Java 25와 로컬 MySQL이 필요합니다. 먼저 MySQL에 `book` 데이터베이스와
접근 가능한 로컬 사용자를 준비하고 `.env.example`을 참고해 실행 환경에
`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`를 설정합니다.
`.env` 파일은 자동으로 읽지 않으며 실제 인증정보를 커밋하지 않습니다.

```bash
./gradlew :api:bootRun
```

기본 프로필은 `local`입니다. MySQL 설정은 Persistence 모듈의
`persistence.yml`이 소유하며 Flyway가 테이블을 생성하고 Hibernate가 매핑을 검증합니다.

```bash
curl -i http://localhost:8080/api/v1/samples \
  -H 'Content-Type: application/json' \
  -d '{"name":"첫 샘플"}'

curl -i http://localhost:8080/api/v1/samples/1
```

생성은 201과 `Location`, 조회는 200을 반환합니다. 응답 필드는 `id`, `name`입니다.
이름은 앞뒤 공백 제거 후 1~100자이며, 잘못된 요청은 400, 없는 ID는 404입니다.
오류 응답 필드는 `code`, `message`입니다.

요청은 Controller → UseCase → Application Service → Repository Port →
Persistence Adapter → MySQL 순서로 처리합니다. 생성·조회 Service가 각각
쓰기·읽기 전용 트랜잭션을 담당합니다.

## 검증

```bash
./gradlew clean check
./gradlew :api:bootJar
```

`check`는 단위 테스트, Controller 계약, Testcontainers MySQL 통합 테스트와
전체 Application Context 구동을 검증합니다. Docker가 필요하며
사용할 수 없을 때 통합 테스트를 자동으로 생략하지 않습니다.
테스트는 `test` 프로필과 일회용 `mysql:8.4.8` 컨테이너를 사용하므로 로컬 DB 인증정보가 필요 없습니다.

Colima를 사용할 때 Testcontainers가 소켓을 찾지 못하면 해당 실행에만
`DOCKER_HOST=unix://$HOME/.colima/default/docker.sock`과
`TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock`을 설정합니다.

- 실행 JAR: `api/build/libs/api.jar`
- 테스트 보고서: 각 모듈의 `build/reports/tests/test/index.html`
