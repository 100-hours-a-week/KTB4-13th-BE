package com.book.core.settlement.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class SettlementCalculatorTest {
    private final SettlementCalculator calculator = new SettlementCalculator();

    @ParameterizedTest
    @CsvSource({"0,0.10,10000,90000", "10000000,0.10,10000,90000", "10000001,0.08,8000,92000", "100000000,0.08,8000,92000",
        "100000001,0.05,5000,95000"})
    void 매출_경계값에_따른_수수료를_유지한다(final BigDecimal recentSalesAmount, final BigDecimal feeRate, final BigDecimal feeAmount,
        final BigDecimal settlementAmount) {
        final var result = calculator.calculate(BigDecimal.valueOf(100000), recentSalesAmount);
        assertThat(result.originalAmount()).isEqualByComparingTo("100000");
        assertThat(result.feeRate()).isEqualByComparingTo(feeRate);
        assertThat(result.feeAmount()).isEqualByComparingTo(feeAmount);
        assertThat(result.settlementAmount()).isEqualByComparingTo(settlementAmount);
    }

    @ParameterizedTest
    @CsvSource({"0.05,0.01,0.04", "-0.05,-0.01,-0.04", "0,0,0"})
    void 양수와_취소음수_및_영원에_HALF_UP을_적용한다(final BigDecimal amount, final BigDecimal feeAmount, final BigDecimal settlementAmount) {
        final var result = calculator.calculate(amount, BigDecimal.ZERO);
        assertThat(result.feeAmount()).isEqualByComparingTo(feeAmount);
        assertThat(result.settlementAmount()).isEqualByComparingTo(settlementAmount);
    }
}
