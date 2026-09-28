package com.book.core.order.api.response;

import java.util.List;

// @formatter:off
public record OrderListResponse(
        List<OrderSummaryResponse> orders,
        String nextCursor) {}
// @formatter:on
