package com.book.core.library.application.service;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.book.application.usecase.GetAiBookIdentifiersUseCase;
import com.book.core.book.application.usecase.GetBooksUseCase;
import com.book.core.library.application.command.AddLibraryBooksCommand;
import com.book.core.library.application.command.CreateShelfPhotoUploadCommand;
import com.book.core.library.application.command.DeleteLibraryBooksCommand;
import com.book.core.library.application.command.ScanShelfPhotoCommand;
import com.book.core.library.application.result.LibraryBookResult;
import com.book.core.library.application.result.ShelfPhotoUploadResult;
import com.book.core.library.application.result.ShelfScanResult.Book;
import com.book.core.library.application.result.ShelfScanResult.Item;
import com.book.core.library.application.result.ShelfScanResult;
import com.book.core.library.application.usecase.AddLibraryBooksUseCase;
import com.book.core.library.application.usecase.CreateShelfPhotoUploadUseCase;
import com.book.core.library.application.usecase.DeleteLibraryBooksUseCase;
import com.book.core.library.application.usecase.GetLibraryBooksUseCase;
import com.book.core.library.application.usecase.ScanShelfPhotoUseCase;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LibraryService {
    private final GetLibraryBooksUseCase getLibraryBooksUseCase;
    private final AddLibraryBooksUseCase addLibraryBooksUseCase;
    private final GetBooksUseCase getBooksUseCase;
    private final DeleteLibraryBooksUseCase deleteLibraryBooksUseCase;
    private final CreateShelfPhotoUploadUseCase createShelfPhotoUploadUseCase;
    private final ScanShelfPhotoUseCase scanShelfPhotoUseCase;
    private final GetAiBookIdentifiersUseCase getAiBookIdentifiersUseCase;

    public List<LibraryBookResult> getBooks(final Long userId) {
        return getLibraryBooksUseCase.execute(userId);
    }

    /** 같은 책을 동시에 담아 UNIQUE 제약이 충돌하면 먼저 저장된 항목을 기존 정책대로 처리하도록 새 트랜잭션에서 한 번 더 실행합니다. */
    public void addBooks(final AddLibraryBooksCommand command) {
        final List<Long> bookIds = command.bookIds().stream().distinct().toList();
        if (getBooksUseCase.execute(bookIds).size() != bookIds.size()) {
            throw new CoreException(ErrorCode.LIBRARY_BOOK_NOT_FOUND);
        }
        try {
            addLibraryBooksUseCase.execute(command);
        } catch (final DuplicateKeyException exception) {
            addLibraryBooksUseCase.execute(command);
        }
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
