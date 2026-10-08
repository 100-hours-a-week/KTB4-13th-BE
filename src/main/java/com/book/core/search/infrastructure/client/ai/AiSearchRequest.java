package com.book.core.search.infrastructure.client.ai;

import com.book.core.search.application.port.BookSearchRequest;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Locale;

// @formatter:off
@JsonInclude(JsonInclude.Include.NON_NULL)
record AiSearchRequest(
    String query,
    AiSearchFilters filters,
    String sort,
    String cursor,
    int size
) {
    static AiSearchRequest from(final BookSearchRequest request) {
        return new AiSearchRequest(request.query(), AiSearchFilters.from(request), request.sort().name().toLowerCase(Locale.ROOT),
            request.cursor(), request.size());
    }
}
// @formatter:on
