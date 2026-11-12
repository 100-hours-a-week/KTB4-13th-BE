package com.book.core.book.application.port;

import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
public final class BookCatalogException extends RuntimeException {
    private final BookCatalogFailureReason reason;

    public BookCatalogException(final BookCatalogFailureReason reason) {
        super(reason.name());
        this.reason = reason;
    }
}
