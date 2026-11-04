package com.book.core.point.domain;

import com.book.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Entity
@Table(name = "point_balance")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Accessors(fluent = true)
public class PointBalance extends BaseEntity {
    private Long userId;
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "reserved_amount", nullable = false)
    private BigDecimal reserved = BigDecimal.ZERO;

    @Version
    private long version = 0L;

    public PointBalance(final Long userId) {
        this(null, userId, BigDecimal.ZERO);
    }

    public PointBalance(final Long id, final Long userId, final BigDecimal balance) {
        super(id);
        PointAmount.requireWhole(balance);
        this.userId = userId;
        this.balance = balance;
    }

    public BigDecimal available() {
        return balance.subtract(reserved).max(BigDecimal.ZERO);
    }

    public void reserve(final BigDecimal amount) {
        PointAmount.requireWhole(amount);
        if (amount.signum() < 0 || available().compareTo(amount) < 0) {
            throw new com.book.common.exception.CoreException(com.book.common.exception.ErrorCode.POINT_EXCEEDS_BALANCE);
        }
        reserved = reserved.add(amount);
    }

    public void release(final BigDecimal amount) {
        PointAmount.requireWhole(amount);
        if (amount.signum() < 0 || reserved.compareTo(amount) < 0) {
            throw new IllegalStateException("포인트 예약 금액이 일치하지 않습니다.");
        }
        reserved = reserved.subtract(amount);
    }

    public void confirm(final BigDecimal amount) {
        PointAmount.requireWhole(amount);
        release(amount);
        balance = balance.subtract(amount);
    }

    public void apply(final BigDecimal amount) {
        PointAmount.requireWhole(amount);
        balance = balance.add(amount);
    }
}
