package com.book.core.point.domain;

import com.book.common.domain.BaseEntity;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Entity
@Table(name = "point_lot")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Accessors(fluent = true)
public class PointLot extends BaseEntity {
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "point_history_id", nullable = false, unique = true)
    private Long pointHistoryId;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "original_amount", nullable = false, precision = 38, scale = 2)
    private BigDecimal originalAmount;

    @Column(name = "remaining_amount", nullable = false, precision = 38, scale = 2)
    private BigDecimal remainingAmount;

    @Column(name = "expiry_notification_sent_at")
    private LocalDateTime expiryNotificationSentAt;

    @Version
    private long version;

    public PointLot(final Long userId, final Long pointHistoryId, final LocalDateTime expiresAt, final BigDecimal originalAmount,
        final BigDecimal remainingAmount) {
        this.userId = userId;
        this.pointHistoryId = pointHistoryId;
        this.expiresAt = expiresAt;
        this.originalAmount = originalAmount;
        this.remainingAmount = remainingAmount;
        validate();
    }

    public void consume(final BigDecimal amount) {
        if (amount == null || amount.signum() <= 0 || amount.compareTo(remainingAmount) > 0) {
            throw new CoreException(ErrorCode.POINT_DATA_INCONSISTENT);
        }
        remainingAmount = remainingAmount.subtract(amount);
    }

    public void restore(final BigDecimal amount, final LocalDateTime now) {
        if (amount == null || amount.signum() <= 0 || !expiresAt.isAfter(now)
            || remainingAmount.add(amount).compareTo(originalAmount) > 0) {
            throw new CoreException(ErrorCode.POINT_DATA_INCONSISTENT);
        }
        remainingAmount = remainingAmount.add(amount);
    }

    public BigDecimal expire(final LocalDateTime now) {
        if (expiresAt.isAfter(now)) {
            throw new CoreException(ErrorCode.POINT_DATA_INCONSISTENT);
        }
        final BigDecimal expiredAmount = remainingAmount;
        remainingAmount = BigDecimal.ZERO;
        return expiredAmount;
    }

    public void markExpiryNotificationSent(final LocalDateTime sentAt) {
        if (expiryNotificationSentAt == null) {
            expiryNotificationSentAt = sentAt;
        }
    }

    private void validate() {
        if (userId == null || userId <= 0 || pointHistoryId == null || pointHistoryId <= 0 || expiresAt == null || originalAmount == null
            || originalAmount.signum() <= 0 || remainingAmount == null || remainingAmount.signum() < 0
            || remainingAmount.compareTo(originalAmount) > 0) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
    }
}
