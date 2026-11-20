package com.book.core.point.domain;

import com.book.common.domain.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Entity
@Table(name = "point_recovery_settlement")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Accessors(fluent = true)
public class PointRecoverySettlement extends BaseEntity {
    private Long recoveryId;
    private String sourceType;
    private Long sourceReferenceId;
    private BigDecimal amount;

    public PointRecoverySettlement(final Long recoveryId, final String sourceType, final Long sourceReferenceId, final BigDecimal amount) {
        if (recoveryId == null || amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("포인트 상계 정보가 올바르지 않습니다.");
        }
        this.recoveryId = recoveryId;
        this.sourceType = sourceType;
        this.sourceReferenceId = sourceReferenceId;
        this.amount = amount;
    }
}
