package com.book.core.library.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.library.application.port.UserLibraryBookRepositoryPort;
import com.book.core.library.domain.UserLibraryBook;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class DeleteAllLibraryBooksUseCase {
    private final UserLibraryBookRepositoryPort libraryBookRepository;

    /** 회원의 활성 항목만 Soft Delete하고, 이미 삭제된 항목은 삭제 시각을 유지하며 복구하지 않습니다. */
    @Transactional
    public void execute(final Long userId) {
        libraryBookRepository.findActiveByUserId(userId).forEach(UserLibraryBook::delete);
    }
}
