package com.book.core.payment.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public record CreatePaymentRequest(@NotBlank @Size(max = 255) String orderKey, Long useOwnedCouponId, BigDecimal usePoint,
    @Size(max = 1) List<@jakarta.validation.Valid CouponSelectionRequest> couponSelections) {
    public CreatePaymentRequest(final String orderKey, final Long useOwnedCouponId, final BigDecimal usePoint) {
        this(orderKey, useOwnedCouponId, usePoint, List.of());
    }
}
