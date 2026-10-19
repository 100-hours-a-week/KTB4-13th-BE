package com.book.core.order.api.response;

import com.book.core.cancel.domain.enums.CancelType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// @formatter:off
public record OrderCancellationResponse(
        CancelType type,
        BigDecimal refundedAmount,
        LocalDateTime canceledAt,
        List<OrderCancellationItemResponse> items) {}
// @formatter:on
