package com.book.core.order.api.response;

import com.book.core.order.application.result.CreateOrderItemResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

// @formatter:off
public record CreateOrderItemResponse(
    @Schema(description = "상품 ID") Long productId,
    @Schema(description = "상품명") String itemName,
    @Schema(description = "상품 썸네일 URL") String thumbnailUrl,
    @Schema(description = "저자") String author,
    @Schema(description = "할인 전 단가") BigDecimal salePrice,
    @Schema(description = "할인 적용 단가") BigDecimal discountedPrice,
    @Schema(description = "주문 수량") Integer quantity,
    @Schema(description = "할인 적용 단가 기준 소계") BigDecimal totalPrice) {

    public static CreateOrderItemResponse from(final CreateOrderItemResult result) {
        return new CreateOrderItemResponse(
            result.productId(),
            result.itemName(),
            result.thumbnailUrl(),
            result.author(),
            result.salePrice(),
            result.discountedPrice(),
            result.quantity(),
            result.totalPrice());
    }
}
// @formatter:on
