package com.book.core.point.domain;

import com.book.common.domain.BaseEntity;
import com.book.core.point.domain.enums.PointType;
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
@Table(name = "point_earn_event")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Accessors(fluent = true)
public class PointEarnEvent extends BaseEntity {
    private Long userId;
    @Column(name = "source_type")
    @Enumerated(EnumType.STRING)
    private PointType type;
    private Long referenceId;
    private BigDecimal amount;

    public PointEarnEvent(final Long userId, final PointType type, final Long referenceId, final BigDecimal amount) {
        this.userId = userId;
        this.type = type;
        this.referenceId = referenceId;
        this.amount = amount;
    }
}
