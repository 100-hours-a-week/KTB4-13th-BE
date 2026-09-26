package com.book.core.order.api.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

// @formatter:off
public record CreateOrderResponse(
    @Schema(description = "생성된 주문 키") String orderKey,
    @Schema(description = "처리 결과", allowableValues = {"ORDER_CREATED", "NO_ADDRESS"}) String status,
    @Schema(description = "할인 적용 후 총 주문 금액") BigDecimal totalPrice,
    @Schema(description = "상품별 주문 항목") List<CreateOrderItemResponse> items) {

    public CreateOrderResponse {
        items = List.copyOf(items);
    }

    public static CreateOrderResponse created(
        final String orderKey,
        final BigDecimal totalPrice,
        final List<CreateOrderItemResponse> items) {
        return new CreateOrderResponse(orderKey, "ORDER_CREATED", totalPrice, items);
    }

    public static CreateOrderResponse noAddress(
        final String orderKey,
        final BigDecimal totalPrice,
        final List<CreateOrderItemResponse> items) {
        return new CreateOrderResponse(orderKey, "NO_ADDRESS", totalPrice, items);
    }
}
// @formatter:on
