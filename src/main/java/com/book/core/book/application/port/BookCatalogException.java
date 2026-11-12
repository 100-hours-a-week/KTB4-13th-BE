package com.book.core.book.application.port;

import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
public final class BookCatalogException extends RuntimeException {
    public enum Reason {
        TEMPORARY,
        RATE_LIMIT,
        REJECTED
    }

    private final Reason reason;

    public BookCatalogException(final Reason reason) {
        super(reason.name());
        this.reason = reason;
    }
}
