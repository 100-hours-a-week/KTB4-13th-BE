package com.book.core.coupon.domain;

import com.book.common.domain.BaseEntity;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
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

    @Column(name = "total_quantity", nullable = false)
    private int totalQuantity;

    @Column(name = "issued_quantity", nullable = false)
    private int issuedQuantity;

    @Column(name = "starts_at", nullable = false)
    private LocalDateTime startsAt;

    public Coupon(final Long id, final String name, final CouponType type, final BigDecimal discount, final long maxUseCount,
        final BigDecimal minOrderAmount, final LocalDateTime expiredAt, final int totalQuantity, final int issuedQuantity,
        final LocalDateTime startsAt) {
        super(id);
        this.name = name;
        this.type = type;
        this.discount = discount;
        this.maxUseCount = maxUseCount;
        this.minOrderAmount = minOrderAmount;
        if (totalQuantity < 0 || issuedQuantity < 0 || issuedQuantity > totalQuantity || startsAt == null || expiredAt == null) {
            throw new IllegalArgumentException("쿠폰 발급 수량과 발급 기간이 유효하지 않습니다.");
        }
        this.expiredAt = expiredAt;
        this.totalQuantity = totalQuantity;
        this.issuedQuantity = issuedQuantity;
        this.startsAt = startsAt;
    }

    public boolean isIssuableAt(final LocalDateTime now) {
        return !now.isBefore(startsAt) && now.isBefore(expiredAt);
    }

    public void issue(final LocalDateTime now) {
        if (!isIssuableAt(now)) {
            throw new CoreException(ErrorCode.COUPON_NOT_FOUND_OR_EXPIRED);
        }
        if (issuedQuantity >= totalQuantity) {
            throw new CoreException(ErrorCode.COUPON_ISSUE_LIMIT_EXCEEDED);
        }
        issuedQuantity++;
    }
}
