package com.book.core.book.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.book.application.port.BookCatalogException;
import com.book.core.book.application.port.BookCatalogException.Reason;
import com.book.core.book.application.port.BookDetailCallBudget;
import com.book.core.book.application.port.BookRepositoryPort;
import com.book.core.book.application.port.DataForLibraryBookDetailClient;
import com.book.core.book.application.port.DataForLibraryBookDetailClient.BookDetail;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class ImportNewBookDetailUseCase {
    private final BookRepositoryPort books;
    private final DataForLibraryBookDetailClient client;
    private final BookDetailCallBudget budget;

    public DetailLookup findDetail(final String isbn13) {
        if (books.existsByIsbn13(isbn13)) {
            return Outcome.ALREADY_REGISTERED;
        }
        if (!budget.tryAcquire()) {
            return Outcome.BUDGET_EXHAUSTED;
        }

        try {
            final var detail = client.findBookDetail(isbn13);
            if (detail.isEmpty()) {
                return Outcome.MISSING_DETAIL;
            }
            return new Found(detail.orElseThrow());
        } catch (final BookCatalogException exception) {
            if (exception.reason() == Reason.RATE_LIMIT) {
                budget.pauseAfterRateLimit();
                return Outcome.RATE_LIMITED;
            }
            log.atInfo().addKeyValue("isbn13", isbn13).addKeyValue("reason", exception.reason())
                .log("[BOOK] 신간 도서 상세 조회에 실패했습니다. isbn13={}, reason={}", isbn13, exception.reason());
            return Outcome.LOOKUP_FAILED;
        }
    }

    public sealed interface DetailLookup permits Found, Outcome {
    }

    public record Found(BookDetail detail) implements DetailLookup {
    }

    public enum Outcome implements DetailLookup {
        ALREADY_REGISTERED,
        BUDGET_EXHAUSTED,
        MISSING_DETAIL,
        REGISTERED,
        ALREADY_REGISTERED_AFTER_LOOKUP,
        STORAGE_REJECTED,
        RATE_LIMITED,
        LOOKUP_FAILED
    }
}
