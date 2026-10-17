package com.book.core.order.api.response;

import com.book.core.payment.domain.enums.PaymentMethod;
import java.math.BigDecimal;
import java.time.LocalDateTime;

// @formatter:off
public record OrderPaymentResponse(
        BigDecimal couponDiscount,
        BigDecimal usedPoint,
        BigDecimal paidAmount,
        PaymentMethod method,
        LocalDateTime paidAt,
        BigDecimal cancelledPaidAmount,
        BigDecimal cancellablePaidAmount) {}
// @formatter:on
