package com.book.core.point.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PointBalanceTest {
    @Test
    void 신규_잔액은_ID_없이_0으로_초기화한다() {
        final PointBalance pointBalance = new PointBalance(42L);

        assertThat(pointBalance.id()).isNull();
        assertThat(pointBalance.userId()).isEqualTo(42L);
        assertThat(pointBalance.balance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void 기존_ID와_잔액을_복원한다() {
        final PointBalance pointBalance = new PointBalance(7L, 42L, new BigDecimal("100.00"));

        assertThat(pointBalance.id()).isEqualTo(7L);
        assertThat(pointBalance.balance()).isEqualByComparingTo("100.00");
    }

    @ParameterizedTest
    @CsvSource({"1000, 250, 1250", "1000, -250, 750", "1000, 0, 1000", "100.00, 200.0, 300.00"})
    void 변동_금액을_잔액에_반영한다(final BigDecimal initialBalance, final BigDecimal amount, final BigDecimal expectedBalance) {
        final PointBalance pointBalance = new PointBalance(42L);
        pointBalance.apply(initialBalance);

        pointBalance.apply(amount);

        assertThat(pointBalance.balance()).isEqualByComparingTo(expectedBalance);
    }

    @Test
    void 소수_잔액으로_객체를_생성할_수_없다() {
        assertThatThrownBy(() -> new PointBalance(7L, 42L, new BigDecimal("100.50"))).isInstanceOfSatisfying(CoreException.class,
            (final CoreException exception) -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
    }

    @Test
    void 소수_변동액을_거절하고_잔액을_유지한다() {
        final PointBalance pointBalance = new PointBalance(7L, 42L, new BigDecimal("100"));

        assertThatThrownBy(() -> pointBalance.apply(new BigDecimal("0.50"))).isInstanceOfSatisfying(CoreException.class,
            (final CoreException exception) -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
        assertThat(pointBalance.balance()).isEqualByComparingTo("100");
    }

    @Test
    void 여러_변동_금액을_누적해서_반영한다() {
        final PointBalance pointBalance = new PointBalance(42L);
        pointBalance.apply(new BigDecimal("1000"));

        pointBalance.apply(new BigDecimal("500"));
        pointBalance.apply(new BigDecimal("-200"));

        assertThat(pointBalance.balance()).isEqualByComparingTo("1300");
    }
}
