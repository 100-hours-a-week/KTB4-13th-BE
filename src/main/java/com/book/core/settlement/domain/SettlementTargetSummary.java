package com.book.core.settlement.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SettlementTargetSummary(Long merchantId, LocalDate settlementDate, BigDecimal targetAmount, long targetCount,
    long orderCount) {
}
