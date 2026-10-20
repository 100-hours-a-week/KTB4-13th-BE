package com.book.core.settlement.domain;

import java.math.BigDecimal;

public record SettlementAmount(BigDecimal originalAmount, BigDecimal feeAmount, BigDecimal feeRate, BigDecimal settlementAmount) {
}
