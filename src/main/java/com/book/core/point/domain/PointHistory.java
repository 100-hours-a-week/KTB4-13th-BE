package com.book.core.point.domain;

import com.book.common.domain.BaseEntity;
import com.book.core.point.domain.enums.PointType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

@Entity
@Table(name = "point_history")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Accessors(fluent = true)
public class PointHistory extends BaseEntity {
    private Long userId;
    private PointType type;
    private Long referenceId;
    private BigDecimal amount;
    private BigDecimal balanceAfter;

    @Column(name = "event_key", length = 150)
    private String eventKey;

    @Builder
    public PointHistory(final Long id, final Long userId, final PointType type, final Long referenceId, final BigDecimal amount,
        final BigDecimal balanceAfter) {
        super(id);
        this.userId = userId;
        this.type = type;
        this.referenceId = referenceId;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
    }

    public PointHistory withEventKey(final String eventKey) {
        this.eventKey = eventKey;
        return this;
    }

}
