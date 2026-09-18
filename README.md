# book

Spring Boot와 Java 기반의 단일 모듈 프로젝트입니다.
기본 패키지는 `com.book`이며 Sample 생성과 ID 단건 조회를 제공합니다.

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
│   └── sample/
│       ├── api/                # Controller, request, response, spec
│       ├── application/        # command, result, usecase, port
│       ├── domain/             # JPA 매핑을 포함한 업무 모델과 예외
│       └── infrastructure/     # persistence, 필요 시 client
├── common/
│   ├── config/                 # 공유 설정
│   ├── domain/                 # 공통 영속 생명주기
│   ├── exception/              # 공통 오류 계약과 전역 HTTP 예외 처리
│   ├── logging/                # 요청 추적
│   └── response/               # success/data 공통 응답 계약
```

`core`는 Gradle 모듈이 아니라 업무 기능을 모은 패키지입니다.
UseCase는 `@Service`가 붙은 구체 클래스이며 한 업무 흐름을 담당합니다.
Controller → UseCase → Repository Port를 호출하고, Persistence 구현체가 Port를 구현합니다.
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

기본 프로필은 `local`입니다. `src/main/resources/persistence.yml`에서 DB 설정을 관리하며
Flyway가 테이블을 생성하고 Hibernate가 매핑을 검증합니다.

```bash
curl -i http://localhost:8080/api/v1/samples \
  -H 'Content-Type: application/json' \
  -d '{"name":"첫 샘플"}'

curl -i http://localhost:8080/api/v1/samples/1
```

생성은 201과 `Location`, 조회는 200을 반환합니다. 성공 응답의 `data` 안에 `id`, `name`이 포함됩니다.
이름은 앞뒤 공백 제거 후 1~100자이며 잘못된 요청은 400, 없는 ID는 404입니다.
성공 응답은 `success`, `data`를 사용하고, 오류 응답은 `success`, `code`, `message`, `traceId`를 포함합니다.
쓰기·읽기 전용 트랜잭션은 각 UseCase의 public 메서드가 담당합니다.

- Swagger UI: `/swagger-ui/index.html`
- OpenAPI 명세: `/v3/api-docs`

## 검증

```bash
./gradlew clean check
./gradlew bootJar
./gradlew spotlessApply
./gradlew test
./gradlew integrationTest
./gradlew test --tests '*SampleUseCaseTest'
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
