package com.book.core.book.infrastructure.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.book.common.notification.NotificationSender;
import com.book.core.book.application.result.NewBookImportOutcome;
import com.book.core.book.application.result.NewBookImportReport;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

class NewBookImportMessageNotifierTest {
    @ParameterizedTest
    @NullSource
    @ValueSource(strings = "저장 오류")
    void 성공과_실패_메시지에_실행_ID와_시간과_건수를_담는다(final String failureReason) {
        final var sender = mock(NotificationSender.class);
        final var runId = UUID.randomUUID();
        final var report = new NewBookImportReport(runId, 1234L, 5, 3, Map.of(NewBookImportOutcome.REGISTERED, 2), failureReason);

        new NewBookImportMessageNotifier(sender).send(report);

        final var message = ArgumentCaptor.forClass(String.class);
        verify(sender).send(message.capture());
        assertThat(message.getValue()).contains("runId=" + runId, "1234 ms", "후보 ISBN: 5건", "상세 조회: 3건", "신규 등록: 2건", "REGISTERED=2");
        if (failureReason == null) {
            assertThat(message.getValue()).contains("종료됐습니다").doesNotContain("실패 사유");
        } else {
            assertThat(message.getValue()).contains("실패했습니다", "실패 사유: 저장 오류");
        }
    }
}
