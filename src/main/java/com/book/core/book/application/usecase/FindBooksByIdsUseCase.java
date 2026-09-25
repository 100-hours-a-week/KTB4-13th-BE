package com.book.core.book.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.book.application.port.BookRepositoryPort;
import com.book.core.book.domain.Book;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class FindBooksByIdsUseCase {
    private final BookRepositoryPort bookRepository;

    @Transactional(readOnly = true)
    public List<Book> execute(final List<Long> bookIds) {
        return bookRepository.findAllByIdIn(bookIds);
    }
}
