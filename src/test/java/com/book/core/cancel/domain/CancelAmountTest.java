package com.book.core.cancel.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class CancelAmountTest {
    @Test
    void 포인트_결제는_현금_잔액을_먼저_환불하고_나머지를_포인트로_환불한다() {
        assertAmounts(amount(10000, 30000, 0, 15000, 15000, 0, 0), 10000, 0, 0, false);
        assertAmounts(amount(10000, 30000, 10000, 5000, 15000, 0, 0), 5000, 5000, 0, false);
        assertAmounts(amount(10000, 30000, 20000, 0, 10000, 0, 0), 0, 10000, 0, false);
    }

    @Test
    void 쿠폰_최소금액이_깨지는_취소에서는_쿠폰_잔액을_우선_차감하고_한번_복원한다() {
        assertAmounts(amount(10000, 50000, 0, 40000, 0, 10000, 35000), 10000, 0, 0, false);
        assertAmounts(amount(10000, 50000, 10000, 30000, 0, 10000, 35000), 0, 0, 10000, true);
        assertAmounts(amount(10000, 50000, 20000, 30000, 0, 0, 35000), 10000, 0, 0, false);
    }

    @Test
    void 복합_결제의_여섯_부분취소_배분을_참고_사례와_동일하게_계산한다() {
        assertAmounts(amount(10000, 60000, 0, 30000, 15000, 15000, 35000), 10000, 0, 0, false);
        assertAmounts(amount(10000, 60000, 10000, 20000, 15000, 15000, 35000), 10000, 0, 0, false);
        assertAmounts(amount(10000, 60000, 20000, 10000, 15000, 15000, 35000), 0, 0, 10000, true);
        assertAmounts(amount(10000, 60000, 30000, 10000, 15000, 5000, 35000), 5000, 0, 5000, false);
        assertAmounts(amount(10000, 60000, 40000, 5000, 15000, 0, 35000), 5000, 5000, 0, false);
        assertAmounts(amount(10000, 60000, 50000, 0, 10000, 0, 35000), 0, 10000, 0, false);
    }

    @Test
    void 취소_전_잔액이_최소금액과_같으면_참고의_엄격한_초과조건에_따라_쿠폰을_복원하지_않는다() {
        assertAmounts(amount(10000, 35000, 0, 25000, 0, 10000, 35000), 0, 0, 10000, false);
        assertAmounts(amount(10000, 45000, 0, 35000, 0, 10000, 35000), 10000, 0, 0, false);
    }

    private static CancelAmount amount(final long cancel, final long order, final long canceled, final long paid, final long point,
        final long coupon, final long min) {
        return new CancelAmount(BigDecimal.valueOf(cancel), BigDecimal.valueOf(order), BigDecimal.valueOf(canceled),
            BigDecimal.valueOf(paid), BigDecimal.valueOf(point), BigDecimal.valueOf(coupon), BigDecimal.valueOf(min));
    }

    private static void assertAmounts(final CancelAmount amount, final long paid, final long point, final long coupon,
        final boolean restore) {
        assertThat(amount.paidAmount()).isEqualByComparingTo(BigDecimal.valueOf(paid));
        assertThat(amount.pointAmount()).isEqualByComparingTo(BigDecimal.valueOf(point));
        assertThat(amount.couponAmount()).isEqualByComparingTo(BigDecimal.valueOf(coupon));
        assertThat(amount.isRestoreCoupon()).isEqualTo(restore);
    }
}
