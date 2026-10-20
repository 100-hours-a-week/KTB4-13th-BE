package com.book.core.order.domain;

import com.book.common.domain.BaseTimeEntity;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.coupon.domain.enums.CouponType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Entity
@Table(name = "order_item")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Accessors(fluent = true)
public class OrderItem extends BaseTimeEntity {
    private static final int MIN_QUANTITY = 1;
    private static final int MAX_QUANTITY = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "item_name", length = 255)
    private String itemName;

    @Column(name = "thumbnail_url", length = 255)
    private String thumbnailUrl;

    @Column(length = 255)
    private String author;

    @Column(name = "sale_price", precision = 19, scale = 2)
    private BigDecimal salePrice;

    @Column(name = "unit_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "total_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalPrice;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "canceled_quantity", nullable = false)
    private long canceledQuantity = 0L;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "confirmation_reminder_sent_at")
    private LocalDateTime confirmationReminderSentAt;

    @Column(name = "owned_coupon_id", nullable = false)
    private Long ownedCouponId = -1L;

    @Enumerated(EnumType.STRING)
    @Column(name = "coupon_type", nullable = false, length = 32)
    private CouponType couponType = CouponType.NONE;

    @Column(name = "coupon_discount_value", nullable = false, precision = 38, scale = 2)
    private BigDecimal couponDiscountValue = BigDecimal.ZERO;

    @Column(name = "coupon_min_order_amount", nullable = false, precision = 38, scale = 2)
    private BigDecimal couponMinOrderAmount = BigDecimal.ZERO;

    @Column(name = "coupon_max_discount_amount", precision = 38, scale = 2)
    private BigDecimal couponMaxDiscountAmount;

    @Column(name = "used_point_amount", nullable = false, precision = 38, scale = 2)
    private BigDecimal usedPointAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private OrderItemStatus status;

    private OrderItem(final Order order, final Long productId, final String itemName, final String thumbnailUrl, final String author,
        final BigDecimal salePrice, final BigDecimal unitPrice, final Integer quantity) {
        if (order == null || productId == null || productId <= 0 || itemName == null || itemName.isBlank() || author == null
            || author.isBlank() || salePrice == null || salePrice.signum() < 0 || unitPrice == null || unitPrice.signum() < 0
            || quantity == null || !isQuantityInRange(quantity)) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        this.order = order;
        this.productId = productId;
        this.itemName = itemName;
        this.thumbnailUrl = thumbnailUrl;
        this.author = author;
        this.salePrice = salePrice;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.totalPrice = unitPrice.multiply(BigDecimal.valueOf(quantity));
        this.status = OrderItemStatus.CREATED;
    }

    public static boolean isQuantityInRange(final int quantity) {
        return quantity >= MIN_QUANTITY && quantity <= MAX_QUANTITY;
    }

    void paid(final LocalDateTime paidAt) {
        status = OrderItemStatus.PAID;
        this.paidAt = paidAt;
    }

    public void applyPaymentAllocation(final Long ownedCouponId, final CouponType couponType, final BigDecimal couponDiscountValue,
        final BigDecimal couponMinOrderAmount, final BigDecimal couponMaxDiscountAmount, final BigDecimal usedPointAmount) {
        if (status != OrderItemStatus.CREATED || ownedCouponId == null || couponType == null || couponDiscountValue == null
            || couponMinOrderAmount == null || usedPointAmount == null || usedPointAmount.signum() < 0
            || (ownedCouponId < 0 && couponType != CouponType.NONE) || (ownedCouponId > 0 && couponType == CouponType.NONE)) {
            throw new CoreException(ErrorCode.PAYMENT_INVALID_AMOUNT);
        }
        this.ownedCouponId = ownedCouponId;
        this.couponType = couponType;
        this.couponDiscountValue = couponDiscountValue;
        this.couponMinOrderAmount = couponMinOrderAmount;
        this.couponMaxDiscountAmount = couponMaxDiscountAmount;
        this.usedPointAmount = usedPointAmount;
    }

    public void confirm(final LocalDateTime confirmedAt) {
        if (confirmedAt == null || this.confirmedAt != null || isAllCanceled()
            || (status != OrderItemStatus.PAID && status != OrderItemStatus.PARTIAL_CANCELED)) {
            throw new CoreException(ErrorCode.ORDER_CANNOT_BE_CONFIRMED);
        }
        status = OrderItemStatus.CONFIRMED;
        this.confirmedAt = confirmedAt;
    }

    public boolean isConfirmationDue(final LocalDateTime now) {
        return isConfirmationEligible() && paidAt != null && !paidAt.plusDays(7).isAfter(now);
    }

    public boolean isConfirmationReminderDue(final LocalDateTime now) {
        return isConfirmationEligible() && paidAt != null && confirmationReminderSentAt == null && paidAt.plusDays(6).compareTo(now) <= 0
            && paidAt.plusDays(7).isAfter(now);
    }

    public boolean markConfirmationReminderSent(final LocalDateTime sentAt) {
        if (sentAt == null || !isConfirmationReminderDue(sentAt)) {
            return false;
        }
        confirmationReminderSentAt = sentAt;
        return true;
    }

    public BigDecimal couponDiscountForRemainingQuantity() {
        return couponDiscountForQuantity(cancellableQuantity());
    }

    public BigDecimal couponDiscountForQuantity(final long itemQuantity) {
        if (itemQuantity < 0 || itemQuantity > quantity) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        final BigDecimal amount = unitPrice.multiply(BigDecimal.valueOf(itemQuantity));
        if (ownedCouponId <= 0 || amount.compareTo(couponMinOrderAmount) < 0) {
            return BigDecimal.ZERO;
        }
        final BigDecimal discount = switch (couponType) {
            case NONE -> BigDecimal.ZERO;
            case FIXED_AMOUNT -> couponDiscountValue;
            case PERCENT_RATE -> amount.multiply(couponDiscountValue).setScale(0, RoundingMode.DOWN);
        };
        final BigDecimal cappedDiscount = couponMaxDiscountAmount == null ? discount : discount.min(couponMaxDiscountAmount);
        return cappedDiscount.min(amount).max(BigDecimal.ZERO);
    }

    public BigDecimal pointAllocationForNextCanceledUnits(final long cancelQuantity) {
        if (cancelQuantity <= 0 || canceledQuantity + cancelQuantity > quantity) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        return pointAllocationForRange(canceledQuantity, cancelQuantity);
    }

    public BigDecimal pointAllocationForRemainingUnits() {
        return pointAllocationForRange(canceledQuantity, cancellableQuantity());
    }

    public BigDecimal remainingGeneralPaymentAmount() {
        return generalPaymentAmountForQuantity(cancellableQuantity(), canceledQuantity);
    }

    public BigDecimal remainingGeneralPaymentAmountAfterCancel(final long cancelQuantity) {
        if (cancelQuantity <= 0 || canceledQuantity + cancelQuantity > quantity) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        final long remainingQuantity = cancellableQuantity() - cancelQuantity;
        return generalPaymentAmountForQuantity(remainingQuantity, canceledQuantity + cancelQuantity);
    }

    private BigDecimal generalPaymentAmountForQuantity(final long itemQuantity, final long pointAllocationStart) {
        final BigDecimal amount = unitPrice.multiply(BigDecimal.valueOf(itemQuantity)).subtract(couponDiscountForQuantity(itemQuantity))
            .subtract(pointAllocationForRange(pointAllocationStart, itemQuantity));
        if (amount.signum() < 0) {
            throw new CoreException(ErrorCode.POINT_DATA_INCONSISTENT);
        }
        return amount;
    }

    public boolean hasAppliedCoupon() {
        return ownedCouponId != null && ownedCouponId > 0;
    }

    private BigDecimal pointAllocationForRange(final long start, final long count) {
        if (usedPointAmount.signum() == 0 || count == 0) {
            return BigDecimal.ZERO;
        }
        final BigDecimal[] division = usedPointAmount.divideAndRemainder(BigDecimal.valueOf(quantity));
        final long largerUnitCount = division[1].longValueExact();
        final long largerUnitsInRange = Math.max(0L, Math.min(start + count, largerUnitCount) - start);
        return division[0].multiply(BigDecimal.valueOf(count)).add(BigDecimal.valueOf(largerUnitsInRange));
    }

    private boolean isConfirmationEligible() {
        return confirmedAt == null && !isAllCanceled() && (status == OrderItemStatus.PAID || status == OrderItemStatus.PARTIAL_CANCELED);
    }

    public void cancel(final long cancelQuantity) {
        if (status == OrderItemStatus.CONFIRMED || status == OrderItemStatus.CANCELED) {
            throw new CoreException(ErrorCode.ORDER_CANNOT_BE_CANCELED);
        }
        if (cancelQuantity <= 0 || this.canceledQuantity + cancelQuantity > this.quantity) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        this.canceledQuantity += cancelQuantity;
        if (isAllCanceled()) {
            status = OrderItemStatus.CANCELED;
        } else {
            status = OrderItemStatus.PARTIAL_CANCELED;
        }
    }

    public long cancellableQuantity() {
        return quantity - canceledQuantity;
    }

    void cancelRemaining() {
        final long remainingQuantity = cancellableQuantity();
        if (remainingQuantity > 0) {
            cancel(remainingQuantity);
        }
    }

    public boolean isAllCanceled() {
        return quantity.longValue() == canceledQuantity;
    }

    void cancel() {
        if (status != OrderItemStatus.CREATED) {
            throw new CoreException(ErrorCode.ORDER_CANNOT_BE_CANCELED);
        }
        status = OrderItemStatus.CANCELED;
    }

    static OrderItem created(final Order order, final Long productId, final String itemName, final String thumbnailUrl, final String author,
        final BigDecimal salePrice, final BigDecimal unitPrice, final Integer quantity) {
        return new OrderItem(order, productId, itemName, thumbnailUrl, author, salePrice, unitPrice, quantity);
    }
}
