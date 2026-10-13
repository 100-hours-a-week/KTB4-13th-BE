package com.book.core.search.application.result;

import java.math.BigDecimal;

public record SearchBookItemResult(Long bookId, Long productId, String title, String author, String publisher, BigDecimal price,
    boolean inStock, String coverUrl) {
}
