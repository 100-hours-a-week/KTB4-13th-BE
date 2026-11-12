package com.book.core.book.infrastructure.persistence.repository;

import com.book.core.book.application.port.BookRepositoryPort;
import com.book.core.book.application.port.DuplicateImportedIsbnException;
import com.book.core.book.domain.Book;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class BookRepositoryAdapter implements BookRepositoryPort {
    private final BookJpaRepository jpaRepository;

    @Override
    public boolean existsByIsbn13(final String isbn13) {
        return jpaRepository.existsByNormalizedIsbn13(isbn13);
    }

    @Override
    public void save(final Book book) {
        try {
            jpaRepository.saveAndFlush(book);
        } catch (final DataIntegrityViolationException exception) {
            for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
                if (cause instanceof final ConstraintViolationException violation) {
                    final String name = violation.getConstraintName();
                    if ("uq_books_imported_isbn13".equals(name) || "books.uq_books_imported_isbn13".equals(name)) {
                        throw new DuplicateImportedIsbnException(exception);
                    }
                    break;
                }
            }
            throw exception;
        }
    }

    @Override
    public List<Book> findAllByIdIn(final List<Long> ids) {
        return jpaRepository.findAllById(ids);
    }

    @Override
    public List<Book> findWithUnambiguousIsbnByIds(final List<Long> ids) {
        return jpaRepository.findWithUnambiguousIsbnByIds(ids);
    }

    @Override
    public List<Book> findWithUnambiguousIsbnByIsbns(final List<String> isbns) {
        return jpaRepository.findWithUnambiguousIsbnByIsbns(isbns);
    }
}
