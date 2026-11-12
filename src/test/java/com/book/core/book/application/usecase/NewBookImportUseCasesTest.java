package com.book.core.book.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.book.core.book.application.port.BookCatalogException.Reason;
import com.book.core.book.application.port.BookCatalogException;
import com.book.core.book.application.port.BookDetailCallBudget;
import com.book.core.book.application.port.BookRepositoryPort;
import com.book.core.book.application.port.DataForLibraryBookDetailClient.BookDetail;
import com.book.core.book.application.port.DataForLibraryBookDetailClient;
import com.book.core.book.application.port.DuplicateImportedIsbnException;
import com.book.core.book.application.port.NationalLibraryIsbnClient.Page;
import com.book.core.book.application.port.NationalLibraryIsbnClient;
import com.book.core.book.application.port.NewBookImportLock;
import com.book.core.book.application.service.NewBookImportService;
import com.book.core.book.application.usecase.ImportNewBookDetailUseCase.Found;
import com.book.core.book.application.usecase.ImportNewBookDetailUseCase.Outcome;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
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

    @Mock
    private NationalLibraryIsbnClient nationalLibrary;
    @Mock
    private NewBookImportLock lock;
    @Mock
    private CollectNewBookIsbnsUseCase collection;
    @Mock
    private ImportNewBookDetailUseCase details;
    @Mock
    private BookRepositoryPort books;
    @Mock
    private DataForLibraryBookDetailClient detailClient;
    @Mock
    private BookDetailCallBudget budget;
    @Mock
    private RegisterNewBookUseCase registration;

    @Test
    void 락을_획득하지_못하면_두_API_모두_호출하지_않는다() {
        new NewBookImportService(lock, collection, details, registration).importNewBooks(TODAY);

        verifyNoInteractions(collection, details);
    }

    @Test
    void 전체_페이지의_ISBN을_정규화하고_중복을_제거한다() {
        final LocalDate from = TODAY.minusDays(30);
        final LocalDate through = TODAY.minusDays(1);
        when(nationalLibrary.findPaperBookIsbns(from, through, 1))
            .thenReturn(new Page(List.of("978-89-8392-198-7", "9788983921988"), true));
        when(nationalLibrary.findPaperBookIsbns(from, through, 2)).thenReturn(new Page(List.of(ISBN, OTHER_ISBN), false));

        assertThat(new CollectNewBookIsbnsUseCase(nationalLibrary).collect(TODAY)).containsExactly(ISBN, OTHER_ISBN);
    }

    @Test
    void 호출_제한을_만나면_남은_ISBN을_조회하지_않는다() {
        doAnswer((final var invocation) -> {
            invocation.getArgument(0, Runnable.class).run();
            return true;
        }).when(lock).runIfAcquired(any());
        when(collection.collect(TODAY)).thenReturn(List.of(ISBN, OTHER_ISBN));
        when(details.findDetail(ISBN)).thenReturn(Outcome.RATE_LIMITED);

        new NewBookImportService(lock, collection, details, registration).importNewBooks(TODAY);

        verify(details).findDetail(ISBN);
        org.mockito.Mockito.verifyNoMoreInteractions(details);
    }

    @Test
    void 이미_등록된_도서는_호출하지_않는다() {
        when(books.existsByIsbn13(ISBN)).thenReturn(true);

        assertThat(importer().findDetail(ISBN)).isEqualTo(Outcome.ALREADY_REGISTERED);
        verifyNoInteractions(budget, detailClient, registration);
    }

    @Test
    void 예산이_없으면_상세_조회하지_않는다() {
        assertThat(importer().findDetail(ISBN)).isEqualTo(Outcome.BUDGET_EXHAUSTED);
        verifyNoInteractions(detailClient, registration);
    }

    @Test
    void 상세가_없으면_저장하지_않는다() {
        when(budget.tryAcquire()).thenReturn(true);
        when(detailClient.findBookDetail(ISBN)).thenReturn(Optional.empty());

        assertThat(importer().findDetail(ISBN)).isEqualTo(Outcome.MISSING_DETAIL);
        verifyNoInteractions(registration);
    }

    @Test
    void 정보나루_호출_제한을_공유_예산에_반영한다() {
        when(budget.tryAcquire()).thenReturn(true);
        when(detailClient.findBookDetail(ISBN)).thenThrow(new BookCatalogException(Reason.RATE_LIMIT));

        assertThat(importer().findDetail(ISBN)).isEqualTo(Outcome.RATE_LIMITED);
        verify(budget).pauseAfterRateLimit();
        verifyNoInteractions(registration);
    }

    @Test
    void 조회_후_등록하고_ISBN_중복과_다른_저장_제약을_구분한다() {
        final var detail = new BookDetail("신간", "저자", null, "출판사", "800", TODAY.minusDays(1), null);
        doAnswer((final var invocation) -> {
            invocation.getArgument(0, Runnable.class).run();
            return true;
        }).when(lock).runIfAcquired(any());
        when(collection.collect(TODAY)).thenReturn(List.of(ISBN, OTHER_ISBN));
        when(details.findDetail(any())).thenReturn(new Found(detail));
        when(registration.register(ISBN, detail)).thenThrow(new DuplicateImportedIsbnException(new RuntimeException()));
        when(registration.register(OTHER_ISBN, detail)).thenThrow(new DataIntegrityViolationException("other constraint"));
        final var logger = (Logger) LoggerFactory.getLogger(NewBookImportService.class);
        final var appender = new ListAppender<ILoggingEvent>();
        appender.start();
        logger.addAppender(appender);
        try {
            new NewBookImportService(lock, collection, details, registration).importNewBooks(TODAY);
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }

        final var order = inOrder(details, registration);
        order.verify(details).findDetail(ISBN);
        order.verify(registration).register(ISBN, detail);
        order.verify(details).findDetail(OTHER_ISBN);
        order.verify(registration).register(OTHER_ISBN, detail);
        assertThat(appender.list).extracting(ILoggingEvent::getFormattedMessage).anySatisfy(
            (final var message) -> assertThat(message).contains("outcomes={ALREADY_REGISTERED_AFTER_LOOKUP=1, STORAGE_REJECTED=1}"));
    }

    private ImportNewBookDetailUseCase importer() {
        return new ImportNewBookDetailUseCase(books, detailClient, budget);
    }
}
