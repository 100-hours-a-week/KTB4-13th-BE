package com.book.core.search.application.port;

public interface BookSearchClient {
    BookSearchResult search(final BookSearchRequest request);
}
