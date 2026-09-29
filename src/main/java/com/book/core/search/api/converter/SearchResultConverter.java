package com.book.core.search.api.converter;

import com.book.core.search.api.response.BookSearchResponse;
import com.book.core.search.application.port.BookSearchResult;
import org.springframework.stereotype.Component;

@Component
public class SearchResultConverter {
    public BookSearchResponse toBookSearchResponse(final BookSearchResult result) {
        return BookSearchResponse.from(result);
    }
}
