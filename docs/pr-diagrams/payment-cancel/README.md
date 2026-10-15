# 결제·취소 기능 PR 시퀀스 다이어그램

기능별 PR의 정상 처리 순서와 책임 경계를 Archify로 작성했습니다. 코드 PR과 분리한 자료 브랜치이며, PNG 이미지는 PR 본문에서 이 커밋을 고정해 참조합니다.

| Issue | 기능 | 브랜치 | 다이어그램 |
|---|---|---|---|
| #302 | 포인트 수동 기본 입력 검증 정리 | `feat/302-point-input-contract` | [input](input.monochrome.png) |
| #303 | 결제와 결제 이력 저장 기반 추가 | `feat/303-payment-foundation` | [history](history.monochrome.png) |
| #304 | 일반 주문 결제 생성과 할인 계산 추가 | `feat/304-payment-create` | [create](create.monochrome.png) |
| #305 | 취소 가능 잔액 관리 기반 추가 | `feat/305-cancel-balance` | [balance](balance.monochrome.png) |
| #306 | 결제 성공 처리와 장바구니 후처리 추가 | `feat/306-payment-success` | [success-current](success-current.monochrome.png) |
| #307 | 결제 실패 이력 기록 추가 | `feat/307-payment-failure` | [failure](failure.monochrome.png) |
| #308 | 결제 전체 취소 기능 추가 | `feat/308-payment-full-cancel` | [full-cancel](full-cancel.monochrome.png) |
| #309 | 주문상품 부분 취소 기능 추가 | `feat/309-payment-partial-cancel` | [partial-cancel](partial-cancel.monochrome.png) |
| #239 | 포인트 잔액 낙관적 락 | `feat/239-point-balance-version` | [point-version](point-version.monochrome.png) |

## 표현 범위

- 그림은 각 기능 브랜치 시점의 정상 흐름입니다. 반복되는 Domain/Port/저장소 호출은 읽기 쉽게 묶었습니다. 화살표 하나를 SQL 한 번이나 별도 트랜잭션으로 해석하지 않습니다.
- HTTP 검증·소유권·업무 상태 검사와 오류 응답, 미구현 PG 승인·취소, `usePoint=-1`, 부분 취소의 엄격한 쿠폰 경계는 해당 PR 본문과 코드를 함께 확인합니다.
- 성공 처리의 상태 전이와 혜택·이력 반영은 하나의 UseCase 트랜잭션입니다. 장바구니 후처리만 비동기이며 기존 오류 로그를 보존합니다. 비동기 새 로그는 paymentId로 연결하고, 동기 로그는 traceId도 기록합니다.
- 포인트 버전 그림은 기존 #300의 4개 변경 파일과 결제·취소 선행 스택을 기준으로 합니다. 최종 병합 커밋 SHA는 PR의 head를 기준으로 확인합니다.

## 검증 근거

- 9개 명세 모두 showcase 9/9, 오류·경고 0건입니다. 각 `*.validation.json`과 `*.delivery.json`에 결과와 명세·HTML SHA-256을 보관했습니다.
- 전달된 HTML 그대로 1440×900, 1600×1000, 1920×1080, 2048×1320의 배치·읽기 크기·넘침을 검사했습니다. 밝은/어두운 테마 캡처와 `*.visual-check.html` contact sheet를 함께 보관했습니다.
- 브라우저 자동 결과의 `visualReview: pending`은 자동 검사가 시각 검토를 대신하지 않는다는 뜻이며 원본 값을 유지했습니다. 이와 별개로 PR에 사용하는 흰 배경·검은 선 PNG 9개를 이미지로 열어 글자·선·잘림을 확인했습니다.
- `*.native.svg`는 Archify 내장 SVG 내보내기 결과입니다. `*.monochrome.svg`는 위치·연결·글자를 유지하고 색상만 바꾼 파생물이며, PNG는 이 SVG를 2배 해상도로 내보냈습니다. `*.export.json`의 도형 보존·불투명 회색조·크기·해시 검사와 이미지 시각 확인은 서로 다른 근거입니다.
- 한국어 본문을 작성했습니다. Archify Viewer의 고정 메뉴·기본 범례와 HTML lang fallback은 도구 기본 영어를 유지합니다.
- 명세·HTML은 최종 검증 후 변경하지 않았습니다. 공개 검증 JSON의 임시 절대 경로만 같은 폴더 상대 경로로 정규화했습니다.

애플리케이션 테스트와 실제 PG·운영 DB 검증 결과는 이 다이어그램 검증에 포함되지 않습니다.

Archify가 생성한 HTML·SVG의 공백도 해시 일치를 위해 그대로 보관합니다. Git 공백 검사는 직접 작성한 Markdown·JSON에 적용했습니다.
