package com.book.core.point.domain;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.common.exception.CoreException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PointAmountTest {
    @Test
    void 정수값과_마이너스일_표현은_허용한다() {
        assertThatCode(() -> PointAmount.requireWhole(new BigDecimal("1.00"))).doesNotThrowAnyException();
        assertThatCode(() -> PointAmount.requireWhole(BigDecimal.valueOf(-1))).doesNotThrowAnyException();
        assertThatCode(() -> PointAmount.requireNonNegativeWhole(BigDecimal.ZERO)).doesNotThrowAnyException();
    }

    @Test
    void 소수값과_음수_복원값을_거부한다() {
        assertThatThrownBy(() -> PointAmount.requireWhole(new BigDecimal("0.5"))).isInstanceOf(CoreException.class);
        assertThatThrownBy(() -> PointAmount.requireNonNegativeWhole(BigDecimal.valueOf(-1))).isInstanceOf(CoreException.class);
        assertThatThrownBy(() -> PointAmount.requireWhole(null)).isInstanceOf(CoreException.class);
    }
}
