package com.book.core.point.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.point.domain.enums.PointType;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PointHistoryTest {
    @Test
    void 필수값이_누락된_빌더는_이력을_생성하지_않는다() {
        assertThatThrownBy(() -> PointHistory.builder().userId(42L).amount(BigDecimal.TEN).build()).isInstanceOfSatisfying(
            CoreException.class, (final var exception) -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
    }

    @Test
    void 차감_이력과_음수_잔액도_기록할_수_있다() {
        final PointHistory history = PointHistory.builder().userId(42L).type(PointType.REVIEW).referenceId(10L)
            .amount(new BigDecimal("-100")).balanceAfter(new BigDecimal("-50")).build();

        assertThat(history.id()).isNull();
        assertThat(history.amount()).isEqualByComparingTo("-100");
        assertThat(history.balanceAfter()).isEqualByComparingTo("-50");
    }
}
