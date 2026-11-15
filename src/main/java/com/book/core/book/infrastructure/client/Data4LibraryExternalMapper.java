package com.book.core.book.infrastructure.client;

import com.book.core.book.application.result.BookDetail;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import tools.jackson.databind.JsonNode;

final class Data4LibraryExternalMapper {
    private Data4LibraryExternalMapper() {}

    static Optional<BookDetail> toBookDetail(final JsonNode book) {
        final String title = text(book, "bookname");
        final String author = text(book, "authors");
        final String publisher = text(book, "publisher");
        final String category = text(book, "class_no");
        final String publishedAt = text(book, "publication_date");
        if (!required(title) || !required(author) || !required(publisher) || !required(category) || publishedAt == null) {
            return Optional.empty();
        }
        try {
            final LocalDate publishedDate = LocalDate.parse(publishedAt.strip());
            final String coverValue = text(book, "bookImageURL");
            String cover = null;
            if (required(coverValue)) {
                cover = coverValue.strip();
            }
            final String descriptionValue = text(book, "description");
            String description = null;
            if (descriptionValue != null && !descriptionValue.isBlank()
                && descriptionValue.getBytes(StandardCharsets.UTF_8).length <= 65535) {
                description = descriptionValue;
            }
            return Optional.of(new BookDetail(title.strip(), author.strip(), description, publisher.strip(), category.strip(),
                publishedDate, cover));
        } catch (final DateTimeParseException exception) {
            return Optional.empty();
        }
    }

    static String text(final JsonNode node, final String field) {
        final JsonNode value = node.path(field);
        if (!value.isValueNode() || value.isNull()) {
            return null;
        }
        return value.asString();
    }

    private static boolean required(final String value) {
        return value != null && !value.isBlank() && value.strip().length() <= 255;
    }
}
