package com.book.core.search.api.response;

import com.book.core.search.application.port.BookSearchResult;
import java.util.List;

// @formatter:off
public record BookSearchResponse(List<BookSearchItemResponse> items, String nextCursor, String fallbackMessage) {
    public static BookSearchResponse from(final BookSearchResult result) {
        return new BookSearchResponse(result.items().stream().map(BookSearchItemResponse::from).toList(), result.nextCursor(),
                result.fallbackMessage());
    }
}
// @formatter:on
