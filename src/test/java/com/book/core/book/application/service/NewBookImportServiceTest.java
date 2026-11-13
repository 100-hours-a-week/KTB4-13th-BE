package com.book.core.book.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.book.core.book.application.port.NewBookImportLock;
import com.book.core.book.application.port.NewBookImportNotifier;
import com.book.core.book.application.result.BookDetail;
import com.book.core.book.application.result.NewBookDetailFound;
import com.book.core.book.application.result.NewBookImportOutcome;
import com.book.core.book.application.result.NewBookImportReport;
import com.book.core.book.application.usecase.CollectNewBookIsbnsUseCase;
import com.book.core.book.application.usecase.ImportNewBookDetailUseCase;
import com.book.core.book.application.usecase.RegisterNewBookUseCase;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

class NewBookImportServiceTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 13);
    private static final String ISBN = "9788983921987";
    private static final String OTHER_ISBN = "9788972756194";

    @ParameterizedTest
    @CsvSource({"false, false", "true, false", "false, true", "true, true"})
    void 배치_로그와_알림을_연결하고_알림_실패는_배치_결과를_바꾸지_않는다(final boolean batchFails, final boolean notificationFails) {
        final var collection = mock(CollectNewBookIsbnsUseCase.class);
        final var details = mock(ImportNewBookDetailUseCase.class);
        final var registration = mock(RegisterNewBookUseCase.class);
        final var notifier = mock(NewBookImportNotifier.class);
        final var events = new ArrayList<String>();
        final NewBookImportLock lock = (final var batch) -> {
            try {
                batch.run();
                return true;
            } finally {
                events.add("unlock");
            }
        };

        final var detail = new BookDetail("신간", "저자", null, "출판사", "800", TODAY.minusDays(1), null);
        final var batchFailure = new IllegalStateException("외부 응답 원문 예시");
        when(collection.collect(TODAY)).thenReturn(List.of(ISBN, OTHER_ISBN));
        when(details.findDetail(ISBN)).thenReturn(new NewBookDetailFound(detail));
        when(registration.register(ISBN, detail)).thenReturn(true);
        if (batchFails) {
            when(details.findDetail(OTHER_ISBN)).thenReturn(new NewBookDetailFound(detail));
            when(registration.register(OTHER_ISBN, detail)).thenThrow(batchFailure);
        } else {
            when(details.findDetail(OTHER_ISBN)).thenReturn(NewBookImportOutcome.MISSING_DETAIL);
        }
        final var reportCaptor = ArgumentCaptor.forClass(NewBookImportReport.class);
        doAnswer((final var invocation) -> {
            events.add("send");
            if (notificationFails) {
                throw new IllegalStateException("웹훅 URL 예시");
            }
            return null;
        }).when(notifier).send(reportCaptor.capture());

        final var service = new NewBookImportService(lock, collection, details, registration, notifier);
        final var logger = (Logger) LoggerFactory.getLogger(NewBookImportService.class);
        final var appender = new ListAppender<ILoggingEvent>();
        appender.start();
        logger.addAppender(appender);
        try {
            if (batchFails) {
                assertThatThrownBy(() -> service.importNewBooks(TODAY)).isSameAs(batchFailure);
            } else {
                service.importNewBooks(TODAY);
            }
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }

        final var report = reportCaptor.getValue();
        verify(notifier).send(report);
        verifyNoMoreInteractions(notifier);
        assertThat(events).containsExactly("unlock", "send");
        assertThat(MDC.get("runId")).isNull();
        assertThat(report.runId().version()).isEqualTo(4);
        assertThat(report.failed()).isEqualTo(batchFails);
        assertThat(report.elapsedMs()).isNotNegative();
        assertThat(report.candidateCount()).isEqualTo(2);
        assertThat(report.detailCallCount()).isEqualTo(2);
        assertThat(report.registeredCount()).isEqualTo(1);

        final var messages = appender.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
        assertThat(messages).anySatisfy((final var message) -> assertThat(message).contains("시작합니다.", "runId=" + report.runId()));
        assertThat(messages).anySatisfy((final var message) -> assertThat(message).contains("배치가 " + report.status(),
            "runId=" + report.runId(), "detailCallCount=2", "REGISTERED=1", "elapsedMs=" + report.elapsedMs()));
        if (batchFails) {
            assertThat(report.failureReason()).isEqualTo("IllegalStateException");
        }
        if (notificationFails) {
            assertThat(messages).anySatisfy((final var message) -> assertThat(message).contains("알림 전송에 실패", "runId=" + report.runId()));
        }
        assertThat(messages).noneSatisfy((final var message) -> assertThat(message).containsAnyOf("외부 응답 원문 예시", "웹훅 URL 예시"));
    }
}
