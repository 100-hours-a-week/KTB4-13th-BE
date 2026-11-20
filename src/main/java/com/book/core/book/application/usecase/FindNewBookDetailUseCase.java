package com.book.core.book.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.book.application.port.BookCatalogException;
import com.book.core.book.application.port.BookDetailCallBudget;
import com.book.core.book.application.port.BookRepositoryPort;
import com.book.core.book.application.port.NewBookCollector;
import com.book.core.book.application.port.NewBookLookupBackoff;
import com.book.core.book.application.result.NewBookDetailFound;
import com.book.core.book.application.result.NewBookDetailLookup;
import com.book.core.book.application.result.NewBookImportOutcome;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class FindNewBookDetailUseCase {
    private final BookRepositoryPort books;
    private final NewBookCollector collector;
    private final BookDetailCallBudget budget;
    private final NewBookLookupBackoff backoff;

    public NewBookDetailLookup findDetail(final String isbn13, final LocalDate today) {
        if (books.existsByIsbn13(isbn13)) {
            return NewBookImportOutcome.ALREADY_REGISTERED;
        }
        if (backoff.isWaiting(isbn13, today)) {
            return NewBookImportOutcome.WAITING_RETRY;
        }
        if (!budget.tryAcquire()) {
            return NewBookImportOutcome.BUDGET_EXHAUSTED;
        }

        try {
            final var detail = collector.collectDetail(isbn13);
            if (detail.isEmpty()) {
                backoff.deferMissingDetail(isbn13, today);
                return NewBookImportOutcome.MISSING_DETAIL;
            }
            return new NewBookDetailFound(detail.orElseThrow());
        } catch (final BookCatalogException exception) {
            log.atInfo().addKeyValue("isbn13", isbn13).addKeyValue("reason", exception.reason())
                .addKeyValue("externalCode", exception.externalCode())
                .log("[BOOK] 신간 도서 상세 조회에 실패했습니다. isbn13={}, reason={}, externalCode={}", isbn13, exception.reason(),
                    exception.externalCode());
            return switch (exception.reason()) {
                case RATE_LIMIT -> {
                    budget.pauseAfterRateLimit();
                    yield NewBookImportOutcome.RATE_LIMITED;
                }
                case REJECTED -> throw exception;
                case TEMPORARY -> {
                    backoff.deferAfterFailure(isbn13, today);
                    yield NewBookImportOutcome.LOOKUP_FAILED;
                }
            };
        }
    }
}
