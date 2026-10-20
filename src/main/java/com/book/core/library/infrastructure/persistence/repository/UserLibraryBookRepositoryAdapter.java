package com.book.core.library.infrastructure.persistence.repository;

import com.book.core.library.application.port.UserLibraryBookRepositoryPort;
import com.book.core.library.domain.UserLibraryBook;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class UserLibraryBookRepositoryAdapter implements UserLibraryBookRepositoryPort {
    private static final String USER_BOOK_UNIQUE_CONSTRAINT = "uk_user_library_books_user_book";

    private final UserLibraryBookJpaRepository jpaRepository;

    @Override
    public List<UserLibraryBook> findByUserIdAndBookIds(final Long userId, final List<Long> bookIds) {
        return jpaRepository.findByUserIdAndBookIdIn(userId, bookIds);
    }

    @Override
    public List<UserLibraryBook> findActiveByUserIdAndIds(final Long userId, final List<Long> libraryBookIds) {
        return jpaRepository.findByUserIdAndIdInAndDeletedAtIsNull(userId, libraryBookIds);
    }

    @Override
    public List<UserLibraryBook> findActiveByUserId(final Long userId) {
        return jpaRepository.findByUserIdAndDeletedAtIsNull(userId);
    }

    /** 동시 추가로 회원·도서 UNIQUE 제약을 위반하면 재시도할 수 있도록 {@link DuplicateKeyException}으로 구분합니다. */
    @Override
    public List<UserLibraryBook> saveAll(final List<UserLibraryBook> libraryBooks) {
        try {
            return jpaRepository.saveAll(libraryBooks);
        } catch (final DataIntegrityViolationException exception) {
            if (violatesUserBookUniqueConstraint(exception)) {
                throw new DuplicateKeyException(exception.getMessage(), exception);
            }
            throw exception;
        }
    }

    private boolean violatesUserBookUniqueConstraint(final Throwable exception) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof final ConstraintViolationException constraintViolation) {
                final String constraintName = constraintViolation.getConstraintName();
                return constraintName != null && constraintName.endsWith(USER_BOOK_UNIQUE_CONSTRAINT);
            }
            cause = cause.getCause();
        }
        return false;
    }
}
