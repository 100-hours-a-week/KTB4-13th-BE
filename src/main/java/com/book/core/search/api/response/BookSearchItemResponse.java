package com.book.core.search.api.response;

import com.book.core.search.application.port.BookSearchItem;
import java.math.BigDecimal;

// @formatter:off
public record BookSearchItemResponse(
        Long bookId,
        String title,
        String author,
        String publisher,
        BigDecimal price,
        boolean inStock,
        String coverUrl) {
    public static BookSearchItemResponse from(final BookSearchItem item) {
        return new BookSearchItemResponse(item.bookId(), item.title(), item.author(), item.publisher(), item.price(), item.inStock(),
                item.coverUrl());
    }
}
// @formatter:on
