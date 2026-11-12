package com.book.core.book.infrastructure.persistence.repository;

import com.book.core.book.domain.Book;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface BookJpaRepository extends JpaRepository<Book, Long> {
    @Query(value = "SELECT EXISTS(SELECT 1 FROM books WHERE normalized_isbn13 = :isbn13)", nativeQuery = true)
    long selectIsbn13Exists(String isbn13);

    default boolean existsByNormalizedIsbn13(final String isbn13) {
        return selectIsbn13Exists(isbn13) != 0;
    }

    String UNAMBIGUOUS_ISBN = """
        select b from Book b
        where b.isbn13 is not null and not exists (
            select 1 from Book other where other.isbn13 = b.isbn13 and other.id <> b.id
        )
        """;

    @Query(UNAMBIGUOUS_ISBN + " and b.id in :ids")
    List<Book> findWithUnambiguousIsbnByIds(List<Long> ids);

    @Query(UNAMBIGUOUS_ISBN + " and b.isbn13 in :isbns")
    List<Book> findWithUnambiguousIsbnByIsbns(List<String> isbns);
}
