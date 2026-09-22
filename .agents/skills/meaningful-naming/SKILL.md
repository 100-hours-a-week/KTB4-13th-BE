---
name: meaningful-naming
description: Choose, review, or safely rename intention-revealing Java/Spring names for classes, methods, fields, parameters, constants, and functions, including predicates, queries, commands, conversions, factories, and function boundaries. Use for naming or rename reviews; do not use for formatter-only changes, broad architecture reviews, or behavior changes.
---

# Meaningful Naming

이 스킬은 이름만 예쁘게 바꾸는 규칙이 아니라, 이름이 반환값·부수효과·도메인 의도를 정확히 설명하도록 돕습니다. 이름을 바꾸기 전에 선언부와 모든 호출부를 확인하고, 변경 요청이 있을 때만 실제 코드를 수정합니다.

## 판단 순서

1. 공개 API·DB·JSON 이름·프레임워크 Override처럼 외부 계약에 해당하는지 확인하고, 해당하면 호환성을 우선합니다.
2. 대상의 반환 타입, 상태 변경, 외부 효과, 검증·사전조건 실패 시 예외 발생 여부, 호출부를 확인합니다.
3. 대상의 역할을 `predicate`, `query`, `command`, `guard`, `conversion`, `factory` 중 하나로 분류합니다.
4. 기존 프로젝트에서 같은 개념에 사용하는 단어를 검색합니다. `find`, `get`, `fetch`처럼 의미가 겹치는 단어를 새로 섞지 않습니다.
5. 이름만 읽어도 대상·조건·동작이 드러나는지 확인합니다.
6. 이름 변경 시 모든 호출부, 테스트, 문서, API 설명을 함께 검색하고 갱신합니다.
7. 포맷과 관련 테스트를 실행합니다. 실행하지 못한 검증은 성공으로 보고하지 않습니다.

## 분석 대상과 심각도

분석 요청을 받으면 요청된 파일·디렉터리 범위에서 다음 식별자를 확인합니다.

- 클래스, 인터페이스, enum, record와 파일명
- 메서드와 생성자
- 필드, 매개변수, 지역 변수, 상수
- API 경로·요청 필드·DB 이름은 사용자가 분석 범위에 포함했을 때만 확인합니다.

발견 사항은 이름의 취향이 아니라 실제 오해 가능성과 유지보수 비용을 기준으로 분류합니다.

| 심각도 | 기준 | 예시 |
| --- | --- | --- |
| Critical | 이름과 실제 동작이 달라 버그를 유발할 수 있음 | `getUser()`가 내부 상태를 변경함 |
| Major | 의도나 책임을 파악하려면 구현을 다시 읽어야 함 | `process(data)`, `isValidQuantity` |
| Minor | 약어·표기·프로젝트 용어가 일관되지 않음 | 같은 조회에 `find`와 `fetch`를 혼용함 |

단순한 개인 취향 차이는 이슈로 과장하지 말고 선택 가능한 제안으로 표시합니다.

## 이름 규칙

### Predicate

boolean을 반환하고 상태를 변경하지 않는 메서드는 질문형 이름을 사용합니다.

- `is`: 상태나 조건 — `isQuantityInRange`, `isActive`
- `has`: 보유·존재 — `hasItems`, `hasPermission`
- `can`: 가능 여부 — `canCheckout`
- `should`: 정책상 판단 — `shouldRetry`

`is`, `has`, `can`, `should` 메서드는 예외를 던지거나 상태를 변경하는 검증·명령 메서드로 만들지 않습니다. 범위처럼 판단 기준이 있는 경우 모호한 `isValid`보다 대상을 이름에 포함합니다.

```java
boolean isQuantityInRange(int quantity);
boolean hasItems();
```

### Query

상태를 변경하지 않고 값을 반환하는 메서드는 조회 의도가 드러나는 동사를 사용합니다. 프로젝트에서 정한 단어의 의미를 일관되게 유지합니다.

```java
Cart findByUserId(Long userId);
Optional<CartItem> findItemByProductId(Long productId);
```

`find`, `get`, `fetch`를 단순한 동의어처럼 섞지 않습니다. 이름과 실제 조회 범위·실패 방식이 일치해야 합니다.

### Command

상태를 변경하거나 외부 효과를 발생시키는 메서드는 동작과 대상을 이름에 넣습니다.

```java
void applyQuantity(int quantity);
void replaceQuantity(int quantity);
void markAsPaid();
```

`handle`, `process`, `execute`, `doSomething`처럼 책임이 드러나지 않는 이름은 구체적인 도메인 동작으로 바꿉니다. 단, 프레임워크 계약이나 UseCase의 공개 진입점처럼 이미 프로젝트에서 의미가 정해진 이름은 예외입니다.

### Class와 값

- 클래스·객체는 명사 또는 명사구를 사용합니다: `Cart`, `CartItem`, `AddressParser`
- 메서드는 동사 또는 동사구를 사용합니다.
- 변수·상수는 역할과 단위를 드러냅니다: `elapsedTimeInDays`, `MAX_ITEM_COUNT`
- 발음하기 어렵거나 검색하기 어려운 약어·한 글자 이름·`data`, `info`, `temp`를 피합니다. 반복문 카운터처럼 범위가 명확한 짧은 변수는 예외입니다.
- `id`, `api`, `url`, `http`, `sql`, `jpa`, `dto`처럼 문맥상 널리 이해되는 약어는 허용하되, 팀 용어와 충돌하면 프로젝트 용어를 우선합니다.
- 같은 추상 개념에는 하나의 단어를 선택하고 팀 코드 전체에서 재사용합니다.

## 이름 결정 트리

다음 순서로 공개 계약, 식별자 종류, 반환값, 부수효과, 실패 방식을 확인합니다.

```text
Is it an external, public, or framework contract?
├─ Yes → Preserve compatibility and follow the established contract convention
└─ No → What kind of identifier is it?
    ├─ Class or type → Use a noun or noun phrase in PascalCase
    │   └─ Add a role suffix only when it describes a real responsibility
    ├─ Constant → Use a meaningful UPPER_SNAKE_CASE name with its unit when needed
    ├─ Function or method → Does it mutate state or cause an external effect?
    │   ├─ Yes → Use a verb phrase with the action and target
    │   └─ No → Does it throw for a validation or precondition failure?
    │       ├─ Yes → Use require/assert/ensure with the validated subject
    │       └─ No → Does it return boolean?
    │           ├─ Yes → Use is/has/can/should/contains/matches/supports
    │           └─ No → Is it a transformation?
    │               ├─ Yes → Use to/from/as with the target type
    │               └─ No → Is it a creation or factory operation?
    │                   ├─ Yes → Use of/from/create with the source or result
    │                   └─ No → Is it a query?
    │                       ├─ Yes → Reflect result shape and failure behavior
    │                       └─ No → Use a verb phrase that states the actual role
    └─ Field, parameter, or local variable → Is it boolean?
        ├─ Yes → Use is/has/can/should when it reads naturally
        ├─ Collection → Use a plural noun
        └─ No → Use a descriptive camelCase noun with role, domain, and unit
```

Java 코드의 일반 변수·필드·매개변수는 `camelCase`를 우선합니다. `snake_case`는 DB 컬럼이나 외부 계약처럼 해당 표기 규칙이 실제로 요구될 때만 사용합니다.

### 결정 트리 세부 규칙

- 공개 API 필드·경로, JSON 이름, DB 컬럼, 프레임워크 Override는 스타일보다 호환성을 우선합니다. 새 이름을 정할 때만 이 트리를 적용합니다.
- 상태를 변경하지 않는다는 이유만으로 모든 메서드를 Query로 부르지 않습니다. `toCommand`, `fromResult`, `of`, `create`처럼 변환·생성 목적을 먼저 드러냅니다.
- Query 이름은 프로젝트의 조회 단어를 고정합니다. 예를 들어 `find`는 Optional 또는 조회 실패 가능성, `findAll`·`list`는 컬렉션, `count`는 개수, `exists`는 존재 여부를 나타내도록 일관되게 사용합니다.
- `require`, `assert`, `ensure`는 검증·사전조건 실패를 예외로 알리는 Guard에만 사용합니다. 예외를 던진다는 이유만으로 모든 Query를 이 이름으로 바꾸지 않습니다.
- boolean 이름은 의미를 구분합니다: `is`는 상태·조건, `has`는 보유·존재, `can`은 가능 여부, `should`는 정책 판단, `contains`는 구성원 포함, `matches`는 조건 일치, `supports`는 지원 가능 여부입니다.
- `isValid`, `isNot...`, `check...`, `process...`, `data`, `info`, `temp`처럼 대상·조건·역할이 흐려지는 이름은 구체화합니다. 부정형이 꼭 필요하면 이중 부정을 피하고 긍정 조건으로 바꿉니다.
- 값 이름에는 필요한 경우 단위를 포함합니다: `timeoutSeconds`, `itemCount`, `elapsedTimeInDays`, `createdAt`. 컬렉션은 복수형을 사용합니다.

## 함수 경계

- 함수는 한 가지 책임과 한 단계의 추상화만 다룹니다.
- 이름이 설명하는 것보다 더 많은 일을 하면 함수를 분리하거나 이름을 다시 정합니다.
- Query는 답변만 하고 Command는 동작만 하도록 하여 조회와 변경을 섞지 않습니다.
- Guard는 상태를 변경하지 않고 전제조건을 확인하며, 실패 시 예외를 던집니다. boolean을 반환하는 Predicate와 역할을 섞지 않습니다.
- 인자 수 제한을 기계적으로 적용하지 않습니다. 서로 강하게 묶인 값으로 구성된 Command나 좌표처럼 하나의 개념을 표현하는 인자는 그대로 둘 수 있습니다.
- 함수가 짧아지는 것만을 목적으로 의미 없는 위임 함수, Builder, 값 객체를 만들지 않습니다.
- 모든 `switch`를 다형성으로 바꾸거나 모든 `Manager`, `Processor`, `Util`을 금지하는 식으로 확장하지 않습니다. 실제 책임과 도메인 의미가 먼저입니다.

## 이름 선택 예시

| 목적 | 권장 | 피할 이름 |
| --- | --- | --- |
| 수량 범위 질문 | `isQuantityInRange` | `isValidQuantity`, `validateQuantityRange` |
| 수량 변경 | `applyQuantity`, `replaceQuantity` | `handleQuantity`, `processQuantity` |
| 항목 존재 질문 | `hasItems` | `checkItems` |
| 사용자별 조회 | `findByUserId` | `getData`, `loadStuff` |
| 변환 | `toCommand`, `fromResult` | `convert`, `mapData` |

`validateQuantityRange`처럼 boolean을 반환하는데 명령형 이름을 쓰거나, `isValidQuantity`처럼 무엇이 유효한지 숨기는 이름은 피합니다. 검증 실패 시 예외를 던지는 별도 메서드가 필요하다면 그 메서드의 예외 동작을 이름에 반영하고, predicate와 역할을 섞지 않습니다.

## 분석 보고서

파일·디렉터리 분석을 요청받으면 다음 형식으로 결과를 정리합니다.

```markdown
## Summary
- 분석 범위: `src/main/java/...`
- 분석 식별자: 42개
- 발견 사항: 3개 (Critical 1, Major 1, Minor 1)

## Findings
- [Critical] `path/File.java:20` — 현재 이름: `getUser`
  - 문제: 이름은 조회처럼 보이지만 내부 상태를 변경합니다.
  - 제안: `refreshUser`
  - 이유: 실제 부수효과를 이름에 드러냅니다.

## Suggested Renames
- `isValidQuantity` → `isQuantityInRange`
```

분석·리뷰 요청에는 이 형식을 사용하고, 단순한 이름 변경 요청에는 변경 대상·이유·검증 결과만 간결하게 보고합니다.

## 안전한 이름 변경

- 이름을 바꾸기 전에 `rg` 또는 IDE의 참조 검색으로 호출부·테스트·문서·설정 참조를 확인합니다.
- 동작 변경 없이 이름만 바꾸고, 공개 API·DB 컬럼·직렬화 계약은 별도 영향 검토 없이 변경하지 않습니다.
- 이름 후보를 제안할 때는 현재 이름, 실제 동작, 추천 이름, 선택 이유를 함께 제시합니다.
- 일괄 `fix-all`이나 광범위한 자동 리팩터링은 수행하지 않습니다. 사용자가 범위와 실행을 명시한 경우에만 해당 범위로 제한합니다.

## 적용 범위

이 스킬은 이름 선택·변경과 함수 경계 리뷰에만 적용합니다. API 계약, DB 스키마, 아키텍처, 외부 시스템 동작은 이름 변경에 필요한 최소 범위를 넘겨 임의로 바꾸지 않습니다. 사용자가 이름 변경만 요청한 경우 커밋·푸시·PR 생성은 하지 않습니다.

이 원칙은 의도 드러내는 이름, 검색 가능한 이름, 클래스는 명사·메서드는 동사, 개념별 단어 통일, 단일 책임, Command/Query 분리를 프로젝트 상황에 맞게 적용한 것입니다. 줄 수·인자 수·다형성 전환은 절대 기준이 아닙니다.
