package com.book.core.library.api.response;

import com.book.core.library.application.result.ShelfScanResult;
import java.util.List;

public record ShelfScanResponse(String photoType, List<Item> items) {
    public static ShelfScanResponse from(final ShelfScanResult result) {
        return new ShelfScanResponse(result.photoType(), result.items().stream().map(Item::from).toList());
    }

    public record Item(String state, String readText, Book book, List<Spine> spines) {
        private static Item from(final ShelfScanResult.Item item) {
            return new Item(item.state(), item.readText(), item.book() == null ? null : Book.from(item.book()),
                item.spines().stream().map(spine -> new Spine(spine.order(), spine.polygon())).toList());
        }
    }

    public record Book(Long bookId, String isbn, String title, String author, String publisher, Integer year, String coverUrl) {
        private static Book from(final ShelfScanResult.Book book) {
            return new Book(book.bookId(), book.isbn(), book.title(), book.author(), book.publisher(), book.year(), book.coverUrl());
        }
    }

    public record Spine(Integer order, List<List<Double>> polygon) {
    }
}
