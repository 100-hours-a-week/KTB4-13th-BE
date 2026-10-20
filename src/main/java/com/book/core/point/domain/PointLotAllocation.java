package com.book.core.point.domain;

import com.book.common.domain.BaseEntity;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Entity
@Table(name = "point_lot_allocation",
    uniqueConstraints = @UniqueConstraint(name = "udx_point_lot_allocation_debit_lot_item",
        columnNames = {"debit_history_id", "point_lot_id", "order_item_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Accessors(fluent = true)
public class PointLotAllocation extends BaseEntity {
    @Column(name = "debit_history_id", nullable = false)
    private Long debitHistoryId;

    @Column(name = "point_lot_id", nullable = false)
    private Long pointLotId;

    @Column(name = "order_item_id")
    private Long orderItemId;

    @Column(name = "amount", nullable = false, precision = 38, scale = 2)
    private BigDecimal amount;

    @Column(name = "restored_amount", nullable = false, precision = 38, scale = 2)
    private BigDecimal restoredAmount = BigDecimal.ZERO;

    public PointLotAllocation(final Long debitHistoryId, final Long pointLotId, final BigDecimal amount) {
        this(debitHistoryId, pointLotId, null, amount);
    }

    public PointLotAllocation(final Long debitHistoryId, final Long pointLotId, final Long orderItemId, final BigDecimal amount) {
        if (debitHistoryId == null || pointLotId == null || amount == null || amount.signum() <= 0) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        this.debitHistoryId = debitHistoryId;
        this.pointLotId = pointLotId;
        this.orderItemId = orderItemId;
        this.amount = amount;
    }

    public BigDecimal restorableAmount() {
        return amount.subtract(restoredAmount);
    }

    public void restore(final BigDecimal amount) {
        if (amount == null || amount.signum() <= 0 || restoredAmount.add(amount).compareTo(this.amount) > 0) {
            throw new CoreException(ErrorCode.POINT_DATA_INCONSISTENT);
        }
        restoredAmount = restoredAmount.add(amount);
    }
}
