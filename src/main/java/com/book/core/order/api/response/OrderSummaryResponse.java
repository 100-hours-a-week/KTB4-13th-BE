package com.book.core.order.api.response;

import com.book.core.order.domain.OrderStatus;
import java.math.BigDecimal;

// @formatter:off
public record OrderSummaryResponse(
        String key,
        String name,
        BigDecimal totalPrice,
        OrderStatus status) {}
// @formatter:on
