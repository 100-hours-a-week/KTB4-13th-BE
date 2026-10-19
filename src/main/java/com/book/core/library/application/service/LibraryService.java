package com.book.core.library.application.service;

import com.book.core.library.application.command.DeleteLibraryBooksCommand;
import com.book.core.library.application.usecase.DeleteLibraryBooksUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LibraryService {
    private final DeleteLibraryBooksUseCase deleteLibraryBooksUseCase;

    public void deleteBooks(final DeleteLibraryBooksCommand command) {
        deleteLibraryBooksUseCase.execute(command);
    }
}
