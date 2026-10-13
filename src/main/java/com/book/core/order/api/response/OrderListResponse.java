package com.book.core.order.api.response;

import java.util.List;

public record OrderListResponse(List<OrderSummaryResponse> orders, String nextCursor) {
}
