package com.book.core.search.application.port;

import com.book.core.search.application.command.BookSearchSort;

// @formatter:off
public record BookSearchRequest(
        String query,
        String category,
        Integer priceMin,
        Integer priceMax,
        Integer pubYearFrom,
        Integer pubYearTo,
        BookSearchSort sort,
        String cursor,
        int size) {}
// @formatter:on
