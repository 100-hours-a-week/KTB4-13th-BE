package com.book.core.search.application.result;

import java.math.BigDecimal;

// @formatter:off
public record SearchBookItemResult(
        Long bookId,
        String isbn,
        Long productId,
        String title,
        String author,
        String publisher,
        BigDecimal price,
        boolean inStock,
        String coverUrl) {}
// @formatter:on
