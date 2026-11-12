package com.book.core.book.infrastructure.client;

import com.book.core.book.application.result.BookDetail;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Optional;

final class DataForLibraryBookDetailExternalMapper {
    private DataForLibraryBookDetailExternalMapper() {}

    static Optional<BookDetail> toBookDetail(final DataForLibraryExternalBook book) {
        if (!required(book.title()) || !required(book.author()) || !required(book.publisher()) || !required(book.category())
            || book.publishedAt() == null) {
            return Optional.empty();
        }
        try {
            final LocalDate publishedAt = LocalDate.parse(book.publishedAt().strip());
            String cover = null;
            if (required(book.cover())) {
                cover = book.cover().strip();
            }
            String description = null;
            if (book.description() != null && !book.description().isBlank()
                && book.description().getBytes(StandardCharsets.UTF_8).length <= 65535) {
                description = book.description();
            }
            return Optional.of(new BookDetail(book.title().strip(), book.author().strip(), description, book.publisher().strip(),
                book.category().strip(), publishedAt, cover));
        } catch (final DateTimeParseException exception) {
            return Optional.empty();
        }
    }

    private static boolean required(final String value) {
        return value != null && !value.isBlank() && value.strip().length() <= 255;
    }
}
