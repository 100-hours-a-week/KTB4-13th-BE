package com.book.core.library.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.library.application.command.AddLibraryBooksCommand;
import com.book.core.library.application.port.UserLibraryBookRepositoryPort;
import com.book.core.library.domain.UserLibraryBook;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class AddLibraryBooksUseCase {
    private final UserLibraryBookRepositoryPort libraryBookRepository;
    private final Clock clock;

    /** 도서 존재 여부는 호출자가 검증합니다. 동시 추가로 UNIQUE 제약을 위반하면 DuplicateKeyException이 전파됩니다. */
    @Transactional
    public void execute(final AddLibraryBooksCommand command) {
        final List<Long> bookIds = command.bookIds().stream().distinct().sorted().toList();
        if (bookIds.isEmpty()) {
            return;
        }
        final LocalDateTime now = LocalDateTime.now(clock);
        final List<UserLibraryBook> existingBooks = libraryBookRepository.findByUserIdAndBookIds(command.userId(), bookIds);
        existingBooks.forEach(libraryBook -> libraryBook.restore(now));

        final Set<Long> existingBookIds = existingBooks.stream().map(UserLibraryBook::bookId).collect(Collectors.toSet());
        final List<UserLibraryBook> newBooks = bookIds.stream().filter(bookId -> !existingBookIds.contains(bookId))
            .map(bookId -> UserLibraryBook.add(command.userId(), bookId, now)).toList();
        libraryBookRepository.saveAll(newBooks);
    }
}
