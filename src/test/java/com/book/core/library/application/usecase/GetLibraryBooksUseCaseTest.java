package com.book.core.library.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.book.core.book.application.usecase.GetBooksUseCase;
import com.book.core.book.domain.Book;
import com.book.core.library.application.port.UserLibraryBookRepositoryPort;
import com.book.core.library.application.result.LibraryBookResult;
import com.book.core.library.domain.UserLibraryBook;
import com.book.core.product.application.usecase.FindProductIdsByBookIdsUseCase;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GetLibraryBooksUseCaseTest {
    private static final Long USER_ID = 17L;
    private static final LocalDateTime OLDER_ADDED_AT = LocalDateTime.of(2026, 10, 1, 9, 0);
    private static final LocalDateTime NEWER_ADDED_AT = LocalDateTime.of(2026, 10, 2, 9, 0);
    private final UserLibraryBookRepositoryPort libraryBookRepository = mock(UserLibraryBookRepositoryPort.class);
    private final GetBooksUseCase getBooksUseCase = mock(GetBooksUseCase.class);
    private final FindProductIdsByBookIdsUseCase findProductIdsByBookIdsUseCase = mock(FindProductIdsByBookIdsUseCase.class);
    private final GetLibraryBooksUseCase useCase =
        new GetLibraryBooksUseCase(libraryBookRepository, getBooksUseCase, findProductIdsByBookIdsUseCase);

    @Test
    void 활성_도서가_없으면_빈_목록을_반환한다() {
        when(libraryBookRepository.findActiveByUserId(USER_ID)).thenReturn(List.of());

        assertThat(useCase.execute(USER_ID)).isEmpty();

        verifyNoInteractions(getBooksUseCase, findProductIdsByBookIdsUseCase);
    }

    @Test
    void 활성_도서를_최근_추가_순으로_상품과_함께_반환하고_도서가_없으면_제외한다() {
        final UserLibraryBook older = libraryBook(10L, 100L, OLDER_ADDED_AT);
        final UserLibraryBook newerLowerId = libraryBook(20L, 200L, NEWER_ADDED_AT);
        final UserLibraryBook newerHigherId = libraryBook(21L, 300L, NEWER_ADDED_AT);
        final UserLibraryBook missingBook = libraryBook(22L, 400L, NEWER_ADDED_AT.plusDays(1));
        when(libraryBookRepository.findActiveByUserId(USER_ID)).thenReturn(List.of(older, newerLowerId, missingBook, newerHigherId));
        when(getBooksUseCase.execute(List.of(400L, 300L, 200L, 100L)))
            .thenReturn(List.of(book(100L, "오래된 책"), book(200L, "최근 책 A"), book(300L, "최근 책 B")));
        when(findProductIdsByBookIdsUseCase.execute(List.of(400L, 300L, 200L, 100L))).thenReturn(Map.of(100L, 9100L, 300L, 9300L));

        final List<LibraryBookResult> results = useCase.execute(USER_ID);

        assertThat(results).containsExactly(new LibraryBookResult(21L, 300L, 9300L, "최근 책 B", "작가", "cover-300.jpg", NEWER_ADDED_AT),
            new LibraryBookResult(20L, 200L, null, "최근 책 A", "작가", "cover-200.jpg", NEWER_ADDED_AT),
            new LibraryBookResult(10L, 100L, 9100L, "오래된 책", "작가", "cover-100.jpg", OLDER_ADDED_AT));
    }

    private UserLibraryBook libraryBook(final Long id, final Long bookId, final LocalDateTime addedAt) {
        return new UserLibraryBook(id, USER_ID, bookId, addedAt);
    }

    private Book book(final Long id, final String title) {
        return new Book(id, null, null, title, "작가", null, "출판사", "소설", LocalDate.of(2026, 1, 1), "cover-" + id + ".jpg");
    }
}
