# AGENTS.md

Spring Boot + Java 25 단일 모듈 프로젝트의 개발 규칙입니다.
구조와 의존 방향은 이 문서를 기준으로 합니다. 저장소의 `.docs/ARCHITECTURE.md`는 이전 멀티모듈 구조의 기록입니다.

- Java 코드 작성·수정: Spotless와 이 문서의 Java 규칙을 적용합니다. 로컬 `.docs/.conventions/CODE_STYLE_CONVETIONS.md`가 있으면 추가 규칙을 확인합니다.
- Issue·Branch·Commit·PR 작업: 설치된 GitHub 스킬과 `.github` 템플릿·규칙 검사를 따릅니다.
- 스킬 선택·관리: 설치된 각 `SKILL.md`를 기준으로 하며, 로컬 `.docs/SKILL_DOCS.md`가 있으면 색인으로 사용합니다.

## 기술 스택

Java 25, Spring Boot 4, Gradle Kotlin DSL, Spring Web MVC, Spring Data JPA,
MySQL, Flyway, springdoc-openapi + Swagger UI, Lombok, JUnit 6 + AssertJ + Mockito를 사용합니다.

## 빌드 및 실행

```bash
./gradlew clean check
./gradlew bootJar
./gradlew bootRun
./gradlew test
./gradlew test --tests '*SampleUseCaseTest'
./gradlew spotlessApply
./gradlew test jacocoTestReport
```

Swagger UI는 `/swagger-ui/index.html`, OpenAPI 명세는 `/v3/api-docs`입니다.
문서 어노테이션은 Controller의 `api/spec` 인터페이스에서 관리합니다.

## 패키지 구조와 의존 방향

```text
com.book
├── BookApplication
├── core/{feature}
│   ├── api/{request,response,spec}
│   ├── application/{command,result,usecase,port}
│   ├── domain
│   └── infrastructure/{persistence,client}
├── common/exception
└── support/{web,config}
```

- Gradle 하위 모듈은 없습니다. `core`는 업무 기능을 묶는 패키지입니다.
- 필요한 패키지만 만듭니다. 빈 client·converter·config 패키지는 미리 만들지 않습니다.
- Controller는 구체 UseCase를 호출합니다. UseCase는 Domain과 Repository·Client Port를 조율합니다.
- Infrastructure는 Port를 구현합니다. Application과 API는 Infrastructure 구현체를 참조하지 않습니다.
- Domain은 Application·API·Infrastructure·Spring·JPA·Jackson에 의존하지 않습니다.
- 다른 기능의 Infrastructure를 직접 호출하지 않습니다. 기능 간 협력은 공개 UseCase 계약을 사용하고 순환 의존을 피합니다.
- 공통 오류 계약은 `common/exception`, HTTP 예외 처리는 `support/web`이 소유합니다.

## Spring 빈과 트랜잭션

- 필수 의존성은 `private final`과 `@RequiredArgsConstructor`로 주입합니다. 빌드 설정과 직접 생성자가 필요한 예외는 코드 스타일 문서를 따릅니다.
- `application/usecase`에 `@Service` 구체 클래스를 둡니다. 동일 역할의 UseCase 인터페이스와 Service를 기계적으로 쌍으로 만들지 않습니다.
- 한 UseCase는 한 업무 흐름을 담당합니다. 등록·조회·수정·삭제를 범용 Service 하나로 모으지 않습니다.
- 쓰기 트랜잭션은 UseCase의 public 메서드에 둡니다. 조회는 필요한 경우 `@Transactional(readOnly = true)`를 사용합니다.
- Application의 Spring 의존은 DI·트랜잭션에 필요한 범위로 제한합니다.
- Component Scan은 모든 기능을 포함하는 `com.book`을 기준으로 합니다.

## Java와 데이터 변환

- Command, Result, 요청·응답 DTO는 불변 데이터 전달이 목적이면 `record`를 우선합니다.
- Request의 `toCommand()`, Response의 `from(Result)`를 사용합니다. 별도 converter는 복잡하거나 재사용되는 변환에만 추가합니다.
- Command는 업무 전제조건을, Domain 생성자·메서드는 불변식과 상태 전이를 검증합니다.
- Domain과 JPA Entity는 분리합니다. Domain을 DTO처럼 작성하거나 Domain·Entity를 HTTP 응답으로 직접 반환하지 않습니다.
- Domain에 Lombok을 사용할 때는 코드 스타일의 적용 대상·예외·어노테이션 순서를 따릅니다. 생성 규칙을 우회하지 않습니다.
- `Optional`은 반환값에만 제한적으로 사용합니다. 의미 없는 setter·범용 Builder를 추가하지 않습니다.
- 재할당하지 않는 필드·지역 변수·매개변수의 `final` 규칙은 코드 스타일 문서를 따릅니다.
- 접근 수준은 호출·프레임워크 요구사항을 충족하는 최소 범위로 유지합니다.

## 외부 접근

- `application/port/{Feature}Repository`와 `infrastructure/persistence/repository/{Feature}RepositoryImpl`을 분리합니다.
- Spring Data Repository는 `{Feature}JpaRepository`, Entity는 `{Feature}Entity`, 변환은 `{Feature}PersistenceMapper`를 사용합니다.
- 외부 연동은 `{Feature}Client` Port와 `{Feature}ClientImpl` 구현체로 분리합니다. 변환기는 출처가 드러나는 `{Feature}ExternalMapper`를 사용합니다.
- JPA Entity·HTTP DTO·외부 SDK 타입을 Port와 UseCase의 입력·출력에 노출하지 않습니다.
- JPA·외부 SDK 예외는 공통 오류 계약으로 변환하고 상세 원문을 HTTP 응답에 노출하지 않습니다.
- 기능별 기술 설정은 해당 Infrastructure에, 여러 기능이 공유하는 애플리케이션 설정은 `support/config`에 둡니다.
- `common`에는 실제로 여러 기능에서 의미와 규칙이 같은 기술 중립 코드만 둡니다.

## 테스트와 검증

- Domain 테스트는 Spring Context 없이, UseCase 테스트는 Repository·Client Port를 Fake 또는 Mock으로 대체해 실행합니다.
- Controller 테스트는 입력 검증·Command 변환·HTTP 계약을 확인합니다.
- Persistence 테스트는 매핑·쿼리·DB 제약을, Client 테스트는 요청·응답 변환·타임아웃·오류 변환을 확인합니다.
- DB 통합 테스트는 격리된 Testcontainers MySQL을 사용합니다. Docker가 없다고 성공 처리하거나 자동 생략하지 않습니다.
- `ArchitectureTest`로 Domain·Application·API의 의존 방향을 검사합니다.
- Java·빌드·설정 변경 시 관련 테스트와 `./gradlew check`를 실행합니다.
- 문서 변경은 링크·경로·예시와 구현의 일치 여부를 확인합니다.
- Git 규칙 검사 변경은 `node --test .github/scripts/git-conventions.test.cjs`, workflow 변경은 `actionlint`로 검사합니다.
- 스킬 변경 시 frontmatter·호출 설정·참조를 확인하고, 로컬 `.docs/SKILL_DOCS.md`가 있으면 해당 협업 검증 시나리오도 확인합니다.
- 실행하지 못한 검증과 원인을 보고합니다. 캐시된 결과를 새 통합 테스트 성공으로 보고하지 않습니다.

## 변경 시 함께 확인할 파일

- API: Controller 계약 테스트, request·response, spec와 OpenAPI 명세
- DB: Domain, Entity, Repository Port·구현체, Mapper, `src/main/resources/db/migration`
- 외부 연동: Client Port·구현체, 외부 DTO, Mapper, 타임아웃·오류 처리
- 빌드: `build.gradle.kts`, `settings.gradle.kts`, CI workflow

## 금지 사항

- Domain에 `@Entity`, `@Column`, `@JsonProperty`, `@Service`를 추가하지 않습니다.
- UseCase에서 JpaRepository·EntityManager·RestClient·외부 SDK를 직접 사용하지 않습니다.
- Controller에 비즈니스 분기나 트랜잭션 로직을 작성하지 않습니다.
- 비밀번호·토큰·키·서명된 URL·외부 응답 원문을 로그에 남기지 않습니다.
- 요청받지 않은 구조 변경·공개 API 변경·의존성 추가를 함께 수행하지 않습니다.
