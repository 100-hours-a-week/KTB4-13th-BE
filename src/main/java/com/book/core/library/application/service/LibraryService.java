package com.book.core.library.application.service;

import com.book.core.book.application.usecase.GetAiBookIdentifiersUseCase;
import com.book.core.library.application.command.CreateShelfPhotoUploadCommand;
import com.book.core.library.application.command.DeleteLibraryBooksCommand;
import com.book.core.library.application.command.ScanShelfPhotoCommand;
import com.book.core.library.application.result.LibraryBookResult;
import com.book.core.library.application.result.ShelfPhotoUploadResult;
import com.book.core.library.application.result.ShelfScanResult;
import com.book.core.library.application.result.ShelfScanResult.Book;
import com.book.core.library.application.result.ShelfScanResult.Item;
import com.book.core.library.application.usecase.CreateShelfPhotoUploadUseCase;
import com.book.core.library.application.usecase.DeleteLibraryBooksUseCase;
import com.book.core.library.application.usecase.GetLibraryBooksUseCase;
import com.book.core.library.application.usecase.ScanShelfPhotoUseCase;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LibraryService {
    private final GetLibraryBooksUseCase getLibraryBooksUseCase;
    private final DeleteLibraryBooksUseCase deleteLibraryBooksUseCase;
    private final CreateShelfPhotoUploadUseCase createShelfPhotoUploadUseCase;
    private final ScanShelfPhotoUseCase scanShelfPhotoUseCase;
    private final GetAiBookIdentifiersUseCase getAiBookIdentifiersUseCase;

    public List<LibraryBookResult> getBooks(final Long userId) {
        return getLibraryBooksUseCase.execute(userId);
    }

    public void deleteBooks(final DeleteLibraryBooksCommand command) {
        deleteLibraryBooksUseCase.execute(command);
    }

    public ShelfPhotoUploadResult createPhotoUpload(final CreateShelfPhotoUploadCommand command) {
        return createShelfPhotoUploadUseCase.execute(command);
    }

    public ShelfScanResult scanPhoto(final ScanShelfPhotoCommand command) {
        final ShelfScanResult scan = scanShelfPhotoUseCase.execute(command);
        final List<String> isbns = scan.items().stream().map(Item::book).filter(Objects::nonNull).map(Book::isbn).toList();
        return scan.withBookIds(isbns.isEmpty() ? Map.of() : getAiBookIdentifiersUseCase.findBookIds(isbns));
    }
}
