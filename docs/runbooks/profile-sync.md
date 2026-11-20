# AI 취향 프로필 재처리 확인

온보딩 취향 저장과 AI 프로필 반영은 별도 단계입니다. BE는 회원별 최신 동기화 작업을 `profile_sync_jobs`에 보관합니다. 새 작업이 들어오면 이전 revision을 대체하고, 작업자가 처리를 마치면 해당 revision을 삭제합니다. Kafka 전송 경로에서는 브로커 발행 확인과 AI 소비자의 프로필 반영 완료 시점이 다릅니다. [작업 저장소](../../src/main/java/com/book/core/onboarding/infrastructure/persistence/repository/ProfileSyncJobRepositoryAdapter.java)

## 남은 작업 확인

승인된 읽기 전용 세션에서 아래 집계를 확인합니다. `available_at` 연도가 9999인 행은 자동 재시도를 멈추고 보관한 작업입니다. `attempts`는 프로필 처리 실패 시 증가합니다. Kafka 발행 실패는 `[PROFILE_SYNC]` 발행 실패 로그로 확인합니다.

```sql
SELECT COUNT(*) AS remaining_jobs,
       COUNT(CASE WHEN YEAR(available_at) = 9999 THEN 1 END) AS stopped_jobs,
       COUNT(CASE WHEN YEAR(available_at) < 9999 AND attempts > 0 THEN 1 END) AS retry_jobs,
       COUNT(CASE WHEN kafka_publish_at IS NOT NULL THEN 1 END) AS waiting_for_be_relay
FROM profile_sync_jobs;
```

`waiting_for_be_relay`는 BE 내부 토픽 `book.profile-sync.v1`으로 발행할 작업과 발행을 시도했으나 BE 리스너가 아직 수신 처리하지 않은 작업을 셉니다. BE 리스너가 현재 revision을 수신하면 `markReady`가 `kafka_publish_at`을 `NULL`로 바꿉니다. 이때부터 작업자가 프로필 갱신이나 삭제를 처리할 수 있으며, AI 직접 Kafka 경로를 사용하는 경우 `book.ai-profile.v1`으로 따로 발행합니다. AI 프로필 반영은 AI 측에서 실제 저장 또는 삭제가 끝난 기록으로 확인해야 합니다. 집계 항목은 서로 겹칠 수 있습니다. [BE Kafka 전달](../../src/main/java/com/book/core/onboarding/infrastructure/kafka/ProfileSyncKafkaRelay.java), [Kafka 전송](../../src/main/java/com/book/core/onboarding/infrastructure/client/ai/AiPersonalizationProfileKafkaClient.java)

프로필 갱신 실패가 계속되면 재시도 간격은 30초부터 늘어나 최대 15분이 됩니다. 자동 재시도를 멈춘 원인은 `[PROFILE_SYNC]` 실패 로그의 `revision`과 `errorCode`에서 확인합니다. [재처리 정책](../../src/main/java/com/book/core/onboarding/application/usecase/ProcessProfileSyncJobUseCase.java)

## 재처리 지표 수집에 필요한 기록

현재 BE에서 재현할 수 있는 수치는 위 쿼리로 확인하는 조회 시점의 잔여 작업 수입니다. 완료된 행은 삭제되고 새 작업은 이전 revision을 덮어씁니다. `available_at`은 다음 처리 예정 시각이며, 일반 재시도 로그에는 revision이 없습니다. 따라서 이 자료만으로는 이슈의 재처리 성공률, 최종 미동기화 건수, 복구 시간을 사후에 계산할 수 없습니다.

세 지표를 운영에서 측정하려면 아래 기록을 먼저 확보합니다.

- BE 작업 이력에 `userId`, `revision`, 요청 종류(갱신 또는 삭제), 등록·실패 시각, 대체된 경우 다음 revision과 대체 시각을 남깁니다. 동일 revision의 여러 실패 중 최초 실패 시각을 찾을 수 있어야 합니다.
- AI가 실제 저장이나 삭제를 끝낸 결과에 같은 `userId`와 `revision`, 처리 결과와 반영 시각을 남깁니다. 이 키로 양쪽 이력을 연결하고, 관측 종료 시점에는 BE 최신 요청과 AI 최종 반영 상태를 비교합니다.
- 관측 기간과 재처리를 기다리는 기간을 정한 뒤, 양쪽 이력의 보존 기간에 두 기간을 모두 포함합니다. 관측 중 대체된 revision은 재처리 성공률에서 별도 집계합니다. 실제 기록 위치와 보존 기간은 양쪽 서비스에서 확인한 뒤 측정 절차에 문서화합니다.

장애 복구 테스트는 격리 환경의 재처리 동작을 확인합니다. [AI 장애 복구 테스트](../../src/test/java/com/book/core/onboarding/api/OnboardingProfileRefreshIntegrationTest.java), [Kafka 장애 복구 테스트](../../src/test/java/com/book/core/onboarding/infrastructure/kafka/ProfileSyncKafkaIntegrationTest.java)
