package com.book.core.merchant.domain;

import com.book.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;


@Entity
@Table(name = "merchant")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Accessors(fluent = true)
public class Merchant extends BaseEntity {
    @Column(nullable = false)
    private String name;
    @Column(name = "settlement_cycle", nullable = false)
    private int settlementCycle;

    public Merchant(final String name) {
        this(name, 1);
    }

    public Merchant(final String name, final int settlementCycle) {
        this.name = name;
        this.settlementCycle = settlementCycle;
    }
}
