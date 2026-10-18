package com.book.core.coupon.domain;

import com.book.common.domain.BaseEntity;
import com.book.core.coupon.domain.enums.CouponType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Entity
@Table(name = "coupon")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Accessors(fluent = true)
public class Coupon extends BaseEntity {
    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private CouponType type;

    @Column(nullable = false, precision = 38, scale = 2)
    private BigDecimal discount;

    @Column(name = "max_use_count", nullable = false)
    private long maxUseCount;

    @Column(name = "min_order_amount", nullable = false, precision = 38, scale = 2)
    private BigDecimal minOrderAmount;

    @Column(name = "expired_at", nullable = false)
    private LocalDateTime expiredAt;

    public Coupon(final Long id, final String name, final CouponType type, final BigDecimal discount, final long maxUseCount,
        final BigDecimal minOrderAmount, final LocalDateTime expiredAt) {
        super(id);
        this.name = name;
        this.type = type;
        this.discount = discount;
        this.maxUseCount = maxUseCount;
        this.minOrderAmount = minOrderAmount;
        this.expiredAt = expiredAt;
    }
}
