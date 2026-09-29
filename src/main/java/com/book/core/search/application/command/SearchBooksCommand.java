package com.book.core.search.application.command;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;

// @formatter:off
public record SearchBooksCommand(
        String query,
        String category,
        Integer priceMin,
        Integer priceMax,
        Integer pubYearFrom,
        Integer pubYearTo,
        BookSearchSort sort,
        String cursor,
        int size) {
    public SearchBooksCommand {
        if (priceMin != null && priceMax != null && priceMin > priceMax) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        if (pubYearFrom != null && pubYearTo != null && pubYearFrom > pubYearTo) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
    }
}
// @formatter:on
