package com.book.core.settlement.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class SettlementTest {
    @Test
    void 지급_상태를_변경해도_원금과_수수료와_지급액은_유지한다() {
        final LocalDate date = LocalDate.of(2026, 10, 16);
        final Settlement settlement = new Settlement(1L, date, new BigDecimal("1000"), new BigDecimal("100"), new BigDecimal("0.1"),
            new BigDecimal("900"), SettlementState.READY);

        assertThat(settlement.state()).isEqualTo(SettlementState.READY);
        settlement.sent();

        assertThat(settlement.state()).isEqualTo(SettlementState.SENT);
        assertThat(settlement.originalAmount()).isEqualByComparingTo("1000");
        assertThat(settlement.feeAmount()).isEqualByComparingTo("100");
        assertThat(settlement.feeRate()).isEqualByComparingTo("0.1");
        assertThat(settlement.settlementAmount()).isEqualByComparingTo("900");
        assertThat(settlement.merchantId()).isEqualTo(1L);
        assertThat(settlement.settlementDate()).isEqualTo(date);
    }
}
