package com.book.core.order.api.response;

import com.book.core.order.domain.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// @formatter:off
public record OrderDetailResponse(
        String orderKey,
        OrderStatus status,
        BigDecimal totalPrice,
        LocalDateTime createdAt,
        OrderAddressResponse address,
        OrderPaymentResponse payment,
        List<OrderItemResponse> items) {}
// @formatter:on
