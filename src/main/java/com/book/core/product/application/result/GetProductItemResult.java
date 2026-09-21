package com.book.core.product.application.result;

import com.book.core.product.domain.Product;
import java.math.BigDecimal;

public record GetProductItemResult(
        Long itemId,
        String itemName,
        String thumbnailUrl,
        String author,
        BigDecimal salePrice,
        BigDecimal discountedPrice,
        Long orderCount,
        Long reviewCount,
        BigDecimal reviewRate) {
    public static GetProductItemResult from(final Product product) {
        return new GetProductItemResult(
                product.id(),
                product.name(),
                product.thumbnailUrl(),
                product.book().author(),
                product.salePrice(),
                product.discountedPrice(),
                0L,
                0L,
                BigDecimal.ZERO);
    }
}
