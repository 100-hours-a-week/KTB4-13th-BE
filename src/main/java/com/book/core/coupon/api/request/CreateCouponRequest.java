package com.book.core.coupon.api.request;

import com.book.core.coupon.domain.enums.CouponType;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CreateCouponRequest(
    @NotBlank @Size(max = 255) String name,
    @NotNull CouponType type,
    @NotNull @PositiveOrZero @Digits(integer = 36, fraction = 2) BigDecimal discount,
    @NotNull @Positive Long maxUseCount,
    @NotNull @PositiveOrZero @Digits(integer = 36, fraction = 2) BigDecimal minOrderAmount,
    @NotNull @Positive Integer totalQuantity,
    @NotNull LocalDateTime startsAt,
    @NotNull LocalDateTime expiredAt
) {
}
