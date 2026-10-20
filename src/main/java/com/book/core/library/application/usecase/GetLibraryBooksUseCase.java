package com.book.core.library.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.book.application.usecase.GetBooksUseCase;
import com.book.core.book.domain.Book;
import com.book.core.library.application.port.UserLibraryBookRepositoryPort;
import com.book.core.library.application.result.LibraryBookResult;
import com.book.core.library.domain.UserLibraryBook;
import com.book.core.product.application.usecase.FindProductIdsByBookIdsUseCase;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class GetLibraryBooksUseCase {
    private final UserLibraryBookRepositoryPort libraryBookRepository;
    private final GetBooksUseCase getBooksUseCase;
    private final FindProductIdsByBookIdsUseCase findProductIdsByBookIdsUseCase;

    /** 최근에 추가한 순서로 반환합니다. 도서·상품은 목록 단위로 한 번씩 조회하며, 판매 중인 상품이 없으면 productId는 null입니다. */
    @Transactional(readOnly = true)
    public List<LibraryBookResult> execute(final Long userId) {
        final List<UserLibraryBook> libraryBooks = libraryBookRepository.findActiveByUserId(userId).stream()
            .sorted(Comparator.comparing(UserLibraryBook::addedAt).thenComparing(UserLibraryBook::id).reversed()).toList();
        if (libraryBooks.isEmpty()) {
            return List.of();
        }

        final List<Long> bookIds = libraryBooks.stream().map(UserLibraryBook::bookId).toList();
        final Map<Long, Book> booksById =
            getBooksUseCase.execute(bookIds).stream().collect(Collectors.toMap(Book::id, Function.identity()));
        final Map<Long, Long> productIdsByBookId = findProductIdsByBookIdsUseCase.execute(bookIds);

        return libraryBooks.stream().filter(libraryBook -> booksById.containsKey(libraryBook.bookId()))
            .map(libraryBook -> toResult(libraryBook, booksById.get(libraryBook.bookId()), productIdsByBookId.get(libraryBook.bookId())))
            .toList();
    }

    private LibraryBookResult toResult(final UserLibraryBook libraryBook, final Book book, final Long productId) {
        return new LibraryBookResult(libraryBook.id(), book.id(), productId, book.title(), book.author(), book.coverImageUrl(),
            libraryBook.addedAt());
    }
}
