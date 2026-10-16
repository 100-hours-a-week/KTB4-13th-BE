package com.book.core.settlement.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** 참고 정산의 매출 구간과 반올림 규칙을 적용한다. */
public class SettlementCalculator {
    private static final BigDecimal DEFAULT_FEE_RATE = new BigDecimal("0.1");
    private static final BigDecimal OVER_10M_FEE_RATE = new BigDecimal("0.08");
    private static final BigDecimal OVER_100M_FEE_RATE = new BigDecimal("0.05");
    private static final BigDecimal SALES_THRESHOLD_100M = BigDecimal.valueOf(100_000_000);
    private static final BigDecimal SALES_THRESHOLD_10M = BigDecimal.valueOf(10_000_000);

    public SettlementAmount calculate(final BigDecimal amount, final BigDecimal recentSalesAmount) {
        final BigDecimal feeRate;
        if (recentSalesAmount.compareTo(SALES_THRESHOLD_100M) > 0) {
            feeRate = OVER_100M_FEE_RATE;
        } else if (recentSalesAmount.compareTo(SALES_THRESHOLD_10M) > 0) {
            feeRate = OVER_10M_FEE_RATE;
        } else {
            feeRate = DEFAULT_FEE_RATE;
        }
        final BigDecimal feeAmount = amount.multiply(feeRate).setScale(2, RoundingMode.HALF_UP);
        return new SettlementAmount(amount, feeAmount, feeRate, amount.subtract(feeAmount).setScale(2, RoundingMode.HALF_UP));
    }
}
