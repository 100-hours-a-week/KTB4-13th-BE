package com.book.core.order.api.response;

import io.swagger.v3.oas.annotations.media.Schema;
// @formatter:off
public record CreateOrderResponse(
    @Schema(description = "생성된 주문 키") String orderKey) {}
// @formatter:on
