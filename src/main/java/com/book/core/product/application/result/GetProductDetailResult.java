package com.book.core.product.application.result;

import com.book.core.product.domain.Product;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

// @formatter:off
public record GetProductDetailResult(
        Long productId,
        String itemName,
        String thumbnailUrl,
        String author,
        String publisher,
        LocalDate publishedAt,
        String category,
        String description,
        BigDecimal salePrice,
        BigDecimal discountedPrice,
        Long reviewCount,
        BigDecimal reviewRate,
        Integer stockQuantity,
        List<GetProductCouponResult> coupons) {
    public static GetProductDetailResult from(final Product product) {
        final var book = product.book();
        return new GetProductDetailResult(
                product.id(),
                product.name(),
                product.thumbnailUrl(),
                book.author(),
                book.publisher(),
                book.publishedAt(),
                book.category(),
                book.description(),
                product.salePrice(),
                product.discountedPrice(),
                // ponytail: review/coupon persistence is deferred until their public ownership rules are defined.
                0L,
                BigDecimal.ZERO,
                product.stockQuantity(),
                List.of());
    }
}
// @formatter:on
