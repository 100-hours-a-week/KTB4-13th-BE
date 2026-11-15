package com.book.core.book.application.port;

import java.time.LocalDate;

public interface NewBookLookupBackoff {
    boolean isWaiting(String isbn13, LocalDate today);

    void deferMissingDetail(String isbn13, LocalDate today);

    void deferAfterFailure(String isbn13, LocalDate today);
}
