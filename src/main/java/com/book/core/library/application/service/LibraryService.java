package com.book.core.library.application.service;

import com.book.core.library.application.command.DeleteLibraryBooksCommand;
import com.book.core.library.application.result.LibraryBookResult;
import com.book.core.library.application.usecase.DeleteLibraryBooksUseCase;
import com.book.core.library.application.usecase.GetLibraryBooksUseCase;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LibraryService {
    private final GetLibraryBooksUseCase getLibraryBooksUseCase;
    private final DeleteLibraryBooksUseCase deleteLibraryBooksUseCase;

    public List<LibraryBookResult> getBooks(final Long userId) {
        return getLibraryBooksUseCase.execute(userId);
    }

    public void deleteBooks(final DeleteLibraryBooksCommand command) {
        deleteLibraryBooksUseCase.execute(command);
    }
}
