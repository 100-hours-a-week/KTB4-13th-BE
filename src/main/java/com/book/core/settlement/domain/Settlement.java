package com.book.core.settlement.domain;

import com.book.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Entity
@Table(name = "settlement", indexes = @Index(name = "udx_settlement_merchant", columnList = "settlement_date, merchant_id", unique = true))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Accessors(fluent = true)
public class Settlement extends BaseEntity {
    @Column(name = "merchant_id", nullable = false)
    private Long merchantId;
    @Column(name = "settlement_date", nullable = false)
    private LocalDate settlementDate;
    @Column(name = "original_amount", nullable = false, precision = 38, scale = 2)
    private BigDecimal originalAmount;
    @Column(name = "fee_amount", nullable = false, precision = 38, scale = 2)
    private BigDecimal feeAmount;
    @Column(name = "fee_rate", nullable = false, precision = 38, scale = 2)
    private BigDecimal feeRate;
    @Column(name = "settlement_amount", nullable = false, precision = 38, scale = 2)
    private BigDecimal settlementAmount;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private SettlementState state;

    public Settlement(final Long merchantId, final LocalDate settlementDate, final BigDecimal originalAmount, final BigDecimal feeAmount,
        final BigDecimal feeRate, final BigDecimal settlementAmount, final SettlementState state) {
        this.merchantId = merchantId;
        this.settlementDate = settlementDate;
        this.originalAmount = originalAmount;
        this.feeAmount = feeAmount;
        this.feeRate = feeRate;
        this.settlementAmount = settlementAmount;
        this.state = state;
    }

    public void sent() {
        state = SettlementState.SENT;
    }
}
