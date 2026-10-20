package com.book.core.library.application.result;

import java.time.LocalDateTime;

public record LibraryBookResult(Long libraryBookId, Long bookId, Long productId, String title, String author, String coverImageUrl,
    LocalDateTime addedAt) {
}
