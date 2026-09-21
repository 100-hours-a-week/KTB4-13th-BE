package com.book.core.product.api.response;

import com.book.core.product.application.result.GetProductDetailResult;
import java.math.BigDecimal;
import java.util.List;

public record ProductDetailResponse(
        Long productId,
        String itemName,
        String thumbnailUrl,
        String author,
        String publisher,
        String publishedAt,
        BigDecimal salePrice,
        BigDecimal discountedPrice,
        Long reviewCount,
        BigDecimal reviewRate,
        Integer stockQuantity,
        List<ProductCouponResponse> coupons) {
    public static ProductDetailResponse from(final GetProductDetailResult result) {
        final var coupons =
                result.coupons().stream().map(ProductCouponResponse::from).toList();
        return new ProductDetailResponse(
                result.productId(),
                result.itemName(),
                result.thumbnailUrl(),
                result.author(),
                result.publisher(),
                result.publishedAt().toString(),
                result.salePrice(),
                result.discountedPrice(),
                result.reviewCount(),
                result.reviewRate(),
                result.stockQuantity(),
                coupons);
    }
}
