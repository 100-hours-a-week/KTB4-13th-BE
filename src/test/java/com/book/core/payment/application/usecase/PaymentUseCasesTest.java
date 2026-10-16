package com.book.core.payment.application.usecase;

import org.mockito.invocation.InvocationOnMock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.coupon.application.usecase.UseOwnedCouponUseCase;
import com.book.core.order.application.usecase.GetPaymentOrderUseCase;
import com.book.core.order.domain.Order;
import com.book.core.payment.application.port.PaymentRepositoryPort;
import com.book.core.payment.application.port.PaymentHistoryRepositoryPort;
import com.book.core.payment.domain.Payment;
import com.book.core.payment.domain.PaymentDiscount;
import com.book.core.payment.domain.enums.PaymentMethod;
import com.book.core.payment.domain.enums.PaymentState;
import com.book.core.point.application.usecase.DeductPointUseCase;
import com.book.core.point.application.usecase.EarnPointUseCase;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class PaymentUseCasesTest {
    private final PaymentRepositoryPort payments = mock(PaymentRepositoryPort.class);
    private final PaymentHistoryRepositoryPort histories = mock(PaymentHistoryRepositoryPort.class);
    private final GetPaymentOrderUseCase orders = mock(GetPaymentOrderUseCase.class);
    private final UseOwnedCouponUseCase coupons = mock(UseOwnedCouponUseCase.class);
    private final DeductPointUseCase deduct = mock(DeductPointUseCase.class);
    private final EarnPointUseCase earn = mock(EarnPointUseCase.class);

    @Test
    void 생성은_주문_소유자와_결제자_및_할인_값을_유지한다() {
        final Order order = order();
        when(payments.findByOrderId(2L)).thenReturn(Optional.empty());
        when(payments.save(any())).thenAnswer((final InvocationOnMock invocation) -> {
            final Payment payment = invocation.getArgument(0);
            ReflectionTestUtils.setField(payment, "id", 3L);
            return payment;
        });
        assertThat(new CreatePaymentUseCase(payments).execute(order, 99L, PaymentDiscount.EMPTY)).isEqualTo(3L);
        final ArgumentCaptor<Payment> captured = ArgumentCaptor.forClass(Payment.class);
        verify(payments).save(captured.capture());
        assertThat(captured.getValue().userId()).isEqualTo(1L);
        assertThat(captured.getValue().payerUserId()).isEqualTo(99L);
        assertThat(captured.getValue().paidAmount()).isEqualByComparingTo("10000");
        assertThat(captured.getValue().state()).isEqualTo(PaymentState.READY);
    }

    @Test
    void 이미_성공한_결제가_있는_주문은_거부한다() {
        final Payment payment = payment(-1L);
        payment.success("test", PaymentMethod.CARD, "test");
        when(payments.findByOrderId(2L)).thenReturn(Optional.of(payment));
        assertThatThrownBy(() -> new CreatePaymentUseCase(payments).execute(order(), 1L, PaymentDiscount.EMPTY)).isInstanceOfSatisfying(
            CoreException.class, (final CoreException e) -> assertThat(e.errorCode()).isEqualTo(ErrorCode.ORDER_ALREADY_PAID));
    }

    private static Order order() {
        final Order order = Order.create(1L, "order", null);
        ReflectionTestUtils.setField(order, "id", 2L);
        order.addItem(10L, "책", "", "저자", BigDecimal.valueOf(10000), BigDecimal.valueOf(10000), 1);
        return order;
    }

    private static Payment payment(final Long couponId) {
        final Payment payment = new Payment(1L, 2L, BigDecimal.valueOf(10000), couponId, BigDecimal.ZERO, BigDecimal.valueOf(1200), 1L,
            BigDecimal.valueOf(8800), PaymentState.READY);
        ReflectionTestUtils.setField(payment, "id", 3L);
        return payment;
    }
}
