package com.book.core.book.application.port;

import com.book.core.book.domain.Book;
import java.util.List;

public interface BookRepositoryPort {
    List<Book> findAllByIdIn(final List<Long> ids);
}
