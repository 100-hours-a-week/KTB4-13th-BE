# 아키텍처와 개발 규칙

## 1. 단일 모듈과 도메인별 패키지

Gradle 프로젝트 하나에서 모든 소스를 빌드합니다. 기능별로 API, Application,
Domain, Infrastructure를 모아 한 기능을 수정할 때 여러 Gradle 모듈을 오가지 않도록 합니다.
`core`는 순수 Domain만을 뜻하지 않으며 업무 기능 패키지의 상위 이름입니다.

```text
src/main/java/com/book/
├── BookApplication.java
├── core/
│   └── {feature}/
│       ├── api/
│       │   ├── {Feature}Controller.java
│       │   ├── request/
│       │   ├── response/
│       │   ├── converter/
│       │   └── spec/
│       ├── application/
│       │   ├── command/
│       │   ├── result/
│       │   ├── service/
│       │   ├── usecase/
│       │   └── port/
│       ├── domain/
│       │   └── {Feature}.java
│       └── infrastructure/
│           ├── persistence/
│           │   ├── entity/
│           │   ├── repository/
│           │   └── mapper/
│           └── client/
├── common/
│   ├── config/
│   ├── domain/
│   │   ├── BaseEntity.java
│   │   └── BaseTimeEntity.java
│   ├── exception/
│   ├── logging/
│   └── response/
│       ├── ApiResponse.java
│       ├── ErrorResponse.java
│       └── PageResponse.java
```

신규·수정 기능은 현재 Cart 구조를 기준으로 필요한 패키지를 생성합니다. HTTP 입력이 있는 기능에는 `api/converter`를 두고, 기능 진입점에는 `application/service`를 둡니다. client·config·policy는 실제 외부 연동이나 공유 설정 요구가 생길 때만 생성합니다.
테스트는 `src/test/java`에서 대상 코드의 패키지를 따릅니다.
설정과 Flyway 마이그레이션은 `src/main/resources`, 테스트 설정은 `src/test/resources`에 둡니다.

## 2. 의존 방향

```text
Controller → {Feature}CommandConverter → {Feature}Service → {Action}UseCase → Domain
                                             ↓
                                  Repository·Client Port ← Adapter → JpaRepository
```

- 신규·수정 API는 `Controller → CommandConverter → FeatureService → ActionUseCase` 순서로 진입합니다. Controller는 Service만 호출하고 Infrastructure를 직접 호출하지 않습니다.
- Application은 Domain과 자신이 정의한 Port에 의존합니다. JPA·HTTP·외부 SDK 타입을 사용하지 않습니다.
- Domain 모델은 Application, API, Infrastructure, Spring, Jackson을 참조하지 않습니다. JPA 매핑과 `common/domain`의 공통 기반 타입(`BaseEntity`, `BaseTimeEntity`)은 허용합니다.
- Infrastructure는 Port를 구현하며 업무 모델을 직접 저장합니다. 도메인 대응 타입이 없는 실제 요구의 영속 전용 모델만 Entity로 분리하고, 예상 기능을 위한 Entity·테이블은 만들지 않습니다.
- 기능 간 협력은 공개 UseCase 계약으로 수행합니다. 다른 기능의 Repository 구현을 직접 참조하거나 순환 의존을 만들지 않습니다.

멀티모듈의 컴파일 격리 대신 `ArchitectureTest`로 핵심 패키지 의존 방향을 검사합니다.
패키지 배치만으로 의존 방향이 강제되지는 않습니다.

### 신규·수정 기능의 표준 흐름

다음 기능은 장바구니의 현재 구조를 복사 가능한 기준으로 사용합니다.

1. `api/request`와 `api/spec`에 HTTP 입력·OpenAPI 계약을 정의합니다.
2. `api/converter/{Feature}CommandConverter`가 요청·경로·쿼리 값을 `application/command`로 옮깁니다.
3. `api/{Feature}Controller`는 입력을 검증한 뒤 `application/service/{Feature}Service`만 호출합니다.
4. `{Feature}Service`는 CRUD와 추가 기능의 실행 순서를 조율하고, 동작별 `usecase/{Action}UseCase`를 호출합니다. 현재 동작이 하나뿐이어도 이 진입점을 유지합니다.
5. `{Action}UseCase`가 트랜잭션 안에서 Port를 조회하고 Domain의 상태 전이를 실행합니다.
6. `application/port/{Feature}RepositoryPort`는 저장 계약을 정의하고, `infrastructure/persistence/repository/{Feature}RepositoryAdapter`가 `{Feature}JpaRepository`를 조합해 구현합니다.
7. 응답 변환이 복잡하거나 재사용될 때만 `api/converter/{Feature}ResultConverter`를 추가합니다. `Void` 성공 응답처럼 변환이 없으면 만들지 않습니다.

Service·Converter 없이 직접 UseCase나 Repository 구현체를 호출하는 구조는 새 기능의 기준으로 복사하지 않습니다. 기존 기능을 수정할 때도 요청 범위를 넘어 일괄 개편하지 않습니다.

## 3. UseCase와 주입

`application/service/{Feature}Service`는 기능의 orchestration layer입니다.
Controller의 진입점으로서 여러 CRUD·추가 기능 UseCase의 실행 순서를 조율하지만, Domain 규칙·트랜잭션·JPA Repository 호출을 소유하지 않습니다.
현재 UseCase가 하나뿐이어도 Service를 제거하지 않고 이후 기능 확장 지점으로 유지합니다.

`application/usecase`의 `{Feature}{Action}UseCase`는 구체 클래스입니다.
현재 코드에서는 `@Service` 또는 프로젝트의 `@UseCase`로 Spring 빈을 등록합니다.
예: `RegisterAddressUseCase`, `GetAddressesUseCase`.
한 업무 흐름마다 하나를 두며 동일 역할의 인터페이스와 Service 구현체를 쌍으로 만들지 않습니다.
Repository·Client는 외부 기술을 격리하고 단위 테스트에서 대체하기 위해 Port를 유지합니다.

필수 의존성은 `private final`과 `@RequiredArgsConstructor`를 기본으로 주입합니다.
검증·추가 초기화·매개변수 어노테이션 등 예외는 [코드 스타일](.conventions/CODE_STYLE_CONVETIONS.md)을 따릅니다.
쓰기 트랜잭션은 UseCase의 public 메서드에 두며 조회는 필요한 경우 readOnly를 사용합니다.
Controller와 Domain에는 트랜잭션을 두지 않습니다.

## 4. 데이터와 변환

| 역할 | 위치·이름 | 책임 |
| --- | --- | --- |
| HTTP 요청·응답 | api/request·response | 입력 형식과 공개 응답 계약 |
| API 명세 | api/spec | springdoc 어노테이션 |
| Command·Result | application/command·result | 기술 중립 UseCase 입력·출력 |
| Domain 모델 | domain/{Feature} | JPA 매핑·상태·불변식·업무 행위 |
| 저장 Port | application/port/{Feature}RepositoryPort | 저장·조회 계약 |
| JPA 저장소 | infrastructure/persistence/repository/{Feature}JpaRepository | Spring Data JPA |
| 기능 orchestration | application/service/{Feature}Service | Controller 진입점·여러 UseCase 실행 순서 조율 |
| 저장 구현 | infrastructure/persistence/repository/{Feature}RepositoryAdapter | Port 구현·Spring Data JPA 조합 |
| API 입력 변환 | api/converter/{Feature}CommandConverter | HTTP 입력을 Command로 변환 |
| API 출력 변환 | api/converter/{Feature}ResultConverter | Result를 응답 DTO로 변환 |
| 영속성 변환 | infrastructure/persistence/mapper/{Feature}PersistenceMapper | 별도 표현이 필요할 때만 사용 |
| 영속 전용 Entity | infrastructure/persistence/entity | 도메인 대응 타입이 없는 실제 요구의 기록·기술 모델 |
| 외부 Port | application/port/{Feature}Client | 외부 기능 계약 |
| 외부 구현 | infrastructure/client | 외부 DTO·SDK·타임아웃·오류 변환 |

- 신규로 만들거나 수정하는 HTTP API는 기능별 `CommandConverter`를 통해 HTTP 입력을 Command로 변환합니다. Converter는 표현 변환만 담당하며 업무 규칙이나 외부 호출을 소유하지 않습니다. 기존 `Request.toCommand()`는 해당 기능을 수정할 때 함께 정리합니다.
- 결과 응답의 변환이 필요할 때만 `ResultConverter`를 둡니다. 응답이 없거나 단순한 정적 응답이면 만들지 않습니다.
- Domain 모델은 별도 업무 Entity를 두지 않습니다. Domain 모델을 HTTP 응답으로 직접 반환하지 않습니다.
- 사용자 식별자는 API·Command·Port 전반에서 `userId`로 통일합니다.
- 여러 JPA Domain 모델이 `id`, `status`, `created_at`, `updated_at`을 공유하면 `common/domain/BaseEntity`를 상속합니다. 기능 Domain 모델마다 `@Id`와 `@GeneratedValue(strategy = GenerationType.IDENTITY)`를 중복 선언하지 않습니다.
- 공통 생명주기 컬럼이 없는 Entity까지 `BaseEntity`를 강제하지 않습니다. 공통 기반을 사용하지 않는 모델은 필요한 식별자 매핑을 자체적으로 둡니다.
- 영속 모델의 공개 생성자는 DB 필수값을 모두 받고, 기존 ID를 복원하는 생성자는 `super(id)`를 호출합니다. 신규 Domain 객체는 ID 없이 생성하고, JPA 복원용 보호된 무인자 생성자만 유효성 검증을 우회할 수 있습니다.
- Command·Result·DTO는 불변 데이터 전달이 목적이면 record를 우선합니다.
- Domain은 불변식을 보장하는 생성자·팩터리와 상태 전이 메서드를 사용합니다.
- Mapper는 데이터 표현만 변환하고 업무 판단은 하지 않습니다.
- 신규 저장 구현은 `RepositoryPort → RepositoryAdapter → JpaRepository` 구조를 사용합니다. Adapter는 Port와 Spring Data Repository를 조합합니다. 기존 `RepositoryImpl`은 기존 기능의 점진적 정리 대상이며 새 기능에 복사하지 않습니다. Querydsl은 복잡한 조인·통계·벌크 연산·프로젝션이 실제로 필요할 때만 QueryRepository로 추가합니다.

## 5. 검증과 오류

HTTP 입력의 형식·길이·필수값은 Bean Validation과 Controller의 `@Valid`로 검증합니다.
Command는 생성 시 업무 전제를, Domain은 불변식과 상태 전이를 검증합니다.
권한이나 현재 저장 상태처럼 실행 시점에 확인할 조건은 UseCase가 Port를 통해 조회하여 검증합니다.

공통 오류 계약은 `common/exception/CoreException`, `ErrorCode`, `common/response/ErrorResponse`입니다.
`ErrorCode`는 중앙 오류 코드 enum이며 HTTP 상태·공개 코드·메시지·로그 레벨을 함께 제공합니다.
기능별 오류 코드는 별도 `core/{feature}/domain/exception`에 만들지 않고 `common/exception/ErrorCode`에서 관리합니다.
`CoreException`의 `data`는 필요한 안전한 오류 부가 정보만 담고, 비밀번호·토큰·키·외부 응답 원문은 넣지 않습니다.
알려진 비즈니스 오류는 `CoreException`과 `ErrorCode`로 표현합니다. 저장소의 `DataAccessException`·`PersistenceException`은 Repository에서 일괄 변환하지 않고 전파하며, `common/exception/GlobalExceptionHandler`가 안전한 HTTP 응답으로 매핑합니다. 기술 예외에 별도 비즈니스 의미가 생길 때만 명시적으로 변환합니다.
민감한 오류 원문이나 인증정보를 응답·로그에 노출하지 않습니다.

## 6. 공통 코드와 접근 수준

- `common`에는 기능에 종속되지 않고 여러 기능이 공유하는 코드를 둡니다. 추후 멀티모듈이나 독립 모듈로 분리할 때 기술 중립 코드를 우선합니다.
- `common/domain`에는 여러 JPA Entity가 공유하는 영속성 생명주기 타입만 둡니다. `BaseEntity`는 공유 `id`·상태·시간을, `BaseTimeEntity`는 생성·수정 시간을 소유합니다. 업무 규칙은 두지 않습니다.
- `common/response`에는 `ApiResponse<T>(success, data)`, `ErrorResponse(success, code, message, traceId, data)` 및 페이지 응답 계약을 둡니다. `ErrorResponse.data`에는 안전한 부가 정보만 담습니다.
- 전역 HTTP 예외 처리와 공유 애플리케이션 설정은 각각 `common/exception`, `common/config`에 둡니다.
- 기능별 DB·외부 연동 설정은 해당 Infrastructure가 소유합니다.
- UseCase·Port·경계를 넘는 DTO는 호출에 필요한 public 접근을 제공합니다.
- Controller·Repository 구현 등은 프레임워크가 허용하는 최소 접근 수준을 사용합니다.
- Domain에 적용하는 Lombok 어노테이션과 예외는 코드 스타일 문서를 따릅니다.

## 7. 테스트와 변경 확인

- Domain: Spring 없이 생성 규칙·상태 전이 검증
- Service: UseCase 호출 순서와 Command 전달 검증
- UseCase: Port를 Fake·Mock으로 대체해 업무 흐름·결과·오류 검증
- Controller: 입력 검증·Command 변환·HTTP 응답과 오류 계약 검증
- Persistence: 직접 JPA 모델의 매핑·기술 예외 전파·MySQL 쿼리·Flyway·DB 제약 검증
- Client: 요청·응답 변환·타임아웃·오류 변환 검증
- ArchitectureTest: Domain·Application·API 의존 방향 검증
- BookApplicationTest: 실제 Spring Context·트랜잭션 프록시·HTTP→MySQL 결합 검증

Java·빌드·설정 변경 후 관련 테스트와 `./gradlew check`를 실행합니다.
`./gradlew test`는 Docker가 필요하지 않은 테스트를, `./gradlew integrationTest`는 Testcontainers MySQL이 필요한 테스트를 실행합니다.
`./gradlew check`는 두 작업을 모두 포함합니다. 실행 JAR는 `./gradlew bootJar`로 생성합니다.
테스트는 Docker가 없다고 자동 생략하지 않습니다.
API 변경은 DTO·spec·Controller 테스트, DB 변경은 Domain 모델·필요한 영속 전용 Entity·Port·마이그레이션을 함께 확인합니다.

## 8. 이전 구조 보관

전환 전 멀티모듈 버전은 `023-dev/ver2-book`의
`3ddbb6059b54f1522b72bdad6b5555a071e09a57`에 보관합니다.
Snowflake 도입 검토 브랜치는 이번 전환에 포함하지 않습니다.
