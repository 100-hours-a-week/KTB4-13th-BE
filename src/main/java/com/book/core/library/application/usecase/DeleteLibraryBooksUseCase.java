package com.book.core.library.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.library.application.command.DeleteLibraryBooksCommand;
import com.book.core.library.application.port.UserLibraryBookRepositoryPort;
import com.book.core.library.domain.UserLibraryBook;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class DeleteLibraryBooksUseCase {
    private final UserLibraryBookRepositoryPort libraryBookRepository;

    /** 회원 소유의 활성 항목만 삭제하고, 없거나 이미 삭제된 항목은 무시합니다. */
    @Transactional
    public void execute(final DeleteLibraryBooksCommand command) {
        final List<Long> libraryBookIds = command.libraryBookIds().stream().distinct().toList();
        libraryBookRepository.findActiveByUserIdAndIds(command.userId(), libraryBookIds).forEach(UserLibraryBook::delete);
    }
}
