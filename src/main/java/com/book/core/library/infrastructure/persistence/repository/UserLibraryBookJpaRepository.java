package com.book.core.library.infrastructure.persistence.repository;

import com.book.core.library.domain.UserLibraryBook;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface UserLibraryBookJpaRepository extends JpaRepository<UserLibraryBook, Long> {
    List<UserLibraryBook> findByUserIdAndBookIdIn(final Long userId, final List<Long> bookIds);

    List<UserLibraryBook> findByUserIdAndIdInAndDeletedAtIsNull(final Long userId, final List<Long> ids);

    List<UserLibraryBook> findByUserIdAndDeletedAtIsNull(final Long userId);
}
