package com.book.core.library.api.response;

import com.book.core.library.application.result.LibraryBookResult;
import java.util.List;

public record LibraryBooksResponse(List<LibraryBookResponse> books) {
    public static LibraryBooksResponse from(final List<LibraryBookResult> results) {
        return new LibraryBooksResponse(results.stream().map(LibraryBookResponse::from).toList());
    }
}
