package com.book.core.search.application.port;

import java.math.BigDecimal;

// @formatter:off
public record BookSearchItem(
        Long bookId,
        String title,
        String author,
        String publisher,
        BigDecimal price,
        String coverUrl) {}
// @formatter:on
