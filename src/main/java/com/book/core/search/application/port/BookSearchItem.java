package com.book.core.search.application.port;

import java.math.BigDecimal;

public record BookSearchItem(Long bookId, String title, String author, String publisher, BigDecimal price, boolean inStock,
    String coverUrl) {
}
