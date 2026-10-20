package com.book.core.payment.domain;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.coupon.domain.enums.CouponType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PaymentDiscount {
    public static final PaymentDiscount EMPTY = new PaymentDiscount(List.of(), BigDecimal.ZERO, -1L, BigDecimal.ZERO);

    private final Long useOwnedCouponId;
    private final BigDecimal usePointAmount;
    private final CouponType couponType;
    private final BigDecimal couponDiscount;
    private final BigDecimal couponMinOrderAmount;
    private final BigDecimal usePoint;
    private final List<OrderItemPaymentAllocation> allocations;
    private final BigDecimal totalPaidAmount;

    /** 기존 단일 상품 결제 계약을 유지합니다. */
    public PaymentDiscount(final List<PaymentCoupon> ownedCoupons, final BigDecimal pointBalance, final Long useOwnedCouponId,
        final BigDecimal usePointAmount) {
        this.useOwnedCouponId = useOwnedCouponId;
        this.usePointAmount = usePointAmount;
        this.allocations = null;
        this.totalPaidAmount = null;
        if (useOwnedCouponId > 0) {
            final PaymentCoupon ownedCoupon = ownedCoupons.stream().filter((final PaymentCoupon item) -> item.id().equals(useOwnedCouponId))
                .findFirst().orElseThrow(() -> new CoreException(ErrorCode.OWNED_COUPON_INVALID));
            couponType = ownedCoupon.type();
            couponDiscount = ownedCoupon.discount();
            couponMinOrderAmount = ownedCoupon.minOrderAmount();
        } else {
            couponType = CouponType.NONE;
            couponDiscount = BigDecimal.ZERO;
            couponMinOrderAmount = BigDecimal.ZERO;
        }
        usePoint = usePointAmount.signum() > 0 ? validatePoints(pointBalance, usePointAmount) : BigDecimal.ZERO;
    }

    /** 주문상품별 쿠폰을 먼저 계산한 뒤 포인트를 비례 배분합니다. */
    public PaymentDiscount(final List<PaymentItemPrice> items, final List<PaymentCoupon> selectedCoupons, final BigDecimal pointBalance,
        final BigDecimal requestedPoints) {
        if (items.isEmpty()) {
            throw new CoreException(ErrorCode.PAYMENT_INVALID_AMOUNT);
        }
        if (selectedCoupons.size() > 1) {
            throw new CoreException(ErrorCode.PAYMENT_INVALID_AMOUNT);
        }
        final BigDecimal pointAmount = requestedPoints.signum() > 0 ? validatePoints(pointBalance, requestedPoints) : BigDecimal.ZERO;
        final var couponByItem = new java.util.HashMap<Long, PaymentCoupon>();
        final Set<Long> usedCouponIds = new HashSet<>();
        for (final PaymentCoupon coupon : selectedCoupons) {
            if (coupon.orderItemId() == null || coupon.id() == null || coupon.id() <= 0
                || couponByItem.putIfAbsent(coupon.orderItemId(), coupon) != null || !usedCouponIds.add(coupon.id())) {
                throw new CoreException(ErrorCode.PAYMENT_INVALID_AMOUNT);
            }
        }

        final List<BigDecimal> itemAmounts = new ArrayList<>(items.size());
        final List<PaymentCoupon> itemCoupons = new ArrayList<>(items.size());
        BigDecimal couponAmount = BigDecimal.ZERO;
        BigDecimal amountAfterCoupon = BigDecimal.ZERO;
        for (final PaymentItemPrice item : items) {
            final BigDecimal originalAmount = item.unitPrice().multiply(BigDecimal.valueOf(item.quantity()));
            final PaymentCoupon coupon = couponByItem.get(item.orderItemId());
            final BigDecimal discountAmount = coupon == null ? BigDecimal.ZERO : calculateCouponDiscount(coupon, originalAmount);
            itemAmounts.add(originalAmount.subtract(discountAmount));
            itemCoupons.add(coupon);
            couponAmount = couponAmount.add(discountAmount);
            amountAfterCoupon = amountAfterCoupon.add(originalAmount.subtract(discountAmount));
        }
        if (!couponByItem.keySet().stream().allMatch(id -> items.stream().anyMatch(item -> item.orderItemId().equals(id)))) {
            throw new CoreException(ErrorCode.OWNED_COUPON_INVALID);
        }
        if (pointAmount.compareTo(amountAfterCoupon) > 0) {
            throw new CoreException(ErrorCode.PAYMENT_INVALID_AMOUNT);
        }

        final List<BigDecimal> pointAllocations = allocatePoints(itemAmounts, pointAmount, amountAfterCoupon);
        final List<OrderItemPaymentAllocation> result = new ArrayList<>(items.size());
        for (int index = 0; index < items.size(); index++) {
            final PaymentItemPrice item = items.get(index);
            final PaymentCoupon coupon = itemCoupons.get(index);
            result.add(new OrderItemPaymentAllocation(item.orderItemId(), coupon == null ? -1L : coupon.id(),
                coupon == null ? CouponType.NONE : coupon.type(), coupon == null ? BigDecimal.ZERO : coupon.discount(),
                coupon == null ? BigDecimal.ZERO : coupon.minOrderAmount(), coupon == null ? null : coupon.maxDiscountAmount(),
                itemAmounts.get(index).subtract(item.unitPrice().multiply(BigDecimal.valueOf(item.quantity()))).negate(),
                pointAllocations.get(index)));
        }
        allocations = List.copyOf(result);
        this.useOwnedCouponId = selectedCoupons.isEmpty() ? -1L : selectedCoupons.getFirst().id();
        this.usePointAmount = pointAmount;
        couponType = usedCouponIds.isEmpty() ? CouponType.NONE : CouponType.FIXED_AMOUNT;
        couponDiscount = couponAmount;
        couponMinOrderAmount = BigDecimal.ZERO;
        usePoint = pointAmount;
        totalPaidAmount = amountAfterCoupon.subtract(pointAmount);
        if (totalPaidAmount.signum() < 0) {
            throw new CoreException(ErrorCode.PAYMENT_INVALID_AMOUNT);
        }
    }

    public Long useOwnedCouponId() {
        return useOwnedCouponId;
    }

    public BigDecimal usePointAmount() {
        return usePointAmount;
    }

    public CouponType couponType() {
        return couponType;
    }

    public BigDecimal couponDiscount() {
        return couponDiscount;
    }

    public BigDecimal couponMinOrderAmount() {
        return couponMinOrderAmount;
    }

    public BigDecimal usePoint() {
        return usePoint;
    }

    public List<OrderItemPaymentAllocation> allocations() {
        return allocations == null ? List.of() : allocations;
    }

    public BigDecimal paidAmount(final BigDecimal orderPrice) {
        if (totalPaidAmount != null) {
            return totalPaidAmount;
        }
        if (orderPrice.compareTo(couponMinOrderAmount) < 0) {
            throw new CoreException(ErrorCode.OWNED_COUPON_MIN_AMOUNT_NOT_REACHED);
        }
        final BigDecimal couponDiscountAmount = switch (couponType) {
            case NONE -> BigDecimal.ZERO;
            case FIXED_AMOUNT -> couponDiscount;
            case PERCENT_RATE -> orderPrice.multiply(couponDiscount);
        };
        // NOTE: 구버전 단일상품 결제 생성자의 -1 입력 계산 계약을 유지합니다.
        final BigDecimal amount = orderPrice.subtract(couponDiscountAmount.add(usePointAmount));
        if (amount.signum() < 0) {
            throw new CoreException(ErrorCode.PAYMENT_INVALID_AMOUNT);
        }
        return amount;
    }

    private static BigDecimal validatePoints(final BigDecimal pointBalance, final BigDecimal points) {
        if (points.compareTo(pointBalance) > 0) {
            throw new CoreException(ErrorCode.POINT_EXCEEDS_BALANCE);
        }
        if (points.stripTrailingZeros().scale() > 0 || points.remainder(BigDecimal.TEN).signum() != 0) {
            throw new CoreException(ErrorCode.PAYMENT_INVALID_AMOUNT);
        }
        return points;
    }

    private static BigDecimal calculateCouponDiscount(final PaymentCoupon coupon, final BigDecimal itemAmount) {
        if (coupon.type() == CouponType.NONE || itemAmount.compareTo(coupon.minOrderAmount()) < 0) {
            throw new CoreException(ErrorCode.OWNED_COUPON_MIN_AMOUNT_NOT_REACHED);
        }
        final BigDecimal discount = switch (coupon.type()) {
            case NONE -> BigDecimal.ZERO;
            case FIXED_AMOUNT -> coupon.discount();
            case PERCENT_RATE -> itemAmount.multiply(coupon.discount()).setScale(0, RoundingMode.DOWN);
        };
        final BigDecimal capped = coupon.maxDiscountAmount() == null ? discount : discount.min(coupon.maxDiscountAmount());
        return capped.min(itemAmount).max(BigDecimal.ZERO);
    }

    private static List<BigDecimal> allocatePoints(final List<BigDecimal> amounts, final BigDecimal points, final BigDecimal totalAmount) {
        final List<BigDecimal> allocations = new ArrayList<>(amounts.size());
        final List<Remainder> remainders = new ArrayList<>(amounts.size());
        BigDecimal allocated = BigDecimal.ZERO;
        for (int index = 0; index < amounts.size(); index++) {
            final BigDecimal raw = totalAmount.signum() == 0 ? BigDecimal.ZERO
                : points.multiply(amounts.get(index)).divide(totalAmount, 24, RoundingMode.DOWN);
            final BigDecimal floor = raw.setScale(0, RoundingMode.DOWN);
            allocations.add(floor);
            allocated = allocated.add(floor);
            remainders.add(new Remainder(index, raw.subtract(floor)));
        }
        int remaining = points.subtract(allocated).intValueExact();
        remainders.sort(Comparator.comparing(Remainder::fraction).reversed().thenComparingInt(Remainder::index));
        for (int index = 0; index < remaining; index++) {
            final int target = remainders.get(index).index();
            allocations.set(target, allocations.get(target).add(BigDecimal.ONE));
        }
        return allocations;
    }

    private record Remainder(int index, BigDecimal fraction) {
    }
}
