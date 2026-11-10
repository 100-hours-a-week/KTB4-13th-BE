package com.book.core.library.application.result;

import java.util.List;
import java.util.Map;

public record ShelfScanResult(String photoType, List<Item> items) {
    /** BE 도서로 연결되지 않은 ISBN의 책은 bookId가 null이며 나의 도서관에 담을 수 없습니다. */
    public ShelfScanResult withBookIds(final Map<String, Long> bookIdsByIsbn) {
        return new ShelfScanResult(photoType,
            items.stream()
                .map(item -> new Item(item.state(), item.readText(),
                    item.book() == null ? null
                        : item.book().withBookId(item.book().isbn() == null ? null : bookIdsByIsbn.get(item.book().isbn())),
                    item.spines()))
                .toList());
    }

    public record Item(String state, String readText, Book book, List<Spine> spines) {
    }

    public record Book(Long bookId, String isbn, String title, String author, String publisher, Integer year, String coverUrl) {
        private Book withBookId(final Long bookId) {
            return new Book(bookId, isbn, title, author, publisher, year, coverUrl);
        }
    }

    public record Spine(Integer order, List<List<Double>> polygon) {
    }
}
