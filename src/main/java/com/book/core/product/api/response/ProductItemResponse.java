package com.book.core.product.api.response;

import com.book.core.product.application.result.GetProductItemResult;
import java.math.BigDecimal;

public record ProductItemResponse(
        Long itemId,
        String itemName,
        String thumbnailUrl,
        String author,
        BigDecimal salePrice,
        BigDecimal discountedPrice,
        Long orderCount,
        Long reviewCount,
        BigDecimal reviewRate) {
    public static ProductItemResponse from(final GetProductItemResult result) {
        return new ProductItemResponse(
                result.itemId(),
                result.itemName(),
                result.thumbnailUrl(),
                result.author(),
                result.salePrice(),
                result.discountedPrice(),
                result.orderCount(),
                result.reviewCount(),
                result.reviewRate());
    }
}
