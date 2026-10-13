package com.book.core.coupon.domain;

import com.book.common.domain.BaseEntity;
import com.book.core.coupon.domain.enums.OwnedCouponStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Entity
@Table(name = "owned_coupon", indexes = @Index(name = "udx_owned_coupon", columnList = "user_id,coupon_id", unique = true))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Accessors(fluent = true)
public class OwnedCoupon extends BaseEntity {
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "coupon_id", nullable = false)
    private Long couponId;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 32)
    private OwnedCouponStatus status;

    @Column(name = "max_use_count", nullable = false)
    private long maxUseCount = 1L;

    @Column(name = "used_count", nullable = false)
    private long usedCount = 0L;

    public OwnedCoupon(final Long userId, final Long couponId, final OwnedCouponStatus status, final long maxUseCount,
        final long usedCount) {
        this.userId = userId;
        this.couponId = couponId;
        this.status = status;
        this.maxUseCount = maxUseCount;
        this.usedCount = usedCount;
    }

    public OwnedCoupon(final Long userId, final Long couponId, final OwnedCouponStatus status, final long maxUseCount) {
        this.userId = userId;
        this.couponId = couponId;
        this.status = status;
        this.maxUseCount = maxUseCount;
    }

    public void use() {
        usedCount += 1L;
        if (isFullyUsed()) {
            status = OwnedCouponStatus.USED;
        }
    }

    public void revert() {
        usedCount -= 1L;
        if (isUnused()) {
            status = OwnedCouponStatus.ISSUED;
        }
    }

    public boolean isUnused() {
        return usedCount == 0L;
    }

    public boolean isFullyUsed() {
        return (maxUseCount - usedCount) == 0L;
    }
}
