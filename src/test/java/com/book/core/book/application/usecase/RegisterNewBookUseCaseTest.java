package com.book.core.book.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.book.core.book.application.port.BookRepositoryPort;
import com.book.core.book.application.result.BookDetail;
import com.book.core.book.application.result.NewBookCandidate;
import com.book.core.book.domain.Book;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RegisterNewBookUseCaseTest {
    private static final String ISBN = "9788983921987";
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 12);

    @Mock
    private BookRepositoryPort books;

    @Test
    void 국중도_출간일로_저장하고_등록_시점에도_같은_ISBN이_있으면_추가로_저장하지_않는다() {
        final var candidate = new NewBookCandidate(ISBN, TODAY.minusDays(1));
        final var detail = new BookDetail("신간", "저자", null, "출판사", "800", null);
        when(books.existsByIsbn13(ISBN)).thenReturn(false, true);
        final var useCase = new RegisterNewBookUseCase(books);

        assertThat(useCase.register(candidate, detail)).isTrue();
        assertThat(useCase.register(candidate, detail)).isFalse();

        final var savedBook = ArgumentCaptor.forClass(Book.class);
        verify(books).save(savedBook.capture());
        assertThat(savedBook.getValue().isbn13()).isEqualTo(ISBN);
        assertThat(savedBook.getValue().publishedAt()).isEqualTo(candidate.publishedAt());
    }
}
