package com.book.core.point.domain;

import com.book.common.domain.BaseEntity;
import com.book.core.point.domain.enums.PointType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Entity
@Table(name = "point_recovery")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Accessors(fluent = true)
public class PointRecovery extends BaseEntity {
    private Long userId;
    @Enumerated(EnumType.STRING)
    private PointType sourceType;
    private Long sourceReferenceId;
    private BigDecimal originalAmount;
    private BigDecimal remainingAmount;

    public PointRecovery(final Long userId, final PointType sourceType, final Long sourceReferenceId, final BigDecimal originalAmount,
        final BigDecimal remainingAmount) {
        this.userId = userId;
        this.sourceType = sourceType;
        this.sourceReferenceId = sourceReferenceId;
        this.originalAmount = originalAmount;
        this.remainingAmount = remainingAmount;
    }

    public BigDecimal settle(final BigDecimal amount) {
        final BigDecimal applied = remainingAmount.min(amount);
        remainingAmount = remainingAmount.subtract(applied);
        return applied;
    }
}
