# ADR-0006 — 요청 컨텍스트에 ScopedValue를 도입하지 않는다

## 상태

채택

## 현재 요청 경계

- **동기 Servlet 요청.** 보안 필터 앞에서 실행되는 `RequestMdcFilter`는 헤더에서 받거나 새로 만든 요청 ID와 HTTP 메서드·URI를 MDC에 넣고 `finally`에서 제거한다. 응답 헤더, 오류 본문의 `traceId`, 요청 로그와 일부 AI 호출이 같은 요청 ID를 사용한다. [필터 등록](../../src/main/java/com/book/common/filter/ServletFilterConfig.java), [MDC 필터](../../src/main/java/com/book/common/filter/RequestMdcFilter.java), [오류 응답](../../src/main/java/com/book/common/response/ErrorResponse.java), [AI 검색 클라이언트](../../src/main/java/com/book/core/search/infrastructure/client/ai/BookSearchClientImpl.java)
- **채팅 SSE.** Controller는 Servlet 요청 스레드에서 사용자 ID를 명령에 담고 MDC를 복사한다. 별도 executor의 작업 스레드는 오류 응답을 만들 때까지 복사한 MDC를 사용하고 `finally`에서 비운다. 이벤트를 쓰는 별도 스레드는 이미 만들어진 응답을 전송한다. 필터에는 비동기 재디스패치에서 컨텍스트를 다시 설정하는 로직이 없다. [채팅 Controller](../../src/main/java/com/book/core/agentchat/api/ChatSessionController.java), [SSE executor](../../src/main/java/com/book/core/agentchat/api/ChatSseExecutor.java)
- **인증과 백그라운드 작업.** `UserIdArgumentResolver`는 Spring Security의 JWT subject를 읽어 Controller에 전달한다. 비동기 결제 후처리와 알림 저장은 필요한 식별자를 파라미터나 명령으로 받는다. 스케줄러가 시작한 작업에는 원래 HTTP 요청이 없다. [사용자 ID resolver](../../src/main/java/com/book/common/config/security/UserIdArgumentResolver.java), [결제 후처리](../../src/main/java/com/book/core/payment/application/usecase/PostProcessPaymentUseCase.java), [알림 저장](../../src/main/java/com/book/core/notification/application/service/NotificationService.java), [메모리 추출 스케줄러](../../src/main/java/com/book/core/preference/infrastructure/scheduler/UserMemoryExtractionScheduler.java)

## 결정

동기 호출에서 `ScopedValue`를 쓰면 정상 종료와 예외에서 바인딩이 자동 해제된다. 하지만 로그와 오류 응답은 여전히 MDC를 읽으므로 요청 ID를 두 곳에 저장하거나 모든 소비자를 바꿔야 한다. 일반 executor 작업과 별도 Servlet 디스패치에는 바인딩이 자동 전파되지 않아 SSE에는 계속 명시적인 전달이 필요하다. `StructuredTaskScope`의 전파는 스코프 안에서 시작하고 종료하는 자식 작업을 대상으로 한다. [Java 25 ScopedValue 문서](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/lang/ScopedValue.html)

요청 로그는 MDC, 사용자 식별은 `@UserId`와 명시적인 명령·파라미터를 유지한다. 같은 호출 범위에서 여러 계층이 공유해야 하는 새 읽기 전용 값이 생기면 실제 소비자와 스레드 경계를 확인해 `ScopedValue`를 다시 검토한다.

## 검증 근거

- 요청 ID 생성·전달, 예외 시 MDC 정리 및 기존 MDC 항목 보존을 검증한다. [RequestMdcFilterTest](../../src/test/java/com/book/common/logging/RequestMdcFilterTest.java)
- 인증 및 선택적 사용자 ID 해석과, SSE 오류 이벤트의 `traceId`가 요청 헤더와 일치하는지 검증한다. [UserIdArgumentResolverTest](../../src/test/java/com/book/common/config/security/UserIdArgumentResolverTest.java), [ChatSessionHttpIntegrationTest](../../src/test/java/com/book/core/agentchat/api/ChatSessionHttpIntegrationTest.java)

[관련 이슈 #191](https://github.com/100-hours-a-week/KTB4-13th-BE/issues/191)
