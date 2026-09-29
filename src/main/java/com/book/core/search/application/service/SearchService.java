package com.book.core.search.application.service;

import com.book.core.search.application.command.SearchBooksCommand;
import com.book.core.search.application.port.BookSearchResult;
import com.book.core.search.application.usecase.SearchBooksUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SearchService {
    private final SearchBooksUseCase searchBooksUseCase;

    public BookSearchResult searchBooks(final SearchBooksCommand command) {
        return searchBooksUseCase.execute(command);
    }
}
