package com.book.core.payment.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.book.core.payment.domain.enums.PaymentMethod;
import com.book.core.payment.domain.enums.PaymentState;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PaymentTest {
    @Test
    void 승인_값과_시각을_기록하고_SUCCESS로_전환한다() {
        final Payment payment = new Payment(1L, 2L, BigDecimal.valueOf(10000), -1L, BigDecimal.ZERO, BigDecimal.ZERO, 3L,
            BigDecimal.valueOf(10000), PaymentState.READY);
        payment.success("test-key", PaymentMethod.CARD, "test-approval");
        assertThat(payment.state()).isEqualTo(PaymentState.SUCCESS);
        assertThat(payment.externalPaymentKey()).isEqualTo("test-key");
        assertThat(payment.method()).isEqualTo(PaymentMethod.CARD);
        assertThat(payment.approveCode()).isEqualTo("test-approval");
        assertThat(payment.paidAt()).isNotNull();
        assertThat(payment.userId()).isEqualTo(1L);
        assertThat(payment.payerUserId()).isEqualTo(3L);
        assertThat(payment.hasAppliedCoupon()).isFalse();
    }

    @Test
    void 양수_보유쿠폰_ID가_있으면_쿠폰이_적용되어_있다() {
        final Payment payment =
            new Payment(1L, 2L, BigDecimal.TEN, 4L, BigDecimal.ONE, BigDecimal.ZERO, 1L, BigDecimal.valueOf(9), PaymentState.READY);
        assertThat(payment.hasAppliedCoupon()).isTrue();
    }
}
