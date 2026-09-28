package com.book.core.order.application.result;

import java.util.List;

// @formatter:off
public record GetOrdersResult(List<OrderSummaryResult> orders, String nextCursor) {
    public static GetOrdersResult of(final List<OrderSummaryResult> orders, final String nextCursor) {
        return new GetOrdersResult(orders, nextCursor);
    }
}
// @formatter:on
