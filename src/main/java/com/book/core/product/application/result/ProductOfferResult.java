package com.book.core.product.application.result;

import java.math.BigDecimal;

public record ProductOfferResult(Long productId, String isbn, String title, BigDecimal price, int stockQuantity, boolean active) {
}
