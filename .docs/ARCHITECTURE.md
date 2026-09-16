# 아키텍처 & 개발 규칙

> Spring Boot + Java 기반 프로젝트입니다.
> 헥사고날 아키텍처(Ports and Adapters)에 따라 의존 방향과 역할을 분리합니다.

---

## 1. 아키텍처 핵심 규칙

### 의존 방향

헥사곤 내부의 의존은 반드시 Application에서 Domain으로 향합니다.

```text
Inbound Adapter ──→ Port(in) ──→ Application Service ──→ Domain
                                            │
                                            └──→ Port(out) ←── Outbound Adapter
```

Gradle 모듈의 컴파일 의존 방향은 다음과 같습니다.

```text
api ───────────────→ core ──→ common
                                  ↑
infrastructure:client ────┤
infrastructure:persistence┘

api ⇢ runtimeOnly ⇢ infrastructure:client
api ⇢ runtimeOnly ⇢ infrastructure:persistence
```

- `api`는 외부 요청이 들어오는 Inbound Adapter입니다.
- `core`는 Application, Port, Domain을 포함하는 헥사곤 내부입니다.
- `infrastructure:client`와 `infrastructure:persistence`는 Port(out)을 구현하는 Outbound Adapter입니다.
- `common`은 여러 모듈이 함께 사용하는 최소 공통 타입만 제공합니다.
- `core`는 `api`와 `infrastructure`를 참조하지 않습니다.
- `api`는 infrastructure 구현체를 직접 import하지 않습니다.
- Application Service는 Adapter 구현체가 아니라 Port 인터페이스에만 의존합니다.

### 절대 금지하는 의존

```text
Domain       → Application
Domain       → API 또는 Infrastructure
Application  → API 또는 Infrastructure 구현체
API          → Infrastructure 구현체의 클래스
```

Domain과 Application에는 Spring Web, JPA, AWS SDK, 외부 API DTO 같은 기술 세부사항을 두지 않습니다.

---

## 2. 모듈별 책임

| 모듈 | 헥사고날 역할 | 책임 |
|---|---|---|
| `common` | Shared Kernel | 공통 예외 계약, 공통 모델, JSON·시간·ID 등 범용 기능 |
| `core` | Application Core | UseCase, Port, Application Service, Domain 모델과 정책 |
| `api` | Inbound Adapter | Controller, 요청·응답 DTO, 인증 컨텍스트, API 예외 변환 |
| `infrastructure:persistence` | Outbound Adapter | JPA Entity, Spring Data Repository, 조회 구현, 영속성 Mapper |
| `infrastructure:client` | Outbound Adapter | 외부 API와 클라우드 서비스 연동 구현 |

모듈을 나누는 기준은 기술이 아니라 의존 방향입니다. 외부 기술을 교체해도 `core`의 UseCase와 Domain 규칙이 바뀌지 않아야 합니다.

---

## 3. 패키지 구조

### core

```text
core/src/main/java/com/example/{project}/core/
├── application/
│   └── {feature}/
│       ├── command/                 # UseCase 입력 Command
│       ├── port/
│       │   ├── in/                  # 외부에서 호출하는 UseCase 인터페이스
│       │   │   └── result/          # UseCase 반환 Result
│       │   └── out/                 # DB·외부 시스템에 요청하는 인터페이스
│       └── service/                 # Port(in) 구현과 업무 흐름 조율
├── domain/
│   └── {feature}/
│       ├── model/                   # 순수 Domain 모델과 값 객체
│       ├── policy/                  # 여러 Domain 객체에 걸친 정책
│       └── exception/               # 기능별 Domain 예외와 ErrorCode
└── support/                         # Core에 꼭 필요한 기술 중립 보조 코드
```

`{feature}`에는 `auth`, `user`, `product`, `coupon`, `order`, `payment`처럼 업무 기능명을 사용합니다.

### api

```text
api/src/main/java/com/example/{project}/api/
├── adapter/in/web/
│   └── {feature}/
│       ├── {Feature}Controller.java
│       ├── request/
│       └── response/
└── support/
    ├── auth/
    ├── logging/
    └── web/
```

Controller는 Port(in) UseCase만 호출합니다. 각 Adapter가 자신의 요청·응답 계약을 소유합니다.

### infrastructure:persistence

```text
infrastructure/persistence/src/main/java/com/example/{project}/infrastructure/persistence/
├── {feature}/
│   ├── entity/                      # JPA Entity
│   ├── repository/                  # JpaRepository와 Port(out) 구현체
│   └── mapper/                      # Domain ↔ Entity 변환
└── support/
    ├── converter/
    └── jpa/
```

### infrastructure:client

```text
infrastructure/client/src/main/java/com/example/{project}/infrastructure/client/
├── {system}/
│   ├── dto/
│   ├── mapper/
│   └── adapter/
└── support/
```

외부 시스템별 패키지 안에는 Port(out) 구현체와 외부 API 전용 DTO를 둡니다. 외부 응답 DTO를 Domain 모델처럼 사용하지 않습니다.

### common

```text
common/src/main/java/com/example/{project}/common/
├── config/                          # 공통 기술 설정
├── exception/                       # BusinessException, ErrorCode, 공통 오류
├── logger/                          # 공통 로거
└── util/                            # 범용 유틸리티
```

`common`은 편의를 위한 임시 보관소가 아닙니다. 두 개 이상의 기능에서 의미와 규칙이 같은 코드만 이동합니다.
`BusinessException`과 `ErrorCode`는 HTTP에 의존하지 않는 오류 계약입니다. HTTP 응답 본문, 상태 코드 매핑과 예외 처리는 `api/support/web`에서 소유합니다.

---

## 4. 파일명 규칙

### Domain

| 유형 | 규칙 | 예시 |
|---|---|---|
| Domain 객체 | `{명사}.java` | `Member.java` |
| 값 객체 | `{명사}.java` | `Email.java` |
| 상태 enum | `{명사}Status.java` | `MemberStatus.java` |
| 정책 | `{명사}Policy.java` | `MemberPolicy.java` |
| Domain 예외 | `{명사}Exception.java` | `MemberNotFoundException.java` |
| Domain 오류 코드 | `{명사}ErrorCode.java` | `MemberErrorCode.java` |

### Application

| 유형 | 규칙 | 예시 |
|---|---|---|
| Command | `{명사}{동작}Command.java` | `MemberCreateCommand.java` |
| Port(in) | `{명사}{동작}UseCase.java` | `MemberCreateUseCase.java` |
| Port(in) 반환값 | `{명사}{동작}Result.java` | `MemberCreateResult.java` |
| 저장 Port(out) | `{명사}Repository.java` | `MemberRepository.java` |
| 외부 연동 Port(out) | `{기능}Client.java` | `PaymentClient.java` |
| 명령 Service | `{명사}{동작}Service.java` | `MemberCreateService.java` |
| 조회 Service | `{명사}QueryService.java` | `MemberQueryService.java` |

하나의 Service에 등록, 조회, 수정, 삭제를 모두 넣지 않습니다.

```text
허용: MemberCreateService, MemberQueryService, MemberModifyService
금지: MemberService 하나에 모든 기능 구현
```

### Inbound Adapter

| 유형 | 규칙 | 예시 |
|---|---|---|
| Controller | `{명사}Controller.java` | `MemberController.java` |
| 요청 DTO | `{명사}{동작}Request.java` | `MemberCreateRequest.java` |
| 응답 DTO | `{명사}{동작}Response.java` | `MemberDetailResponse.java` |

### Outbound Adapter

| 유형 | 규칙 | 예시 |
|---|---|---|
| JPA Entity | `{명사}Entity.java` | `MemberEntity.java` |
| Spring Data Repository | `{명사}JpaRepository.java` | `MemberJpaRepository.java` |
| Port(out) 구현체 | `{명사}RepositoryImpl.java` | `MemberRepositoryImpl.java` |
| 영속성 Mapper | `{명사}PersistenceMapper.java` | `MemberPersistenceMapper.java` |
| 외부 연동 구현체 | `{기능}ClientImpl.java` | `PaymentClientImpl.java` |
| 외부 연동 Mapper | `{명사}ExternalMapper.java` | `PaymentExternalMapper.java` |
| 외부 API DTO | `{명사}{Request|Response}.java` | `PaymentTokenResponse.java` |

Mapper 이름에는 데이터의 출처를 드러냅니다. `MemberMapper`처럼 범위가 모호한 이름은 사용하지 않습니다.

---

## 5. 레이어별 책임

### Domain

- 핵심 상태와 비즈니스 규칙을 소유합니다.
- 생성 시점의 불변식은 생성자나 정적 팩토리 메서드에서 검증합니다.
- 상태 변경은 의미가 드러나는 메서드로 수행합니다.
- JPA, Spring Web, Jackson, AWS SDK 어노테이션을 사용하지 않습니다.
- JPA Entity와 외부 API DTO를 참조하지 않습니다.
- 단순 getter/setter 모음이 아니라 스스로 규칙을 지키는 모델로 작성합니다.

### Application Service

- Port(in)을 구현하고 하나의 UseCase 흐름을 완성합니다.
- Domain 객체를 생성하고 조율합니다.
- DB나 외부 시스템에는 Port(out)을 통해서만 접근합니다.
- `JpaRepository`, `EntityManager`, `RestClient`, `S3Client` 같은 구현 기술을 직접 사용하지 않습니다.
- API 요청·응답 DTO를 입력·출력으로 사용하지 않습니다.
- 다른 Service를 연쇄 호출해 우회하기보다 필요한 Port와 Domain 협력을 명시합니다.

### Port(in)

- Inbound Adapter가 호출할 유스케이스 계약입니다.
- 입력은 Command, 출력은 Result를 사용합니다.
- HTTP 상태 코드, 쿠키, 헤더를 노출하지 않습니다.

### Port(out)

- Application이 필요로 하는 저장과 외부 기능을 추상화합니다.
- 기술 이름보다 업무 목적을 드러냅니다.
- 특정 구현체의 DTO, 예외, SDK 타입을 메서드 시그니처에 노출하지 않습니다.

### Controller (Inbound Adapter)

- Port(in) 인터페이스 타입을 주입받습니다.
- 요청 DTO를 Command로 변환하고 Result를 응답 DTO로 변환합니다.
- Domain 객체와 JPA Entity를 그대로 반환하지 않습니다.
- 인증 정보, 전송 형식, HTTP 오류 표현만 담당합니다.
- 비즈니스 분기와 트랜잭션 로직을 작성하지 않습니다.

### Persistence Adapter

- Repository Port(out)을 구현합니다.
- Domain과 JPA Entity를 명시적으로 변환합니다.
- Spring Data JPA와 JPQL 또는 Criteria API 같은 조회 기술은 이 모듈 안에서만 사용합니다.
- DB 제약조건과 인덱스는 영속성 규칙으로 관리하되, Domain 규칙을 대체하지 않습니다.

### External Client Adapter

- 외부 API 호출, 인증, 타임아웃, 재시도, 응답 파싱을 담당합니다.
- 외부 오류를 Core가 이해하는 예외나 결과로 변환합니다.
- 외부 DTO와 SDK 타입이 Port 경계를 넘어가지 않게 합니다.

---

## 6. DTO와 모델 위치

| 종류 | 위치 |
|---|---|
| 요청 DTO | `api/.../adapter/in/web/{feature}/request/` |
| 응답 DTO | `api/.../adapter/in/web/{feature}/response/` |
| Command | `core/.../application/{feature}/command/` |
| Port(in) Result | `core/.../application/{feature}/port/in/result/` |
| Domain 모델 | `core/.../domain/{feature}/model/` |
| JPA Entity | `infrastructure/persistence/.../{feature}/entity/` |
| 조회 Projection | `infrastructure/persistence/.../{feature}/repository/` |
| 외부 API DTO | `infrastructure/client/.../{system}/dto/` |
| 기술 중립 공통 오류 계약 | `common/.../exception/` |
| HTTP 오류 응답·상태 코드 매핑 | `api/.../support/web/` |

같은 필드 구성을 가졌다는 이유만으로 서로 다른 경계의 DTO를 공유하지 않습니다. HTTP 계약, UseCase 계약, Domain 모델, DB 모델은 변경 이유가 서로 다릅니다.

---

## 7. 변환 규칙

```text
Request.toCommand()              HTTP 입력 → Command
Response.from(Result)            Result → HTTP 응답
PersistenceMapper.toEntity()     Domain → JPA Entity
PersistenceMapper.toDomain()     JPA Entity → Domain
ExternalMapper.toDomain()        외부 DTO → Core 모델
```

- 변환 책임은 데이터를 소유한 경계에 둡니다.
- Controller에서 필드를 하나씩 조립하는 코드를 반복하지 않습니다.
- Domain이 HTTP DTO나 JPA Entity의 변환 메서드를 소유하지 않습니다.
- Mapper에 비즈니스 판단을 넣지 않습니다.

---

## 8. 유효성 검증 규칙

| 단계 | 검증 대상 | 방법 |
|---|---|---|
| Inbound Adapter | 형식, 필수값, 문자열 길이 | Bean Validation (`@NotBlank`, `@Email`, `@Size`) |
| Command | UseCase 입력의 업무 전제 | 생성자 또는 정적 팩토리 메서드 |
| Domain | 상태 불변식과 상태 전이 | Domain 메서드 |
| Infrastructure | 외부 응답, DB·SDK 제약 | Adapter 내부 검증과 예외 변환 |

Application Service는 유효하게 생성된 Command를 신뢰합니다. 다만 사용자 권한, 현재 저장 상태처럼 실행 시점에만 확인할 수 있는 조건은 Service가 Port를 통해 조회한 뒤 검증합니다.

---

## 9. 예외 처리 규칙

- 공통 예외 계약은 `common`의 `BusinessException`과 `ErrorCode`를 사용합니다.
- 기능별 예외와 ErrorCode는 `core/domain/{feature}/exception/`에 둡니다.
- Infrastructure 예외를 SDK·JPA 예외 그대로 Core나 API에 노출하지 않습니다.
- API 전역 예외 처리기가 Core 예외를 HTTP 응답 규격에 맞게 변환합니다.
- 예외 메시지에 토큰, 비밀번호, 외부 응답 원문 같은 민감정보를 넣지 않습니다.

---

## 10. 접근 제어 규칙

| 대상 | 기본 접근 수준 |
|---|---|
| Port(in), Port(out) | `public` |
| 모듈 경계를 넘는 Command와 Result | `public` |
| Domain 모델과 값 객체 | `public` |
| 패키지 내부 정책·도우미 | package-private |
| Application Service 구현체 | package-private 권장 |
| Inbound·Outbound Adapter 구현체 | package-private 권장 |
| JPA Entity | 프레임워크 요구사항을 충족하는 최소 접근 수준 |

Spring 프록시, 리플렉션, 테스트 접근 때문에 `public`이 필요하면 예외를 허용합니다. 편의를 위해 모든 클래스를 `public`으로 열지는 않습니다.

---

## 11. 자주 하는 실수

```text
금지: Controller에서 Domain 객체를 그대로 반환
대안: Result를 Response로 변환

금지: Application Service에서 JpaRepository나 EntityManager 직접 사용
대안: Port(out) Repository를 주입

금지: Domain에 @Entity, @Column, @JsonProperty 추가
대안: JPA Entity와 API DTO를 각 Adapter에 분리

금지: core에서 infrastructure 구현체 import
대안: Core에 Port를 정의하고 infrastructure가 구현

금지: api에서 persistence 또는 client 구현체 import
대안: API는 Port(in)만 호출하고 구현체는 런타임에 주입

금지: Application Service에서 RestClient, 외부 SDK, AWS SDK 직접 사용
대안: 목적 중심의 Client Port를 정의

금지: 외부 API DTO를 Domain 모델로 재사용
대안: ExternalMapper에서 Core 모델로 변환

금지: JPA Entity를 Repository Port의 반환 타입으로 노출
대안: PersistenceMapper를 거쳐 Domain으로 반환

금지: 하나의 Service에 등록, 조회, 수정, 삭제를 모두 구현
대안: 유스케이스 단위로 Service 분리

금지: 이름이 모호한 Mapper 또는 Client 작성
대안: PersistenceMapper, ExternalMapper처럼 출처와 역할 명시

금지: common에 특정 기능 전용 코드를 이동
대안: 해당 feature의 Domain 또는 Application 패키지에 유지
```

---

## 12. 요청 처리 흐름

### HTTP 요청 처리

```text
[HTTP 요청]
    ↓
Controller (api)
    요청 검증 → Request.toCommand()
    ↓
Port(in) UseCase (core)
    ↓
Application Service (core)
    Domain 조율 → Port(out) 호출
    ↓
Repository 또는 Client Port(out) (core)
    ↓
Persistence 또는 External Client Adapter (infrastructure)
    ↓
[DB 또는 외부 시스템]
    ↓
Result → Response → HTTP 응답
```

---

## 13. 변경 시 확인 사항

### API 변경

- Controller가 Port(in)만 참조하는지 확인합니다.
- Request와 Response DTO를 갱신합니다.
- Controller 계약 테스트를 갱신합니다.

### DB 변경

- Domain과 JPA Entity를 분리했는지 확인합니다.
- Repository Port와 구현체의 반환 타입에 Entity가 노출되지 않는지 확인합니다.
- Mapper, DB 제약조건, 인덱스, 마이그레이션을 함께 검토합니다.

### 외부 연동 변경

- Core에는 Port만 추가했는지 확인합니다.
- 인증정보와 외부 DTO가 infrastructure 밖으로 새지 않는지 확인합니다.
- 타임아웃, 오류 변환, 민감정보 로깅 방지를 검토합니다.
