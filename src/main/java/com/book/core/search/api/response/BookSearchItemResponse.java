package com.book.core.search.api.response;

import com.book.core.search.application.result.SearchBookItemResult;
import java.math.BigDecimal;

// @formatter:off
public record BookSearchItemResponse(
        Long bookId,
        Long productId,
        String title,
        String author,
        String publisher,
        BigDecimal price,
        boolean inStock,
        String coverUrl) {
    public static BookSearchItemResponse from(final SearchBookItemResult item) {
        return new BookSearchItemResponse(item.bookId(), item.productId(), item.title(), item.author(), item.publisher(), item.price(),
                item.inStock(), item.coverUrl());
    }
}
// @formatter:on
