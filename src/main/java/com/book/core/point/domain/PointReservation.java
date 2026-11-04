package com.book.core.point.domain;

import com.book.common.domain.BaseEntity;
import jakarta.persistence.Column;
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
@Table(name = "point_reservation")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Accessors(fluent = true)
public class PointReservation extends BaseEntity {
    private Long userId;
    private Long attemptId;
    private Long paymentId;
    private BigDecimal amount;
    @Column(name = "reservation_state")
    @Enumerated(EnumType.STRING)
    private PointReservationState state;

    public PointReservation(final Long userId, final Long attemptId, final BigDecimal amount, final Long paymentId) {
        this.userId = userId;
        this.attemptId = attemptId;
        this.paymentId = paymentId;
        this.amount = amount;
        this.state = PointReservationState.RESERVED;
    }

    public void confirm() {
        state = PointReservationState.CONFIRMED;
    }

    public void release() {
        state = PointReservationState.RELEASED;
    }
}
