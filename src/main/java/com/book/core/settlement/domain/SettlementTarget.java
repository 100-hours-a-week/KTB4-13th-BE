package com.book.core.settlement.domain;

import com.book.common.domain.BaseEntity;
import com.book.core.payment.domain.enums.TransactionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Entity
@Table(name = "settlement_target")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Accessors(fluent = true)
public class SettlementTarget extends BaseEntity {
    @Column(name = "merchant_id", nullable = false)
    private Long merchantId;
    @Column(name = "settlement_date", nullable = false)
    private LocalDate settlementDate;
    @Column(name = "target_amount", nullable = false, precision = 38, scale = 2)
    private BigDecimal targetAmount;
    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 32)
    private TransactionType transactionType;
    @Column(name = "transaction_id", nullable = false)
    private Long transactionId;
    @Column(name = "order_id", nullable = false)
    private Long orderId;
    @Column(name = "product_id", nullable = false)
    private Long productId;
    @Column(name = "quantity", nullable = false)
    private long quantity;
    @Column(name = "unit_price", nullable = false, precision = 38, scale = 2)
    private BigDecimal unitPrice;
    @Column(name = "total_price", nullable = false, precision = 38, scale = 2)
    private BigDecimal totalPrice;

    public SettlementTarget(final Long merchantId, final LocalDate settlementDate, final BigDecimal targetAmount,
        final TransactionType transactionType, final Long transactionId, final Long orderId, final Long productId, final long quantity,
        final BigDecimal unitPrice, final BigDecimal totalPrice) {
        this.merchantId = merchantId;
        this.settlementDate = settlementDate;
        this.targetAmount = targetAmount;
        this.transactionType = transactionType;
        this.transactionId = transactionId;
        this.orderId = orderId;
        this.productId = productId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.totalPrice = totalPrice;
    }
}
