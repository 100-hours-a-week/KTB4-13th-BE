package com.book.core.book.infrastructure.persistence.repository;

import com.book.core.book.domain.Book;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;

interface BookJpaRepository extends JpaRepository<Book, Long> {
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
