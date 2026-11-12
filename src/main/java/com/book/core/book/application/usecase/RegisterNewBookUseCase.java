package com.book.core.book.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.book.application.port.BookRepositoryPort;
import com.book.core.book.application.result.BookDetail;
import com.book.core.book.domain.Book;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class RegisterNewBookUseCase {
    private final BookRepositoryPort books;

    @Transactional
    public boolean register(final String isbn13, final BookDetail detail) {
        if (books.existsByIsbn13(isbn13)) {
            return false;
        }
        books.save(Book.registerNewBook(isbn13, detail.title(), detail.author(), detail.description(), detail.publisher(),
            detail.category(), detail.publishedAt(), detail.coverImageUrl()));
        return true;
    }
}
