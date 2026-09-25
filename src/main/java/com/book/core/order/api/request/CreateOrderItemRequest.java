package com.book.core.order.api.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateOrderItemRequest(
        @Schema(description = "상품 ID", requiredMode = Schema.RequiredMode.REQUIRED) @NotNull @Positive
        Long itemId,

        @Schema(description = "주문 수량", requiredMode = Schema.RequiredMode.REQUIRED) @NotNull @Positive @Max(500)
        Integer quantity) {}
