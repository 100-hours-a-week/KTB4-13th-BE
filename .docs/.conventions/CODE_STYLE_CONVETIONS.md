# 코드 스타일 컨벤션

## 1. 기본 원칙

- [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)를 기본 스타일 가이드로 사용합니다.
- 아래에 명시한 프로젝트의 포맷 규칙을 기본 가이드보다 우선합니다.
- 설계 체크리스트는 리뷰 지침으로 사용합니다. 수치 제한을 맞추기 위해 클래스나 메서드를 추가하지 않으며, 현재 변경하는 코드에 적용합니다.

## 2. 설계 리뷰 체크리스트

### 제어 흐름을 쉽게 이해할 수 있는가?

- 중첩을 줄일 수 있다면 조기 반환을 우선합니다. 분리할 동작의 책임이 명확할 때 메서드로 추출합니다.
- 선택지를 이해하기 쉬워진다면 `if`/`else` 또는 `switch` 표현식을 사용합니다. 깊게 중첩된 분기는 피합니다.

### 삼항 연산자를 사용하지 않았는가?

- 삼항 연산자(`condition ? value1 : value2`)를 사용하지 않습니다.
- 조건에 따른 값이나 동작은 `if`/`else` 또는 `switch`로 명시적으로 작성합니다.

### 값 객체가 도메인 규칙을 표현하는가?

- 불변식, 도메인 행위 또는 실수를 방지하는 의미 있는 구분을 소유할 때 값 객체를 도입합니다.
- 단순한 값과 경계의 DTO에는 원시값과 문자열을 사용할 수 있습니다. 체크리스트를 만족시키기 위해서만 감싸지 않습니다.

### 컬렉션이 도메인 행위를 소유하는가?

- 컬렉션 전체에 적용되는 규칙이나 행위가 있으면 일급 컬렉션을 사용합니다.
- 데이터 전달에는 일반 컬렉션으로 충분합니다. 필요한 경우 불변 뷰나 방어적 복사로 외부에서 도메인 상태를 변경하지 못하게 합니다.

### 필드와 매개변수가 하나의 개념에 속하는가?

- 관련된 상태와 행위를 함께 둡니다. 필드 개수가 일정 수를 넘어서가 아니라 서로 다른 책임이 있을 때 클래스를 분리합니다.
- 매개변수들이 의미 있는 개념을 표현하면 하나로 묶습니다. 개수를 줄이기 위해서만 매개변수 객체를 만들지 않습니다.
- DTO, record, JPA Entity, Mapper는 계약을 표현하기 위해 여러 필드나 매개변수가 필요할 수 있습니다.

### 도메인이 자신의 상태 전이를 제어하는가?

- 불변식을 우회하는 공개 setter 대신 도메인 메서드로 불변식과 상태 전이를 보장합니다.
- 호출자가 도메인 상태를 알아야 한다면 읽기 전용 접근자를 사용할 수 있습니다. DTO 접근자나 프레임워크가 요구하는 Entity 접근 방식이 도메인 행위를 대신하지는 않습니다.

### Entity와 Domain 모델에 Lombok 표준 어노테이션을 적용했는가?

JPA Entity와 Lombok을 사용하는 Domain 모델에는 클래스 선언 바로 위에 다음 어노테이션을 아래 순서대로 배치합니다.

```java
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Accessors(fluent = true)
```

- `AccessLevel`, `Getter`, `NoArgsConstructor`, `Accessors`를 명시적으로 import하며 와일드카드 import를 사용하지 않습니다.
- `@Accessors(fluent = true)`를 적용하면 호출자는 `getName()` 대신 `name()`을 사용합니다.
- `@NoArgsConstructor(access = AccessLevel.PROTECTED)`로 만든 보호된 무인자 생성자는 JPA 복원 또는 인프라 경계를 위한 것입니다. Domain 생성은 불변식을 보장하는 생성자, 팩터리, Command 또는 도메인 메서드를 거칩니다.
- `record`, enum, interface, 도메인 예외, 정책, 무인자 생성으로 유효하지 않은 Domain 상태가 만들어질 수 있는 클래스에는 이 어노테이션 묶음을 적용하지 않습니다. 적용 대상 모델에서 예외가 필요하면 사유를 기록합니다.

### JPA Entity의 공통 식별자 매핑을 일관되게 했는가?

- 여러 Domain Entity가 `id`, `status`, `created_at`, `updated_at`, `deleted_at`을 공유하면 `common/domain/BaseEntity`를 상속합니다.
- `@Id`와 `@GeneratedValue(strategy = GenerationType.IDENTITY)`는 `BaseEntity`에 한 번만 선언하고, 하위 Entity에 같은 식별자 필드를 중복 선언하지 않습니다.
- 기존 ID를 전달하는 복원·테스트 생성자는 `super(id)`를 호출합니다. 신규 객체는 ID를 직접 만들지 않고 `null`로 생성해 DB 생성 전략을 따릅니다.
- `BaseEntity`의 공통 컬럼이 필요하지 않은 Entity까지 상속시키지 않습니다. 해당 Entity는 필요한 식별자 매핑을 자체적으로 선언합니다.
- ID만 공유한다는 이유로 별도 `BaseIdEntity`를 미리 만들지 않습니다. 실제로 서로 다른 생명주기 조합이 생길 때만 공통 타입을 분리합니다.

### Spring 빈의 생성자 주입에 `@RequiredArgsConstructor`를 사용했는가?

- Controller, UseCase·Application Service, Repository·Client 구현체의 필수 의존성은 `private final` 필드로 선언하고 `@RequiredArgsConstructor`로 주입합니다. 필드 대입만 하는 생성자는 직접 작성하지 않습니다.
- 필드·setter 주입 대신 생성자 주입을 사용합니다. 생성자가 하나라면 `@Autowired`는 생략합니다.
- 생성자에서 검증, 정규화, 추가 초기화 또는 `super(...)` 호출이 필요하면 직접 작성합니다. 수동 생성자와 같은 시그니처를 만드는 `@RequiredArgsConstructor`는 함께 사용하지 않습니다.
- `@Qualifier`, `@Value` 등 생성자 매개변수 어노테이션이 필요하면 직접 생성자를 작성합니다. 필드 어노테이션이 Lombok 생성자 매개변수에 자동 복사된다고 가정하지 않습니다.
- Domain과 Entity의 생성 규칙에는 이 DI 패턴을 일괄 적용하지 않습니다. 불변식을 검증하는 생성자·팩터리와 JPA 기본 생성자는 각각의 목적에 맞게 유지하고, 데이터 전달 타입은 기존 `record` 우선 규칙을 따릅니다.
- `final`만으로 null 검증이 생성되지는 않습니다. 필수값 검증이 필요하면 `@NonNull` 또는 명시적인 생성자 검증을 사용합니다.
- 실제 적용 전 해당 빌드의 Lombok `compileOnly`와 `annotationProcessor` 설정을 확인합니다. 테스트 소스에서도 사용한다면 테스트용 설정도 확인합니다.

```java
@RestController
@RequiredArgsConstructor
class CartController {
    private final CartService cartService;
}
```

위 예시는 의존성 선언 부분만 보여줍니다. 생성 대상 필드와 어노테이션 복사 설정은 [Lombok 공식 문서](https://projectlombok.org/features/constructor)를 따릅니다.

### 기능별 Service orchestration layer를 유지하는가?

- Controller는 `{Feature}Service`만 호출하고 UseCase·Repository·Adapter를 직접 주입하지 않습니다.
- `{Feature}Service`는 기능의 CRUD와 추가 동작을 담당하는 UseCase들의 실행 순서를 조율합니다. 현재 UseCase가 하나뿐이어도 이후 확장 지점으로 유지합니다.
- Service에는 Domain 규칙, 트랜잭션, JPA Repository 호출을 넣지 않습니다. 트랜잭션과 업무 흐름은 각 UseCase가 담당합니다.
- Service가 단순히 하나의 UseCase를 위임하는 현재 단계도 유효한 구조입니다. 기능이 늘어날 때 Controller 계약을 바꾸지 않고 Service 안에서 조율 대상을 추가합니다.

### API 경계의 Converter

- 신규로 만들거나 수정하는 HTTP API는 요청 DTO·경로 변수·쿼리 파라미터를 직접 조합해 Command를 만들지 않고, 기능별 `api/converter/{Feature}CommandConverter`를 호출합니다. 기존 `Request.toCommand()`는 해당 기능을 수정할 때 함께 정리합니다.
- CommandConverter는 표현 변환만 담당합니다. 입력 검증은 Bean Validation, 업무 전제조건은 Command·UseCase·Domain이 담당합니다.
- UseCase 결과를 응답 DTO로 바꾸는 변환이 복잡하거나 여러 Controller에서 재사용될 때만 `api/converter/{Feature}ResultConverter`를 둡니다.
- Converter에는 트랜잭션, 저장소 호출, 외부 API 호출, 비즈니스 분기를 넣지 않습니다.
- HTTP 입력이 없는 API에는 CommandConverter를 만들지 않습니다. 응답 변환이 없거나 `Void` 응답이면 ResultConverter도 만들지 않습니다.

### 호출이 책임 경계를 지키는가?

- 다른 객체의 내부 구조를 따라 들어가 그 객체의 일을 대신하지 않습니다. 해당 행위를 책임지는 객체에 동작을 둡니다.
- 읽기 쉽다면 Fluent API, Stream 파이프라인, DTO 매핑, assertion 체이닝을 허용합니다. 점의 개수만으로 결합도를 판단하지 않습니다.

### 메서드와 클래스의 책임이 명확한가?

- 각 메서드와 클래스는 일관된 하나의 책임에 집중합니다.
- 이해하기 쉬워진다면 서로 다른 책임을 분리합니다. 단순히 코드를 짧게 만들기 위해 분리하지 않습니다.

## 3. 입력 검증과 오류 계약

### 외부 입력에 Jakarta Validation을 활용했는가?

- API DTO의 필수값, 범위, 길이, 형식 제약에는 `jakarta.validation.constraints`의 `@NotBlank`, `@NotNull`, `@Min`, `@Max`, `@Size`, `@Pattern`을 우선 사용합니다.
- Controller에서 `@Valid` 또는 `@Validated`로 검증을 활성화하고, [`GlobalExceptionHandler`](../../src/main/java/com/book/common/exception/GlobalExceptionHandler.java)의 HTTP 오류 계약을 따릅니다.
- 같은 단순 HTTP 입력 검증을 DTO 생성자에 중복 작성하지 않습니다. Command와 Domain 객체는 HTTP를 거치지 않는 호출에서도 자신의 전제조건과 불변식을 보장해야 합니다.
- 공통 비즈니스 오류에는 [`CoreException`](../../src/main/java/com/book/common/exception/CoreException.java)과 [`ErrorCode`](../../src/main/java/com/book/common/exception/ErrorCode.java) 계약을 사용하고, `common/exception`에서 HTTP 응답으로 변환합니다.
- null 기본값, 입력 정규화, 필드 간 조건, 도메인 불변식처럼 어노테이션만으로 표현하기 어려운 규칙은 생성자, Application 또는 Domain 계층에 둡니다.
- 제약을 추가하면 잘못된 HTTP 입력에 대해 예상한 상태 코드와 오류 응답이 반환되는지 테스트합니다.

## 4. 포맷과 Java 작성 규칙

### 들여쓰기에 공백 4개를 사용했는가?

- 들여쓰기 한 단계에 공백 4개를 사용합니다.
- 탭 문자를 사용하지 않습니다.
- 줄을 이어 쓸 때는 공백 8개로 들여씁니다.

### 한 줄을 120자 이내로 작성했는가?

- 각 줄의 길이는 120자 이내로 제한합니다.

### 제어문에 항상 중괄호를 사용했는가?

- 본문이 한 줄이어도 `if`, `for`, `while`에 중괄호를 사용합니다.
- 여는 중괄호는 제어문이나 선언과 같은 줄에 둡니다(K&R 스타일).

### 와일드카드 import를 피했는가?

- `import *`를 사용하지 않습니다.
- static import와 일반 import를 구분합니다.

### Java 파일마다 최상위 타입을 하나만 정의했는가?

- 각 `.java` 파일에는 최상위 class, interface, enum 또는 record를 하나만 정의합니다.
- 모든 class, interface, enum 및 record는 다른 타입 내부가 아닌 별도 `.java` 파일의 최상위 타입으로 선언합니다. 타입 간 소속은 패키지와 이름으로 표현합니다.

### 오버로딩한 메서드를 함께 배치했는가?

- 오버로딩한 메서드는 서로 인접하게 배치합니다.

### 선언문마다 변수를 하나만 선언했는가?

- 하나의 선언문에는 변수 하나만 선언합니다.

### 지역 변수를 사용하는 위치 가까이에 선언했는가?

- 지역 변수는 처음 사용하는 구문 가까이에 선언합니다.

### 변수와 매개변수에 `final`을 명시했는가?

- 재할당하지 않는 지역 변수에는 `final`을 명시합니다. `final var` 선언과 향상된 `for`문의 변수도 포함합니다. 사실상 final인 상태만으로는 이 규칙을 충족하지 않습니다.
- 메서드와 생성자의 매개변수에 `final`을 명시하고 재할당하지 않습니다. 변환한 값은 새로운 `final` 지역 변수에 저장합니다.
- 람다 매개변수를 선언할 때는 타입 또는 `var`와 함께 `final`을 명시합니다. 예: `(final var item) -> item.name()`.
- 생성 이후 값이나 참조가 바뀌지 않는 필드에는 `final`을 명시합니다. 생성자로 주입하는 의존성도 포함합니다. 도메인 상태 전이나 프레임워크 요구사항으로 재할당해야 하는 필드는 non-final로 유지합니다.
- 반복문의 카운터나 누적 변수처럼 재할당이 필요한 지역 변수에는 `final`을 생략합니다. 변수에 final을 붙이기 위해서만 가변 래퍼를 도입하지 않습니다.
- record 컴포넌트처럼 Java 문법상 허용되지 않는 위치에는 `final`을 붙이지 않습니다. 참조에 final을 붙여도 참조 대상 객체나 컬렉션까지 불변이 되는 것은 아닙니다.

```java
String normalizeName(final String name) {
    final var normalizedName = name.strip();
    return normalizedName;
}
```

### Java 명명 규칙을 따랐는가?

- 패키지 이름은 `lowercase`로 작성합니다.
- class, interface, enum, record 이름은 `UpperCamelCase`로 작성합니다.
- 메서드와 필드 이름은 `lowerCamelCase`로 작성합니다.
- 의미상 상수인 경우에만 `UPPER_SNAKE_CASE`를 사용합니다.
- 모든 `static final` 멤버를 상수로 취급하지 않습니다.
- `userID` 대신 `userId`를 사용합니다.

### 필요한 곳에 `@Override`를 사용했는가?

- 상속받은 동작을 재정의하거나 구현하는 메서드에는 `@Override`를 붙입니다.

### 예외를 명시적으로 처리했는가?

- 예외를 아무 처리 없이 무시하지 않습니다.
- 각 예외를 상황에 맞게 처리하거나 기록하거나 전파합니다.

### static 멤버에 클래스 이름으로 접근했는가?

- static 필드와 메서드는 인스턴스가 아닌 클래스 이름으로 접근합니다.

### `finalize()` 사용을 피했는가?

- `finalize()`를 선언하거나 사용하지 않습니다.

### 공개 API를 문서화했는가?

- 공개 API에는 기본적으로 Javadoc을 작성합니다.

### 최신 Java 문법에도 같은 규칙을 적용했는가?

- `switch` 표현식, `record`, 텍스트 블록, Markdown 형식의 Javadoc 등 최신 Java 기능에도 같은 스타일 규칙을 적용합니다.
