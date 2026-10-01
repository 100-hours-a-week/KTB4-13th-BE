# 온보딩 후보 도서 등록

`seed-book-candidates.sql`은 제공받은 CSV의 49개 세부 카테고리와 후보 490권을 등록합니다. Flyway V23 이후에 **별도로 실행하는 운영용 SQL**이며 애플리케이션 시작 시 실행하지 않습니다.

- `local_book_id`를 `books.id`로 사용합니다. 490개 ID와 제목이 모두 일치하고 code가 Q4 선택지에 존재할 때만 한 번의 INSERT로 후보를 등록합니다.
- ID나 제목이 하나라도 다르면 불일치 목록을 출력하고 후보를 한 건도 추가/수정하지 않습니다. 임의의 도서를 생성하거나 제목만으로 다른 ID에 연결하지 않습니다.
- 등록 후 `matched_candidates`가 490인지 확인합니다. 불일치 목록이 비어 있고 이 값이 490이어야 등록 완료입니다. 기존 데이터가 있으면 count만으로 이번 실행 성공을 판단하지 않습니다.
- 재실행하면 동일 카테고리/도서의 순위만 갱신합니다. 다른 매핑이나 기존 NULL 카테고리 후보는 삭제하지 않습니다.
- 같은 도서를 다른 카테고리에 매핑할 수 있지만 조회 응답에는 한 번만 포함됩니다.
- CSV의 `has_cover=N` 75권도 후보에 포함합니다. 표지가 없으면 기존 nullable 응답 계약을 따릅니다.

조회 예시:

```http
GET /api/v1/onboarding/books?subcategoryCodes=novel-sf,science-space
Authorization: Bearer {accessToken}
```

`subcategoryCodes`는 필수이며 중복 제거 후 1~9개를 받습니다. 잘못된 code는 400, 후보가 없는 유효 카테고리는 빈 `data.candidates`를 반환합니다. 요청 카테고리 순서, 각 후보의 rank, bookId 순으로 정렬하며 개수 제한은 없습니다. 기존 클라이언트는 이 쿼리 파라미터를 추가해야 합니다.

이 저장소 작업은 운영 DB에 등록 SQL을 실행하거나 AI 서버의 임베딩 존재 여부를 검증하지 않습니다. 실제 DB 등록은 대상 카탈로그 검증과 별도 운영 절차를 거쳐 수행합니다.
