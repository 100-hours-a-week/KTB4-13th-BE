package com.book.core.book.application.port;

import java.time.LocalDate;
import java.util.Optional;

public interface DataForLibraryBookDetailClient {
    Optional<BookDetail> findBookDetail(String isbn13);

    record BookDetail(String title, String author, String description, String publisher, String category, LocalDate publishedAt,
        String coverImageUrl) {
    }
}
