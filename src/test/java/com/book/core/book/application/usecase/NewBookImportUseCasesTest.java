package com.book.core.book.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.book.core.book.application.port.NewBookCollectorException;
import com.book.core.book.application.port.NewBookCollectorFailureReason;
import com.book.core.book.application.port.NewBookDetailCallBudget;
import com.book.core.book.application.port.BookRepositoryPort;
import com.book.core.book.application.port.DuplicateImportedIsbnException;
import com.book.core.book.application.port.NewBookCollector;
import com.book.core.book.application.port.NewBookImportLock;
import com.book.core.book.application.port.NewBookImportNotifier;
import com.book.core.book.application.port.NewBookLookupBackoff;
import com.book.core.book.application.result.BookDetail;
import com.book.core.book.application.result.NewBookCandidate;
import com.book.core.book.application.result.NewBookDetailFound;
import com.book.core.book.application.result.NewBookImportOutcome;
import com.book.core.book.application.result.NewBookImportReport;
import com.book.core.book.application.service.NewBookImportService;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class NewBookImportUseCasesTest {
    private static final String ISBN = "9788983921987";
    private static final String OTHER_ISBN = "9788972756194";
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 12);
    private static final NewBookCandidate CANDIDATE = new NewBookCandidate(ISBN, TODAY.minusDays(1));
    private static final NewBookCandidate OTHER_CANDIDATE = new NewBookCandidate(OTHER_ISBN, TODAY.minusDays(2));

    @Mock
    private NewBookCollector collector;
    @Mock
    private NewBookImportLock lock;
    @Mock
    private NewBookImportNotifier notifier;
    @Mock
    private CollectNewBooksUseCase collection;
    @Mock
    private FindNewBookDetailUseCase details;
    @Mock
    private BookRepositoryPort books;
    @Mock
    private NewBookDetailCallBudget budget;
    @Mock
    private RegisterNewBookUseCase registration;
    @Mock
    private NewBookLookupBackoff backoff;

    @Test
    void 락을_획득하지_못하면_두_API_모두_호출하지_않는다() {
        new NewBookImportService(lock, collection, details, registration, notifier).importNewBooks(TODAY);

        verifyNoInteractions(collection, details, notifier);
    }

    @Test
    void 최근_30일_후보의_ISBN을_정규화하고_중복은_처음_출간일을_유지한다() {
        when(collector.collectCandidates(TODAY.minusDays(30), TODAY.minusDays(1))).thenReturn(
            List.of(new NewBookCandidate("978-89-8392-198-7", TODAY.minusDays(1)), new NewBookCandidate("9788983921988", TODAY),
                new NewBookCandidate(ISBN, TODAY.minusDays(9)), OTHER_CANDIDATE));

        assertThat(new CollectNewBooksUseCase(collector).collect(TODAY)).containsExactly(CANDIDATE, OTHER_CANDIDATE);
    }

    @Test
    void 호출_제한을_만나면_남은_ISBN을_조회하지_않는다() {
        doAnswer((final var invocation) -> {
            invocation.getArgument(0, Runnable.class).run();
            return true;
        }).when(lock).runIfAcquired(any());
        when(collection.collect(TODAY)).thenReturn(List.of(CANDIDATE, OTHER_CANDIDATE));
        when(details.findDetail(ISBN, TODAY)).thenReturn(NewBookImportOutcome.RATE_LIMITED);

        new NewBookImportService(lock, collection, details, registration, notifier).importNewBooks(TODAY);

        verify(details).findDetail(ISBN, TODAY);
        verifyNoMoreInteractions(details);
    }

    @Test
    void 이미_등록된_도서는_호출하지_않는다() {
        when(books.existsByIsbn13(ISBN)).thenReturn(true);

        assertThat(importer().findDetail(ISBN, TODAY)).isEqualTo(NewBookImportOutcome.ALREADY_REGISTERED);
        verifyNoInteractions(budget, collector, registration);
    }

    @Test
    void 예산이_없으면_상세_조회하지_않는다() {
        assertThat(importer().findDetail(ISBN, TODAY)).isEqualTo(NewBookImportOutcome.BUDGET_EXHAUSTED);
        verifyNoInteractions(collector, registration);
    }

    @Test
    void 상세가_없으면_저장하지_않는다() {
        when(budget.tryAcquire()).thenReturn(true);
        when(collector.collectDetail(ISBN)).thenReturn(Optional.empty());

        assertThat(importer().findDetail(ISBN, TODAY)).isEqualTo(NewBookImportOutcome.MISSING_DETAIL);
        verify(backoff).deferMissingDetail(ISBN, TODAY);
        verifyNoInteractions(registration);
    }

    @Test
    void 정보나루_호출_제한을_공유_예산에_반영한다() {
        when(budget.tryAcquire()).thenReturn(true);
        when(collector.collectDetail(ISBN)).thenThrow(new NewBookCollectorException(NewBookCollectorFailureReason.RATE_LIMIT));

        assertThat(importer().findDetail(ISBN, TODAY)).isEqualTo(NewBookImportOutcome.RATE_LIMITED);
        verify(budget).pauseAfterRateLimit();
        verifyNoInteractions(registration);
    }

    @Test
    void 재시도_날짜_전이면_예산을_쓰지_않고_건너뛴다() {
        when(backoff.isWaiting(ISBN, TODAY)).thenReturn(true);

        assertThat(importer().findDetail(ISBN, TODAY)).isEqualTo(NewBookImportOutcome.WAITING_RETRY);
        verifyNoInteractions(budget, collector);
    }

    @Test
    void 일시_오류는_ISBN_재시도를_미룬다() {
        when(budget.tryAcquire()).thenReturn(true);
        when(collector.collectDetail(ISBN)).thenThrow(new NewBookCollectorException(NewBookCollectorFailureReason.TEMPORARY));

        assertThat(importer().findDetail(ISBN, TODAY)).isEqualTo(NewBookImportOutcome.LOOKUP_FAILED);
        verify(backoff).deferAfterFailure(ISBN, TODAY);
    }

    @Test
    void 요청_거부는_ISBN을_미루지_않는다() {
        when(budget.tryAcquire()).thenReturn(true);
        when(collector.collectDetail(ISBN)).thenThrow(new NewBookCollectorException(NewBookCollectorFailureReason.REJECTED));

        assertThatThrownBy(() -> importer().findDetail(ISBN, TODAY)).isInstanceOf(NewBookCollectorException.class);
        verify(backoff).isWaiting(ISBN, TODAY);
        verifyNoMoreInteractions(backoff);
    }

    @Test
    void 요청_거부가_나면_남은_후보를_조회하지_않고_실패로_보고한다() {
        doAnswer((final var invocation) -> {
            invocation.getArgument(0, Runnable.class).run();
            return true;
        }).when(lock).runIfAcquired(any());
        when(collection.collect(TODAY)).thenReturn(List.of(CANDIDATE, OTHER_CANDIDATE));
        when(details.findDetail(ISBN, TODAY)).thenThrow(new NewBookCollectorException(NewBookCollectorFailureReason.REJECTED, "authErr"));

        assertThatThrownBy(() -> new NewBookImportService(lock, collection, details, registration, notifier).importNewBooks(TODAY))
            .isInstanceOf(NewBookCollectorException.class);
        verify(details).findDetail(ISBN, TODAY);
        verifyNoMoreInteractions(details);
        verify(notifier).send(argThat((final var report) -> report.failureReason().contains("authErr") && report.detailCallCount() == 1));
    }

    @Test
    void 일시_오류가_연속_5회면_중단한다() {
        runBatchWith(NewBookImportOutcome.LOOKUP_FAILED, 7);

        verify(details, times(5)).findDetail(any(), any());
        verify(notifier).send(argThat(NewBookImportReport::failed));
    }

    @Test
    void 조회_후_등록하고_ISBN_중복과_다른_저장_제약을_구분한다() {
        final var detail = new BookDetail("신간", "저자", null, "출판사", "800", null);
        doAnswer((final var invocation) -> {
            invocation.getArgument(0, Runnable.class).run();
            return true;
        }).when(lock).runIfAcquired(any());
        when(collection.collect(TODAY)).thenReturn(List.of(CANDIDATE, OTHER_CANDIDATE));
        when(details.findDetail(any(), any())).thenReturn(new NewBookDetailFound(detail));
        when(registration.register(CANDIDATE, detail)).thenThrow(new DuplicateImportedIsbnException(new RuntimeException()));
        when(registration.register(OTHER_CANDIDATE, detail)).thenThrow(new DataIntegrityViolationException("other constraint"));
        final var logger = (Logger) LoggerFactory.getLogger(NewBookImportService.class);
        final var appender = new ListAppender<ILoggingEvent>();
        appender.start();
        logger.addAppender(appender);
        try {
            new NewBookImportService(lock, collection, details, registration, notifier).importNewBooks(TODAY);
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }

        final var order = inOrder(details, registration);
        order.verify(details).findDetail(ISBN, TODAY);
        order.verify(registration).register(CANDIDATE, detail);
        order.verify(details).findDetail(OTHER_ISBN, TODAY);
        order.verify(registration).register(OTHER_CANDIDATE, detail);
        assertThat(appender.list).extracting(ILoggingEvent::getFormattedMessage).anySatisfy(
            (final var message) -> assertThat(message).contains("outcomes={ALREADY_REGISTERED_AFTER_LOOKUP=1, STORAGE_REJECTED=1}"));
    }

    private void runBatchWith(final NewBookImportOutcome outcome, final int candidates) {
        doAnswer((final var invocation) -> {
            invocation.getArgument(0, Runnable.class).run();
            return true;
        }).when(lock).runIfAcquired(any());
        final var isbns = IntStream.range(0, candidates)
            .mapToObj((final var index) -> index == 0 ? ISBN : "97889839219" + String.format("%02d", index))
            .map((final var isbn) -> new NewBookCandidate(isbn, TODAY.minusDays(1))).toList();
        when(collection.collect(TODAY)).thenReturn(isbns);
        when(details.findDetail(any(), any())).thenReturn(outcome);

        new NewBookImportService(lock, collection, details, registration, notifier).importNewBooks(TODAY);
    }

    private FindNewBookDetailUseCase importer() {
        return new FindNewBookDetailUseCase(books, collector, budget, backoff);
    }
}
