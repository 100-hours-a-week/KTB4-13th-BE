package com.book.core.product.application.result;

import com.book.core.product.domain.Product;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record GetProductDetailResult(Long productId, String itemName, String thumbnailUrl, String author, String publisher,
    LocalDate publishedAt, String category, String description, BigDecimal salePrice, BigDecimal discountedPrice, Long reviewCount,
    BigDecimal reviewRate, Integer stockQuantity, List<Long> couponIds) {
    public static GetProductDetailResult from(final Product product) {
        final var book = product.book();
        return new GetProductDetailResult(product.id(), product.name(), product.thumbnailUrl(), book.author(), book.publisher(),
            book.publishedAt(), book.category(), book.description(), product.salePrice(), product.discountedPrice(),
            // shortcut: 리뷰 집계는 아직 연결하지 않았으며, 상품 상세에 집계를 연결할 때 교체한다.
            0L, BigDecimal.ZERO, product.stockQuantity(), List.of());
    }

    public GetProductDetailResult withStockQuantity(final int availableQuantity) {
        return new GetProductDetailResult(productId, itemName, thumbnailUrl, author, publisher, publishedAt, category, description,
            salePrice, discountedPrice, reviewCount, reviewRate, availableQuantity, couponIds);
    }

    public GetProductDetailResult withCouponIds(final List<Long> couponIds) {
        return new GetProductDetailResult(productId, itemName, thumbnailUrl, author, publisher, publishedAt, category, description,
            salePrice, discountedPrice, reviewCount, reviewRate, stockQuantity, couponIds);
    }
}
