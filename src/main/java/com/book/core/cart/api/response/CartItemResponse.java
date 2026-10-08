package com.book.core.cart.api.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

// @formatter:off
public record CartItemResponse(
        Long cartItemId,
        Long productId,
        @Schema(description = "상품 행이 없는 장바구니 항목이면 null", nullable = true) String itemName,
        @Schema(description = "상품 표지가 없거나 상품 행이 없으면 null", nullable = true) String thumbnailUrl,
        @Schema(description = "상품 또는 연결 도서가 삭제되었거나 상품 행이 없으면 null", nullable = true) BigDecimal salePrice,
        @Schema(description = "상품 또는 연결 도서가 삭제되었거나 상품 행이 없으면 null", nullable = true) BigDecimal discountedPrice,
        Integer quantity,
        @Schema(description = "상품과 연결 도서가 활성이고 재고가 수량 이상이면 true") boolean isAvailableForPurchase) {}
// @formatter:on
