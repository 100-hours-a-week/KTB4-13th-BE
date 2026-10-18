package com.book.core.product.api.response;

import com.book.core.product.application.result.GetProductDetailResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

public record ProductDetailResponse(Long productId, String itemName, String thumbnailUrl, String author, String publisher,
    String publishedAt, String category, String description, BigDecimal salePrice, BigDecimal discountedPrice, Long reviewCount,
    BigDecimal reviewRate, Integer stockQuantity,
    @Schema(description = "상품 또는 카테고리에 연결된 활성 쿠폰 ID 목록. 만료 쿠폰도 포함합니다.") List<Long> couponIds) {
    public static ProductDetailResponse from(final GetProductDetailResult result) {
        return new ProductDetailResponse(result.productId(), result.itemName(), result.thumbnailUrl(), result.author(), result.publisher(),
            result.publishedAt().toString(), result.category(), result.description(), result.salePrice(), result.discountedPrice(),
            result.reviewCount(), result.reviewRate(), result.stockQuantity(), result.couponIds());
    }
}
