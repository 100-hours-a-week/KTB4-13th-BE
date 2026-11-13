# AI 취향 프로필 재처리 확인

온보딩 취향 저장과 AI 프로필 반영은 별도 단계입니다. BE는 회원별 최신 동기화 작업을 `profile_sync_jobs`에 보관합니다. 새 작업이 들어오면 이전 revision을 대체하고, 작업자가 처리를 마치면 해당 revision을 삭제합니다. Kafka 전송 경로에서는 브로커 발행 확인과 AI 소비자의 프로필 반영 완료 시점이 다릅니다. [작업 저장소](../../src/main/java/com/book/core/onboarding/infrastructure/persistence/repository/ProfileSyncJobRepositoryAdapter.java), [Kafka 전송](../../src/main/java/com/book/core/onboarding/infrastructure/client/ai/AiPersonalizationProfileKafkaClient.java)

## 남은 작업 확인

승인된 읽기 전용 세션에서 아래 집계를 확인합니다. `available_at` 연도가 9999인 행은 자동 재시도를 멈추고 보관한 작업입니다. `attempts`는 프로필 처리 실패 시 증가합니다. Kafka 발행 실패는 `[PROFILE_SYNC]` 발행 실패 로그로 확인합니다.

```sql
SELECT COUNT(*) AS remaining_jobs,
       COUNT(CASE WHEN YEAR(available_at) = 9999 THEN 1 END) AS stopped_jobs,
       COUNT(CASE WHEN YEAR(available_at) < 9999 AND attempts > 0 THEN 1 END) AS retry_jobs,
       COUNT(CASE WHEN kafka_publish_at IS NOT NULL THEN 1 END) AS waiting_for_kafka
FROM profile_sync_jobs;
```

`waiting_for_kafka`에는 발행 대기뿐 아니라 발행을 시도했지만 아직 작업자에게 전달되지 않은 revision도 포함됩니다. 집계 항목은 서로 겹칠 수 있습니다. 프로필 갱신 실패가 계속되면 재시도 간격은 30초부터 늘어나 최대 15분이 됩니다. 자동 재시도를 멈춘 원인은 `[PROFILE_SYNC]` 실패 로그의 `revision`과 `errorCode`에서 확인합니다. [재처리 정책](../../src/main/java/com/book/core/onboarding/application/usecase/ProcessProfileSyncJobUseCase.java), [Kafka 전달](../../src/main/java/com/book/core/onboarding/infrastructure/kafka/ProfileSyncKafkaRelay.java)

## 재처리 결과 측정

- **재처리 성공률**은 같은 관측 기간에 실패한 revision 중 AI에서 반영을 확인한 비율입니다. 새 revision으로 대체된 작업은 따로 집계합니다.
- **최종 미동기화 건수**는 관측 종료 시점에 BE의 최신 취향 또는 삭제 요청과 AI의 최종 상태가 일치하지 않는 회원 수입니다.
- **복구 시간**은 동일 revision의 최초 실패 관측 시각부터 AI 반영 확인 시각까지입니다.

측정에는 실패 시점과 revision, AI 측 반영 확인 기록이 필요합니다. 위 BE 작업 집계는 조사 시작점이며 세 지표의 실측값은 제공하지 않습니다. [AI 장애 복구 테스트](../../src/test/java/com/book/core/onboarding/api/OnboardingProfileRefreshIntegrationTest.java), [Kafka 장애 복구 테스트](../../src/test/java/com/book/core/onboarding/infrastructure/kafka/ProfileSyncKafkaIntegrationTest.java)
