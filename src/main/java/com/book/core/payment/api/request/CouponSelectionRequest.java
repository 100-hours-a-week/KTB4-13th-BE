package com.book.core.payment.api.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CouponSelectionRequest(@NotNull @Positive Long orderItemId, @NotNull @Positive Long ownedCouponId) {
}
