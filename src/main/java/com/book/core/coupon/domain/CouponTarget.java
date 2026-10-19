package com.book.core.coupon.domain;

import com.book.common.domain.BaseEntity;
import com.book.core.coupon.domain.enums.CouponTargetType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Entity
@Table(name = "coupon_target")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Accessors(fluent = true)
public class CouponTarget extends BaseEntity {
    @Column(name = "coupon_id", nullable = false)
    private Long couponId;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 32)
    private CouponTargetType targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    public CouponTarget(final Long id, final Long couponId, final CouponTargetType targetType, final Long targetId) {
        super(id);
        this.couponId = couponId;
        this.targetType = targetType;
        this.targetId = targetId;
    }
}
