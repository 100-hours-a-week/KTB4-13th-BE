package com.book.core.book.infrastructure.client;

import com.book.core.book.application.result.BookDetail;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import tools.jackson.databind.JsonNode;

final class JeongboNaruExternalMapper {
    private JeongboNaruExternalMapper() {}

    static Optional<BookDetail> toBookDetail(final JsonNode book) {
        final String title = text(book, "bookname");
        final String author = text(book, "authors");
        final String publisher = text(book, "publisher");
        final String category = text(book, "class_no");
        // 정보나루 publication_date는 출판 연도만 주므로 출간일은 국중도 목록 값을 사용합니다.
        if (!required(title) || !required(author) || !required(publisher) || !required(category)) {
            return Optional.empty();
        }
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
        return Optional.of(new BookDetail(title.strip(), author.strip(), description, publisher.strip(), category.strip(), cover));
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
