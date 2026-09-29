package com.book.core.search.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.search.application.command.SearchBooksCommand;
import com.book.core.search.application.port.BookSearchClient;
import com.book.core.search.application.port.BookSearchRequest;
import com.book.core.search.application.port.BookSearchResult;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class SearchBooksUseCase {
    private final BookSearchClient bookSearchClient;

    public BookSearchResult execute(final SearchBooksCommand command) {
        return bookSearchClient.search(new BookSearchRequest(command.query(), command.category(), command.priceMin(), command.priceMax(),
            command.pubYearFrom(), command.pubYearTo(), command.sort(), command.cursor(), command.size()));
    }
}
