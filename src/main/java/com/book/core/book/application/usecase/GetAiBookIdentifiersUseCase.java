package com.book.core.book.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.book.application.port.BookRepositoryPort;
import com.book.core.book.domain.Book;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class GetAiBookIdentifiersUseCase {
    private static final Pattern ISBN13 = Pattern.compile("[0-9]{13}");

    private final BookRepositoryPort books;

    public List<String> findIsbns(final List<Long> bookIds) {
        final List<Long> ids = bookIds.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return List.of();
        }
        final Map<Long, String> isbnsById = books.findWithUnambiguousIsbnByIds(ids).stream()
            .filter((final Book book) -> isIsbn13(book.isbn13())).collect(Collectors.toMap(Book::id, Book::isbn13));
        return ids.stream().map(isbnsById::get).filter(Objects::nonNull).toList();
    }

    public Map<String, Long> findBookIds(final List<String> isbns) {
        final Map<String, Long> found =
            findUnambiguousBooks(isbns).stream().filter(Book::isActive).collect(Collectors.toMap(Book::isbn13, Book::id));
        final long omitted = isbns.stream().filter((final String isbn) -> isbn == null || !found.containsKey(isbn)).count();
        if (omitted > 0) {
            log.warn("[AI_CATALOG] 건너뛴 응답 도서 수={}", omitted);
        }
        return found;
    }

    public Set<String> findUnambiguousIsbns(final List<String> isbns) {
        return findUnambiguousBooks(isbns).stream().map(Book::isbn13).collect(Collectors.toUnmodifiableSet());
    }

    private List<Book> findUnambiguousBooks(final List<String> isbns) {
        final List<String> valid = isbns.stream().filter(GetAiBookIdentifiersUseCase::isIsbn13).distinct().toList();
        if (valid.isEmpty()) {
            return List.of();
        }
        return books.findWithUnambiguousIsbnByIsbns(valid).stream().filter((final Book book) -> isIsbn13(book.isbn13())).toList();
    }

    public static boolean isIsbn13(final String value) {
        return value != null && ISBN13.matcher(value).matches();
    }
}
