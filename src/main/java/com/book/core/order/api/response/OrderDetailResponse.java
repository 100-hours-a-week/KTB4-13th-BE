package com.book.core.order.api.response;

import com.book.core.order.domain.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// @formatter:off
public record OrderDetailResponse(
        String key,
        String name,
        BigDecimal totalPrice,
        OrderStatus status,
        List<OrderItemResponse> items,
        OrderAddressResponse address,
        LocalDateTime createdAt) {}
// @formatter:on
