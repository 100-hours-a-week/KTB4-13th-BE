package com.book.core.payment.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.coupon.domain.enums.CouponType;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class PaymentDiscountTest {
    @Test
    void 미입력_포인트의_마이너스_1은_결제액을_1_늘리지만_사용포인트는_0이다() {
        final PaymentDiscount discount = new PaymentDiscount(List.of(), BigDecimal.ZERO, -1L, BigDecimal.valueOf(-1));
        assertThat(discount.usePoint()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(discount.paidAmount(BigDecimal.valueOf(10000))).isEqualByComparingTo("10001");
    }

    @Test
    void 포인트_소수값은_거절하지만_정수인_소수표현은_허용한다() {
        assertThatThrownBy(() -> new PaymentDiscount(List.of(), BigDecimal.TEN, -1L, new BigDecimal("1.5"))).isInstanceOfSatisfying(
            CoreException.class, (final CoreException e) -> assertThat(e.errorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
        final PaymentDiscount discount = new PaymentDiscount(List.of(), BigDecimal.TEN, -1L, new BigDecimal("1.00"));
        assertThat(discount.paidAmount(BigDecimal.TEN)).isEqualByComparingTo("9");
    }

    @Test
    void 상품가격이나_비율할인으로_최종금액이_소수이면_거절한다() {
        assertThatThrownBy(() -> PaymentDiscount.EMPTY.paidAmount(new BigDecimal("10.50"))).isInstanceOfSatisfying(CoreException.class,
            (final CoreException e) -> assertThat(e.errorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
        final PaymentCoupon coupon = new PaymentCoupon(10L, CouponType.PERCENT_RATE, new BigDecimal("0.15"), BigDecimal.ZERO);
        final PaymentDiscount discount = new PaymentDiscount(List.of(coupon), BigDecimal.ZERO, 10L, BigDecimal.ZERO);
        assertThatThrownBy(() -> discount.paidAmount(BigDecimal.valueOf(11))).isInstanceOfSatisfying(CoreException.class,
            (final CoreException e) -> assertThat(e.errorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
    }

    @Test
    void 할인없는_EMPTY는_원래_주문_금액을_유지한다() {
        assertThat(PaymentDiscount.EMPTY.paidAmount(BigDecimal.valueOf(10000))).isEqualByComparingTo("10000");
    }

    @Test
    void 정액_쿠폰과_포인트를_함께_차감한다() {
        final PaymentCoupon coupon = new PaymentCoupon(10L, CouponType.FIXED_AMOUNT, BigDecimal.valueOf(2000), BigDecimal.valueOf(5000));
        final PaymentDiscount discount = new PaymentDiscount(List.of(coupon), BigDecimal.valueOf(5000), 10L, BigDecimal.valueOf(1200));
        assertThat(discount.couponDiscount()).isEqualByComparingTo("2000");
        assertThat(discount.usePoint()).isEqualByComparingTo("1200");
        assertThat(discount.paidAmount(BigDecimal.valueOf(10000))).isEqualByComparingTo("6800");
    }

    @Test
    void 비율_쿠폰은_주문금액에_곱하고_쿠폰할인_필드는_비율을_보존한다() {
        final PaymentCoupon coupon = new PaymentCoupon(10L, CouponType.PERCENT_RATE, new BigDecimal("0.10"), BigDecimal.ZERO);
        final PaymentDiscount discount = new PaymentDiscount(List.of(coupon), BigDecimal.ZERO, 10L, BigDecimal.ZERO);
        assertThat(discount.couponDiscount()).isEqualByComparingTo("0.10");
        assertThat(discount.paidAmount(BigDecimal.valueOf(10000))).isEqualByComparingTo("9000");
    }

    @Test
    void 체크아웃_후보에_없는_보유_쿠폰은_거부한다() {
        assertThatThrownBy(() -> new PaymentDiscount(List.of(), BigDecimal.TEN, 10L, BigDecimal.ZERO)).isInstanceOfSatisfying(
            CoreException.class, (final CoreException e) -> assertThat(e.errorCode()).isEqualTo(ErrorCode.OWNED_COUPON_INVALID));
    }

    @Test
    void 잔액을_초과한_포인트는_거부한다() {
        assertThatThrownBy(() -> new PaymentDiscount(List.of(), BigDecimal.TEN, -1L, BigDecimal.valueOf(11))).isInstanceOfSatisfying(
            CoreException.class, (final CoreException e) -> assertThat(e.errorCode()).isEqualTo(ErrorCode.POINT_EXCEEDS_BALANCE));
    }

    @Test
    void 최소_주문금액_미달은_거부하고_같은_금액은_허용한다() {
        final PaymentCoupon coupon = new PaymentCoupon(10L, CouponType.FIXED_AMOUNT, BigDecimal.ONE, BigDecimal.TEN);
        final PaymentDiscount discount = new PaymentDiscount(List.of(coupon), BigDecimal.ZERO, 10L, BigDecimal.ZERO);
        assertThatThrownBy(() -> discount.paidAmount(BigDecimal.valueOf(9))).isInstanceOf(CoreException.class);
        assertThat(discount.paidAmount(BigDecimal.TEN)).isEqualByComparingTo("9");
    }

    @Test
    void 음수_결제액은_거부하고_0원은_허용한다() {
        final PaymentDiscount discount = new PaymentDiscount(List.of(), BigDecimal.TEN, -1L, BigDecimal.TEN);
        assertThatThrownBy(() -> discount.paidAmount(BigDecimal.valueOf(9))).isInstanceOfSatisfying(CoreException.class,
            (final CoreException e) -> assertThat(e.errorCode()).isEqualTo(ErrorCode.PAYMENT_INVALID_AMOUNT));
        assertThat(discount.paidAmount(BigDecimal.TEN)).isEqualByComparingTo("0");
    }
}
