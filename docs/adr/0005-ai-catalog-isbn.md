# ADR-0005: AI 연동의 도서 식별자와 요청 크기

## 상태

채택

## 맥락

AI 검색과 추천은 ISBN-13으로 도서를 식별한다. 주문, 리뷰, 나의 도서관, 상품은 BE의 도서 ID에 연결된다. 한 ISBN이 여러 도서에 연결되면 AI 결과를 다른 상품으로 연결하거나 사용자의 이력을 잘못 전달할 수 있다.

## 결정

- AI에 연결할 수 있는 ISBN은 13자리 숫자이며 도서 ID 하나에만 대응해야 한다. 비어 있거나 형식이 틀리거나 중복된 ISBN은 AI 검색·추천과 프로필 이력에서 제외한다.
- 기존 도서와 상품 ID를 보존한다. 일반 판매와 기존 기록은 유지하고, 원본으로 ISBN을 확인한 뒤 같은 도서에 값을 보정한다. 중복 행은 원본을 확인할 때까지 모두 AI 연동에서 제외한다.
- BE에 저장된 개인화 동의를 확인해 구매·도서관 이력을 싣는다. 미동의자는 빈 목록으로 보낸다.
- 피드는 인코딩한 경로와 쿼리의 크기, 챗봇은 직렬화한 JSON 본문의 크기로 제한한다. ISBN은 한 번만 보내고 공간이 부족할 때는 최근 이력을 우선한다. 채팅의 대화 제외 목록은 최근 대화 이력으로 우선한다.
- 챗봇의 본문 한도는 64 KiB다. 피드는 페이지마다 커서에 쓸 공간을 확보해 ISBN 예산을 일정하게 유지한다. URL 한도는 `AI_FEED_REQUEST_TARGET_MAX_BYTES`로 설정하며 기본값은 4,096바이트다. 배포 전에 BE에서 AI까지 실제 경로의 한도를 확인하고 그 안에서 값을 설정한다. AI와 프록시의 운영 로그에서 구매·도서관 쿼리 값이 가려지는지도 확인한다.
- 쇼핑 에이전트가 전체 장바구니를 ISBN으로 표현할 수 없으면 해당 대화 턴은 추천 전용으로 처리한다. 일반 장바구니와 구매 경로는 기존 상품 ID를 사용한다.

## ISBN 정리 절차

먼저 운영 카탈로그의 이상 행을 조회한다.

```sql
SELECT id, isbn13
FROM books
WHERE isbn13 IS NULL OR NOT REGEXP_LIKE(isbn13, '^[0-9]{13}$', 'c');

SELECT isbn13, COUNT(*) AS book_count
FROM books
WHERE isbn13 IS NOT NULL
GROUP BY isbn13
HAVING COUNT(*) > 1;
```

기존 ID로 원본 카탈로그와 대조해 확인된 값만 갱신한다. ISBN을 복구할 때는 AI 카탈로그가 같은 도서를 가리키는지 확인한 뒤 BE의 격리를 해제한다. ISBN 변경만으로는 기존 사용자 이력의 수정 시각이 바뀌지 않으므로, 복구한 책의 영향을 받는 사용자 프로필을 다시 갱신한다. 조회 결과가 모두 비고 새 적재 경로도 같은 규칙을 지키면 후속 Flyway 마이그레이션에서 `NOT NULL`, 13자리 검사, 전체 ISBN에 대한 `UNIQUE`를 적용한다. 적용 전 기존 ISBN 인덱스를 확인한다.

## 관련 문서

- [BE #252](https://github.com/100-hours-a-week/KTB4-13th-BE/issues/252)
- [AI 피드 쿼리 로그 마스킹](https://github.com/100-hours-a-week/KTB4-13th-AI/issues/333)
- [AI 모델 API 명세](https://github.com/100-hours-a-week/KTB4-13th-wiki/blob/main/docs/ai/1-model-api/spec.md)
