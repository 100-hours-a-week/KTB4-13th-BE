package com.book.core.point.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.book.core.point.domain.enums.PointType;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PointHistoryTest {
    @Test
    void 차감_이력과_음수_잔액도_기록할_수_있다() {
        final PointHistory history = PointHistory.builder().userId(42L).type(PointType.REVIEW).referenceId(10L)
            .amount(new BigDecimal("-100")).balanceAfter(new BigDecimal("-50")).build();

        assertThat(history.id()).isNull();
        assertThat(history.amount()).isEqualByComparingTo("-100");
        assertThat(history.balanceAfter()).isEqualByComparingTo("-50");
    }
}
