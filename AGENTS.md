# AGENTS.md

이 파일은 Codex가 이 저장소에서 Spring Boot + Java 25 코드를 작성할 때 따라야 할 규칙을 정의합니다.
작업에 해당하는 기준 문서를 읽고 적용합니다.

- 모듈 구조·책임·명명 변경: [아키텍처](.docs/ARCHITECTURE.md)
- Java 코드 작성·수정: [코드 스타일](.docs/.convetions/CODE_STYLE_CONVETIONS.md)
- Issue·Branch·Commit·PR 작업: [Git 규칙](.docs/.convetions/GIT_CONVETIONS.md)
- 스킬 선택·관리: [스킬 색인](.docs/SKILL_DOCS.md)

## 기술 스택

- 언어: Java 25
- 프레임워크: Spring Boot 4
- 빌드: Gradle Kotlin DSL
- API: Spring Web MVC 기반 HTTP API
- ORM: Spring Data JPA
- 데이터베이스: MySQL
- 문서화: Spring REST Docs + Asciidoctor
- 테스트: JUnit 6 + AssertJ + Mockito

## 빌드 및 실행

```bash
# 전체 검증
./gradlew clean check

# 실행 JAR 생성
./gradlew :api:bootJar

# API 서버 실행
./gradlew :api:bootRun

# 전체 테스트
./gradlew test

# 모듈별 테스트
./gradlew :api:test
./gradlew :core:test
./gradlew :common:test
./gradlew :infrastructure:persistence:test
./gradlew :infrastructure:client:test

# 단일 테스트 클래스 실행
./gradlew :api:test --tests "*MemberControllerTest"

# API 문서 생성
./gradlew :api:asciidoctor
```

## 모듈 구조

```text
컴파일 의존:
api ──────────────────────→ core ──→ common
infrastructure:client ────→ core ──→ common
infrastructure:persistence → core ──→ common

런타임 결합:
api ⇢ runtimeOnly ⇢ infrastructure:client
api ⇢ runtimeOnly ⇢ infrastructure:persistence
```

- `common`: 여러 모듈이 실제로 공유하는 기술 중립 타입과 공통 예외 계약
- `core`: UseCase, Port, Application Service, Domain 모델과 정책
- `api`: 애플리케이션 진입점, Controller, 요청·응답 DTO, 인증 컨텍스트, API 예외 처리
- `infrastructure:persistence`: JPA Entity, Spring Data Repository, 영속성 Mapper와 Repository Port 구현
- `infrastructure:client`: 외부 API DTO, Mapper와 Client Port 구현

`core`는 `api`와 `infrastructure`를 참조하지 않습니다. `api`는 Infrastructure 구현 클래스를 직접 import하지 않습니다.

## Spring 빈과 트랜잭션

- 애플리케이션 진입점의 Component Scan 범위는 모든 모듈이 공유하는 최상위 패키지를 포함해야 합니다.
- Application Service는 `@Service`로 등록하고 Port(in) 인터페이스를 구현합니다.
- 쓰기 UseCase의 트랜잭션 경계는 Application Service의 public 메서드입니다.
- 조회 UseCase는 필요한 경우 `@Transactional(readOnly = true)`를 사용합니다.
- Domain에는 Spring 어노테이션을 사용하지 않습니다.
- Core에서 허용하는 Spring 의존은 DI와 트랜잭션에 필요한 최소 범위로 제한합니다. Spring Web, Spring Security, JPA, 외부 SDK는 금지합니다.

## Java 작성 규칙

- Command, Result, 요청·응답 DTO처럼 불변 데이터 전달이 목적이면 `record`를 우선 검토합니다.
- Domain 모델은 상태와 행위를 표현해야 하며 DTO처럼 작성하지 않습니다.
- Command는 생성 시점에 업무 전제조건을 검증합니다.
- Domain의 불변식과 상태 전이는 Domain 메서드가 검증합니다.
- Lombok을 사용하는 Domain 모델은 [코드 스타일 문서](.docs/.convetions/CODE_STYLE_CONVETIONS.md)의 표준 annotation prefix를 사용합니다. 보호된 무인자 생성자는 Domain 생성 규칙과 불변식을 우회하는 데 사용하지 않습니다.
- `Optional`은 반환값에만 제한적으로 사용하고 필드나 메서드 인자로 사용하지 않습니다.
- 의미 없는 getter/setter와 범용 Builder를 자동으로 추가하지 않습니다.
- 구현체의 접근 수준은 프레임워크와 모듈 경계가 허용하는 최소 범위로 유지합니다.

## Application Core 규칙

- Controller가 호출할 기능은 `application/port/in`의 UseCase 인터페이스로 정의합니다.
- DB와 외부 시스템 접근은 `application/port/out`의 Repository 또는 Client 인터페이스로 정의합니다.
- Application Service는 Adapter 구현체가 아니라 Port(out)에만 의존합니다.
- Application Service는 하나의 유스케이스 흐름만 담당합니다. 등록·조회·수정·삭제 기능을 하나의 Service에 모으지 않습니다.
- 비즈니스 규칙은 Domain에 두고 Application Service는 Domain과 Port를 조율합니다.
- Core의 메서드 시그니처에 HTTP DTO, JPA Entity, 외부 SDK 타입을 노출하지 않습니다.

## Inbound Adapter 규칙

- Controller는 Service 구현체가 아닌 Port(in) UseCase 인터페이스를 주입받습니다.
- 요청 DTO의 `toCommand()`에서 Command로 변환합니다.
- UseCase의 Result는 응답 DTO의 `from()`에서 변환합니다.
- Domain 객체와 JPA Entity를 HTTP 응답으로 직접 반환하지 않습니다.
- Controller에는 비즈니스 분기와 트랜잭션 로직을 작성하지 않습니다.
- HTTP 응답 형식과 API 전용 예외 처리는 `api` 모듈이 소유합니다.

## Outbound Adapter 규칙

- DB 접근은 Core의 `{Feature}Repository`와 Infrastructure의 `{Feature}RepositoryImpl`로 분리합니다.
- Spring Data Repository는 `{Feature}JpaRepository` 이름을 사용합니다.
- JPA Entity는 `infrastructure:persistence`에만 둡니다.
- Domain과 Entity 변환은 `{Feature}PersistenceMapper`가 담당합니다.
- 외부 연동은 Core의 `{Feature}Client`와 Infrastructure의 `{Feature}ClientImpl`로 분리합니다.
- 외부 DTO 변환은 출처가 드러나는 `{Feature}ExternalMapper`가 담당합니다.
- JPA 예외와 외부 SDK 예외를 Core 경계 밖으로 그대로 전달하지 않습니다.

## Common과 설정 규칙

- `common`은 임시 코드 보관소로 사용하지 않습니다.
- HTTP 응답 DTO와 API 전용 오류 표현은 `api`에 둡니다.
- 특정 DB, 클라우드 또는 외부 API 설정은 해당 Infrastructure 모듈의 `support`에 둡니다.
- `common/config`에는 두 개 이상의 모듈이 실제로 공유하며 Adapter SDK에 의존하지 않는 설정만 둡니다.
- `common`에 코드를 이동하기 전에 의미와 규칙이 여러 모듈에서 동일한지 확인합니다.

## 테스트 규칙

- Domain 테스트는 Spring Context 없이 실행합니다.
- Application Service 테스트는 Port(out)을 Fake 또는 Mock으로 대체합니다.
- Controller 테스트는 요청 검증, Command 변환, 응답과 오류 계약을 확인합니다.
- Persistence Adapter 테스트는 Entity 매핑, 쿼리와 DB 제약조건을 확인합니다.
- 외부 Client 테스트는 요청 생성, 응답 변환, 타임아웃과 오류 변환을 확인합니다.
- DB 의존 통합 테스트는 재현 가능한 격리 환경을 사용합니다.
- Java 코드·빌드·애플리케이션 설정 변경 시 관련 테스트와 `./gradlew check`를 실행합니다.
- 문서만 변경하면 링크·참조 경로·예시와 실제 구현의 일치 여부를 확인합니다.
- 스킬 변경 시 frontmatter·호출 설정·참조를 검증하고, 실행 지침을 바꾸면 [협업 검증 시나리오](.docs/SKILL_DOCS.md#github-협업-스킬-검증) 중 해당 사례를 확인합니다.
- Git 규칙 검사 변경 시 `node --test .github/scripts/git-conventions.test.cjs`를 실행하고, workflow 변경 시 `actionlint`로 검사합니다.
- 실행하지 못한 검증과 원인을 보고합니다. 문서 검증이나 캐시된 결과를 새 통합 테스트 성공으로 보고하지 않습니다.

## 변경 시 함께 확인할 파일

- API 변경: Controller 테스트, 요청·응답 DTO, REST Docs 테스트, `api/src/docs/asciidoc`
- DB 변경: Domain, Entity, Repository Port와 구현체, Mapper, 마이그레이션
- 외부 연동 변경: Client Port와 구현체, 외부 DTO, Mapper, 타임아웃과 오류 변환
- 모듈 의존 변경: 각 `build.gradle.kts`와 `settings.gradle.kts`

## 금지 사항

- Domain에 `@Entity`, `@Column`, `@JsonProperty`, `@Service`를 추가하지 않습니다.
- Core에서 Controller DTO, JPA Repository, EntityManager, RestClient, 외부 SDK를 직접 사용하지 않습니다.
- API에서 Infrastructure 구현 클래스를 컴파일 의존으로 참조하지 않습니다.
- Adapter 구현체를 Port 대신 직접 주입하지 않습니다.
- 비밀번호, 토큰, 키, 서명된 URL이나 외부 응답 원문을 로그에 남기지 않습니다.
- 요청받지 않은 모듈 재구성, 공개 API 변경, 의존성 추가를 함께 수행하지 않습니다.
