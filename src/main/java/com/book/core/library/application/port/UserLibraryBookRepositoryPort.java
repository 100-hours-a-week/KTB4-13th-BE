package com.book.core.library.application.port;

import com.book.core.library.domain.UserLibraryBook;
import java.util.List;

public interface UserLibraryBookRepositoryPort {
    /** 삭제된 항목을 포함해 회원의 도서관 항목을 도서 ID로 조회합니다. */
    List<UserLibraryBook> findByUserIdAndBookIds(final Long userId, final List<Long> bookIds);

    List<UserLibraryBook> findActiveByUserIdAndIds(final Long userId, final List<Long> libraryBookIds);

    List<UserLibraryBook> saveAll(final List<UserLibraryBook> libraryBooks);
}
