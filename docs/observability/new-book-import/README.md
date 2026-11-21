# 신간 등록 배치 관측 대시보드

신간 등록 배치가 여러 인스턴스에서 같은 시각에 실행돼도 한 곳에서만 돌고, 외부 API 호출 한도 안에서 어떻게 처리되는지 Prometheus, Loki, Grafana(PLG)로 보여 줍니다.
## 무엇을 보여 주나

| 패널 | 보여 주는 것 | 데이터 |
|---|---|---|
| 배치 실행 결과 | 실행 완료, 락 때문에 건너뜀, 실패 횟수 | `book_new_import_runs_total{result}`, `scheduler_lock_attempts_total{result="not_acquired"}` |
| 배치 실행 시간 (최대) | 한 번 실행에 걸린 최대 시간 | `book_new_import_duration_seconds_max` |
| 일일 상세 호출 한도 사용률 | 오늘 쓴 정보나루 상세 호출 수 ÷ 하루 한도 | `book_new_import_detail_calls_total`, `book_new_import_detail_daily_limit` |
| 락 경합 | 인스턴스별로 락을 잡았는지, 못 잡고 건너뛰었는지 | `scheduler_lock_attempts_total{lock_name="new-book-import", instance_name, result}` |
| 단계별 처리 건수 | 후보 → 상세 조회 → 결과(상세 없음, 한도 소진, 등록 등) | `book_new_import_candidates_total`, `book_new_import_detail_calls_total`, `book_new_import_outcomes_total{outcome}` |
| 외부 API 오류 코드 분포 | 응답 본문에 담긴 오류 코드(`isbnMpngErr`, `authErr` 등) | `book_new_import_external_errors_total{provider, code}` |
| 외부 API 평균 응답 시간 | 국중도, 정보나루 응답 시간 | `http_client_requests_seconds` (Spring RestClient 관측) |
| 외부 API 요청 수 | 공급처별 HTTP 상태 | `http_client_requests_seconds_count{client_name, status}` |
| 배치 로그 | `[BOOK]`, `[SCHEDULER]` 로그를 인스턴스별로 | Loki `{job="book"}` |

## 구성

```text
instance-a (:18081) ─┐  /actuator/prometheus   ┌─ Prometheus (:19090) ─┐
instance-b (:18082) ─┘ ────────────────────────┘                       ├─ Grafana (:13000)
        │ JSON 로그 파일(logs/*.log) ── Alloy ── Loki (:13100) ───────────┘
        └ MySQL (:13306, shedlock·호출 한도·도서), Redis (:16379)
```

- 앱 쪽에는 의존성을 추가하지 않았습니다. 이미 있는 `micrometer-registry-prometheus`와 JSON 로그(logstash 형식)를 씁니다.
- 신간 배치 지표는 `infrastructure/observability/NewBookImportMetrics`가 기록합니다. 배치 결과는 알림 Adapter가, 본문 오류 코드는 `NewBookCollectorAdapter`가 기록합니다.
- 락은 ShedLock `@SchedulerLock`으로 스케줄러 메서드에서 잡습니다. 락을 잡았는지, 건너뛰었는지는 `common/config/scheduler/SchedulerLockListener`가 `scheduler_lock_attempts_total`로 기록하고 `[SCHEDULER]` 로그를 남깁니다.
- 카운터는 앱이 뜰 때 0으로 미리 등록합니다. 처음 증가할 때 생기는 카운터는 Prometheus `increase()`가 첫 값을 놓치기 때문입니다.

## 실행 방법

1. 관측 스택과 MySQL, Redis를 띄웁니다.

   ```bash
   cd docs/observability/new-book-import
   docker compose up -d
   ```

2. 앱 JAR를 만듭니다.

   ```bash
   ./gradlew bootJar
   ```

3. API 키를 환경 변수로 넣고 두 인스턴스를 띄웁니다. 키는 명령줄 인자가 아니라 환경 변수로만 넘깁니다.

   ```bash
   export NATIONAL_LIBRARY_ISBN_API_KEY=<국중도 키>
   export DATA_FOR_LIBRARY_API_KEY=<정보나루 키>
   docs/observability/new-book-import/run-instances.sh
   ```

   - 기본값은 2분마다 실행(`NEW_BOOK_CRON`), 정보나루 상세 하루 한도 10건(`DATA_FOR_LIBRARY_DAILY_LIMIT`)입니다.
   - 빈 DB의 V66 마이그레이션이 운영 도서 데이터를 요구해서, 테스트용 시드 마이그레이션(`src/test/resources/db/migration`)을 함께 적용합니다.
   - 로그는 `logs/instance-a.log`, `logs/instance-b.log`에 쌓이고 Git에는 올라가지 않습니다.

4. Grafana에서 확인합니다: <http://localhost:13000/d/new-book-import>

5. 확인이 끝나면 인스턴스와 스택을 내립니다. 인스턴스를 켜 두면 실행마다 국중도 목록 API를 약 150번 호출합니다.

   ```bash
   kill $(pgrep -f 'spring.application.name=instance-')
   docker compose down -v
   ```

## 확인 결과 (2026-11-21, 로컬)

`@SchedulerLock` 구현으로 두 인스턴스를 띄워 2분 간격으로 네 번 실행한 구간(02:53:00~03:01:30)입니다. 빈 DB에서 시작했습니다.

- **락:** 네 번 모두 같은 초(02:54, 02:56, 02:58, 03:00)에 두 인스턴스가 시작했고, instance-b만 실행했습니다. instance-a는 네 번 모두 건너뛰었습니다(`scheduler_lock_attempts_total`: instance-b `acquired` 4, instance-a `not_acquired` 4).
- **호출 한도:** 1회차가 정보나루 상세 10건을 써서 한도를 모두 썼고(사용률 100%), 2~4회차는 상세 조회 없이 한도 소진으로 끝났습니다.
- **결과:** 상세 10건 모두 상세 없음이었습니다. 그중 7건은 정보나루에 없는 ISBN(`isbnMpngErr`)이었고, 나머지 3건은 응답은 왔지만 등록에 필요한 값(분류번호 등)이 없었습니다.
- **실행 시간:** 국중도 목록 160페이지를 받는 데 대부분이 걸려 한 번에 26~35초였습니다.
- **오류:** 두 인스턴스 로그에 ERROR가 없었고, 실패한 실행도 없었습니다.
- **보안:** `/actuator/prometheus`와 로그 파일에서 두 API 키 값이 나오지 않는 것을 확인했습니다. 지표의 `uri` 태그에는 `{jeongboNaruKey}` 같은 자리 표시자만 남습니다.

## 읽을 때 주의할 점

- 건수 패널은 `increase()`로 계산해서 Prometheus 보간 때문에 실제 값과 조금 다를 수 있습니다. 예를 들어 이번 구간의 국중도 요청 수는 실제 640회인데 621로, 후보 수는 실제 63,772건인데 62,749로 보입니다. 정확한 값은 배치 로그의 `listCallCount`, `detailCallCount`, `outcomes`를 보면 됩니다.
- `http_client_requests_seconds`는 첫 요청 때 생기는 지표라 짧은 구간에서는 첫 몇 건이 빠질 수 있습니다(정보나루 실제 10회, 표시 9회).
- 배치 로그 패널의 시각은 Alloy가 로그 파일을 읽은 시각이라, 앱이 남긴 시각보다 몇 초 늦게 보일 수 있습니다(이번 구간은 약 6초).
- 로컬 확인용 구성입니다. 운영 Prometheus와 Grafana의 위치는 이 저장소에서 확인하지 못했습니다.
