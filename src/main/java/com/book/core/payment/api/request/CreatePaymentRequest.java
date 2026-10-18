package com.book.core.payment.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreatePaymentRequest(@NotBlank @Size(max = 255) String orderKey, Long useOwnedCouponId, BigDecimal usePoint) {
}
