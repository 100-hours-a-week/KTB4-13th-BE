package com.book.core.order.api.response;

import com.book.core.order.domain.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

// @formatter:off
public record OrderSummaryResponse(
        String orderKey,
        String representativeBookName,
        int itemCount,
        String thumbnailUrl,
        BigDecimal totalPrice,
        OrderStatus status,
        LocalDateTime paidAt) {}
// @formatter:on
