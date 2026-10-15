package com.book.core.point.domain;

import com.book.common.domain.BaseEntity;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

@Entity
@Table(name = "point_balance")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Accessors(fluent = true)
public class PointBalance extends BaseEntity {
    private Long userId;
    private BigDecimal balance = BigDecimal.ZERO;

    @Version
    private long version = 0L;

    public PointBalance(final Long userId) {
        this(null, userId, BigDecimal.ZERO);
    }

    public PointBalance(final Long id, final Long userId, final BigDecimal balance) {
        super(id);
        if (userId == null || userId <= 0 || balance == null) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        this.userId = userId;
        this.balance = balance;
    }

    public void apply(final BigDecimal amount) {
        if (amount == null) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        balance = balance.add(amount);
    }
}
