package com.book.core.cancel.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class CancelBalanceTest {
    @Test
    void 취소액은_가능액에서_차감하고_취소누계에_더한다() {
        final CancelBalance balance = new CancelBalance(1L, BigDecimal.valueOf(1000), BigDecimal.valueOf(200), BigDecimal.valueOf(100),
            BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        balance.cancel(BigDecimal.valueOf(300), BigDecimal.valueOf(50), BigDecimal.valueOf(20));
        assertThat(balance.cancellablePaidAmount()).isEqualByComparingTo("700");
        assertThat(balance.cancellablePointAmount()).isEqualByComparingTo("150");
        assertThat(balance.cancellableCouponAmount()).isEqualByComparingTo("80");
        assertThat(balance.totalCanceledAmount()).isEqualByComparingTo("370");
    }
}
