package com.book.core.order.api.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

// @formatter:off
public record CreateOrderRequest(
        @Schema(description = "장바구니에 담긴 상품과 주문 수량", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotEmpty List<@Valid CreateOrderItemRequest> items) {}
// @formatter:on
