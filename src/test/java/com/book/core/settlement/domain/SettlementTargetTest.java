package com.book.core.settlement.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.book.core.payment.domain.enums.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class SettlementTargetTest {
    @Test
    void 부분취소는_음수_차감액과_양수_주문상품_금액을_구분한다() {
        final SettlementTarget target = new SettlementTarget(1L, LocalDate.of(2026, 10, 16), new BigDecimal("-200"),
            TransactionType.PARTIAL_CANCEL, 10L, 20L, 30L, 2, new BigDecimal("100"), new BigDecimal("200"));

        assertThat(target.targetAmount()).isEqualByComparingTo("-200");
        assertThat(target.totalPrice()).isEqualByComparingTo("200");
        assertThat(target.unitPrice()).isEqualByComparingTo("100");
        assertThat(target.quantity()).isEqualTo(2);
        assertThat(target.transactionType()).isEqualTo(TransactionType.PARTIAL_CANCEL);
    }
}
