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
│       │   └── spec/
│       ├── application/
│       │   ├── command/
│       │   ├── result/
│       │   ├── usecase/
│       │   └── port/
│       ├── domain/
│       │   ├── {Feature}.java
│       │   └── exception/
│       └── infrastructure/
│           ├── persistence/
│           │   ├── entity/
│           │   ├── repository/
│           │   └── mapper/
│           └── client/
├── common/
│   ├── config/
│   ├── domain/
│   │   └── BaseTimeEntity.java
│   ├── exception/
│   ├── logging/
│   └── response/
│       ├── ApiResponse.java
│       ├── ErrorResponse.java
│       └── PageResponse.java
```

필요한 패키지만 생성합니다. converter, client, config, policy 등을 미리 만들지 않습니다.
테스트는 `src/test/java`에서 대상 코드의 패키지를 따릅니다.
설정과 Flyway 마이그레이션은 `src/main/resources`, 테스트 설정은 `src/test/resources`에 둡니다.

## 2. 의존 방향

```text
Controller → 구체 UseCase → Domain
                   ↓
          Repository·Client Port ← Infrastructure 구현체
```

- API는 UseCase와 Command·Result를 사용하고 Infrastructure를 직접 호출하지 않습니다.
- Application은 Domain과 자신이 정의한 Port에 의존합니다. JPA·HTTP·외부 SDK 타입을 사용하지 않습니다.
- Domain 모델은 Application, API, Infrastructure, Spring, Jackson을 참조하지 않습니다. JPA 매핑과 `common/domain`의 `BaseTimeEntity`는 허용합니다.
- Infrastructure는 Port를 구현하며 업무 모델을 직접 저장합니다. 도메인 대응 타입이 없는 실제 요구의 영속 전용 모델만 Entity로 분리하고, 예상 기능을 위한 Entity·테이블은 만들지 않습니다.
- 기능 간 협력은 공개 UseCase 계약으로 수행합니다. 다른 기능의 Repository 구현을 직접 참조하거나 순환 의존을 만들지 않습니다.

멀티모듈의 컴파일 격리 대신 `ArchitectureTest`로 핵심 패키지 의존 방향을 검사합니다.
패키지 배치만으로 의존 방향이 강제되지는 않습니다.

## 3. UseCase와 주입

`application/usecase`의 `{Feature}{Action}UseCase`는 `@Service` 구체 클래스입니다.
예: `SampleCreateUseCase`, `SampleQueryUseCase`.
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
| 저장 Port | application/port/{Feature}Repository | 저장·조회 계약 |
| JPA 저장소 | infrastructure/persistence/repository/{Feature}JpaRepository | Spring Data JPA |
| 저장 구현 | infrastructure/persistence/repository/{Feature}RepositoryImpl | Port 구현·오류 변환 |
| 영속성 변환 | infrastructure/persistence/mapper/{Feature}PersistenceMapper | 별도 표현이 필요할 때만 사용 |
| 영속 전용 Entity | infrastructure/persistence/entity | 도메인 대응 타입이 없는 실제 요구의 기록·기술 모델 |
| 외부 Port | application/port/{Feature}Client | 외부 기능 계약 |
| 외부 구현 | infrastructure/client | 외부 DTO·SDK·타임아웃·오류 변환 |

- 단순 필드 매핑은 Controller에서 직접 처리할 수 있습니다. 복잡하거나 재사용되는 변환만 `Request.toCommand()`, `Response.from(Result)` 또는 별도 converter로 분리합니다.
- Domain 모델은 별도 업무 Entity를 두지 않습니다. Domain 모델을 HTTP 응답으로 직접 반환하지 않습니다.
- 사용자 식별자는 API·Command·Port 전반에서 `userId`로 통일합니다.
- 영속 모델의 공개 생성자는 DB 필수값을 모두 받고, JPA 복원용 보호된 무인자 생성자만 유효성 검증을 우회할 수 있습니다.
- Command·Result·DTO는 불변 데이터 전달이 목적이면 record를 우선합니다.
- Domain은 불변식을 보장하는 생성자·팩터리와 상태 전이 메서드를 사용합니다.
- Mapper는 데이터 표현만 변환하고 업무 판단은 하지 않습니다.

## 5. 검증과 오류

HTTP 입력의 형식·길이·필수값은 Bean Validation과 Controller의 `@Valid`로 검증합니다.
Command는 생성 시 업무 전제를, Domain은 불변식과 상태 전이를 검증합니다.
권한이나 현재 저장 상태처럼 실행 시점에 확인할 조건은 UseCase가 Port를 통해 조회하여 검증합니다.

공통 오류 계약은 `common/exception/BusinessException`, `ErrorCode`, `CommonErrorCode`입니다.
`ErrorCode`는 오류 코드·메시지와 HTTP 상태 코드를 함께 제공합니다.
기능별 오류 코드는 `core/{feature}/domain/exception`에 둡니다.
JPA·외부 SDK 예외를 공통 계약으로 변환하고 `common/exception/GlobalExceptionHandler`가 HTTP 상태와 응답을 매핑합니다.
민감한 오류 원문이나 인증정보를 응답·로그에 노출하지 않습니다.

## 6. 공통 코드와 접근 수준

- `common`에는 기능에 종속되지 않고 여러 기능이 공유하는 코드를 둡니다. 추후 멀티모듈이나 독립 모듈로 분리할 때 기술 중립 코드를 우선합니다.
- `common/domain`에는 여러 JPA Entity가 공유하는 영속성 생명주기 타입만 둡니다. 업무 규칙은 두지 않습니다.
- `common/response`에는 여러 기능이 공유하는 `success/data` 응답과 페이지 응답 계약을 둡니다.
- 전역 HTTP 예외 처리와 공유 애플리케이션 설정은 각각 `common/exception`, `common/config`에 둡니다.
- 기능별 DB·외부 연동 설정은 해당 Infrastructure가 소유합니다.
- UseCase·Port·경계를 넘는 DTO는 호출에 필요한 public 접근을 제공합니다.
- Controller·Repository 구현 등은 프레임워크가 허용하는 최소 접근 수준을 사용합니다.
- Domain에 적용하는 Lombok 어노테이션과 예외는 코드 스타일 문서를 따릅니다.

## 7. 테스트와 변경 확인

- Domain: Spring 없이 생성 규칙·상태 전이 검증
- UseCase: Port를 Fake·Mock으로 대체해 업무 흐름·결과·오류 검증
- Controller: 입력 검증·Command 변환·HTTP 응답과 오류 계약 검증
- Persistence: 직접 JPA 모델의 매핑·기술 예외 변환·MySQL 쿼리·Flyway·DB 제약 검증
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
