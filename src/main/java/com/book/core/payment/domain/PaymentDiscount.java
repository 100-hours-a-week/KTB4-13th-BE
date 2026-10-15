package com.book.core.payment.domain;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.coupon.domain.enums.CouponType;
import java.math.BigDecimal;
import java.util.List;
import lombok.Getter;
import lombok.experimental.Accessors;

/** 참고 결제의 쿠폰 및 포인트 계산을 보존합니다. */
@Getter
@Accessors(fluent = true)
public class PaymentDiscount {
    public static final PaymentDiscount EMPTY = new PaymentDiscount(List.of(), BigDecimal.ZERO, -1L, BigDecimal.ZERO);
    private final Long useOwnedCouponId;
    private final BigDecimal usePointAmount;
    private final CouponType couponType;
    private final BigDecimal couponDiscount;
    private final BigDecimal couponMinOrderAmount;
    private final BigDecimal usePoint;

    public PaymentDiscount(final List<PaymentCoupon> ownedCoupons, final BigDecimal pointBalance, final Long useOwnedCouponId,
        final BigDecimal usePointAmount) {
        this.useOwnedCouponId = useOwnedCouponId;
        this.usePointAmount = usePointAmount;
        if (useOwnedCouponId > 0) {
            final var ownedCoupon = ownedCoupons.stream().filter((final var item) -> item.id().equals(useOwnedCouponId)).findFirst()
                .orElseThrow(() -> new CoreException(ErrorCode.OWNED_COUPON_INVALID));
            couponType = ownedCoupon.type();
            couponDiscount = ownedCoupon.discount();
            couponMinOrderAmount = ownedCoupon.minOrderAmount();
        } else {
            couponType = CouponType.NONE;
            couponDiscount = BigDecimal.ZERO;
            couponMinOrderAmount = BigDecimal.ZERO;
        }
        if (usePointAmount.compareTo(BigDecimal.ZERO) > 0) {
            if (usePointAmount.compareTo(pointBalance) > 0) {
                throw new CoreException(ErrorCode.POINT_EXCEEDS_BALANCE);
            }
            usePoint = usePointAmount;
        } else {
            usePoint = BigDecimal.ZERO;
        }
    }

    /** 사용자 선택에 따라 정규화 전 usePointAmount를 빼는 참고 동작을 유지합니다. */
    public BigDecimal paidAmount(final BigDecimal orderPrice) {
        if (orderPrice.compareTo(couponMinOrderAmount) < 0) {
            throw new CoreException(ErrorCode.OWNED_COUPON_MIN_AMOUNT_NOT_REACHED);
        }
        final BigDecimal couponDiscountAmount = switch (couponType) {
            case NONE -> BigDecimal.ZERO;
            case FIXED_AMOUNT -> couponDiscount;
            case PERCENT_RATE -> orderPrice.multiply(couponDiscount);
        };
        final BigDecimal amount = orderPrice.subtract(couponDiscountAmount.add(usePointAmount));
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new CoreException(ErrorCode.PAYMENT_INVALID_AMOUNT);
        }
        return amount;
    }
}
