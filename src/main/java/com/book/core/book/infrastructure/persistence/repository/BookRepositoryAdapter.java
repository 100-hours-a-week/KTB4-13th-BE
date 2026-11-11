package com.book.core.book.infrastructure.persistence.repository;

import com.book.core.book.application.port.BookRepositoryPort;
import com.book.core.book.domain.Book;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class BookRepositoryAdapter implements BookRepositoryPort {
    private final BookJpaRepository jpaRepository;

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
