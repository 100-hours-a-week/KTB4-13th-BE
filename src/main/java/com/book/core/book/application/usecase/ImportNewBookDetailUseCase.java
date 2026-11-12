package com.book.core.book.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.book.application.port.BookCatalogException;
import com.book.core.book.application.port.BookCatalogFailureReason;
import com.book.core.book.application.port.BookDetailCallBudget;
import com.book.core.book.application.port.BookRepositoryPort;
import com.book.core.book.application.port.DataForLibraryBookDetailClient;
import com.book.core.book.application.result.NewBookDetailFound;
import com.book.core.book.application.result.NewBookDetailLookup;
import com.book.core.book.application.result.NewBookImportOutcome;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class ImportNewBookDetailUseCase {
    private final BookRepositoryPort books;
    private final DataForLibraryBookDetailClient client;
    private final BookDetailCallBudget budget;

    public NewBookDetailLookup findDetail(final String isbn13) {
        if (books.existsByIsbn13(isbn13)) {
            return NewBookImportOutcome.ALREADY_REGISTERED;
        }
        if (!budget.tryAcquire()) {
            return NewBookImportOutcome.BUDGET_EXHAUSTED;
        }

        try {
            final var detail = client.findBookDetail(isbn13);
            if (detail.isEmpty()) {
                return NewBookImportOutcome.MISSING_DETAIL;
            }
            return new NewBookDetailFound(detail.orElseThrow());
        } catch (final BookCatalogException exception) {
            if (exception.reason() == BookCatalogFailureReason.RATE_LIMIT) {
                budget.pauseAfterRateLimit();
                return NewBookImportOutcome.RATE_LIMITED;
            }
            log.atInfo().addKeyValue("isbn13", isbn13).addKeyValue("reason", exception.reason())
                .log("[BOOK] 신간 도서 상세 조회에 실패했습니다. isbn13={}, reason={}", isbn13, exception.reason());
            return NewBookImportOutcome.LOOKUP_FAILED;
        }
    }
}
