package com.book.core.payment.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.cancel.application.usecase.CreateCancelBalanceUseCase;
import com.book.core.coupon.application.usecase.UseOwnedCouponUseCase;
import com.book.core.order.application.usecase.GetPaymentOrderUseCase;
import com.book.core.order.domain.Order;
import com.book.core.order.domain.OrderItem;
import com.book.core.order.domain.OrderItemStatus;
import com.book.core.order.domain.OrderStatus;
import com.book.core.payment.application.port.PaymentHistoryRepositoryPort;
import com.book.core.payment.application.port.PaymentRepositoryPort;
import com.book.core.payment.domain.Payment;
import com.book.core.payment.domain.PaymentDiscount;
import com.book.core.payment.domain.PaymentHistory;
import com.book.core.payment.domain.enums.PaymentMethod;
import com.book.core.payment.domain.enums.PaymentState;
import com.book.core.payment.domain.enums.TransactionType;
import com.book.core.point.application.usecase.DeductPointUseCase;
import com.book.core.point.application.usecase.EarnPointUseCase;
import com.book.core.point.domain.PointAmount;
import com.book.core.point.domain.enums.PointType;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.springframework.test.util.ReflectionTestUtils;

class PaymentUseCasesTest {
    private final PaymentRepositoryPort payments = mock(PaymentRepositoryPort.class);
    private final PaymentHistoryRepositoryPort histories = mock(PaymentHistoryRepositoryPort.class);
    private final GetPaymentOrderUseCase orders = mock(GetPaymentOrderUseCase.class);
    private final UseOwnedCouponUseCase coupons = mock(UseOwnedCouponUseCase.class);
    private final DeductPointUseCase deduct = mock(DeductPointUseCase.class);
    private final EarnPointUseCase earn = mock(EarnPointUseCase.class);
    private final CreateCancelBalanceUseCase cancel = mock(CreateCancelBalanceUseCase.class);

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

    @Test
    void 승인_금액은_참고처럼_스케일까지_비교하고_불일치는_거부한다() {
        when(payments.findByOrderId(2L)).thenReturn(Optional.of(payment(-1L)));
        final ValidatePaymentUseCase validator = new ValidatePaymentUseCase(payments);
        validator.validateForApprove(2L, BigDecimal.valueOf(8800));
        assertThatThrownBy(() -> validator.validateForApprove(2L, new BigDecimal("8800.00"))).isInstanceOf(CoreException.class);
        assertThatThrownBy(() -> validator.validateForApprove(2L, BigDecimal.TEN)).isInstanceOf(CoreException.class);
        validator.validateForFail(2L);
    }

    @Test
    void 승인_검증은_SUCCESS와_없는_결제를_거부한다() {
        final Payment payment = payment(-1L);
        payment.success("test", PaymentMethod.CARD, "test");
        when(payments.findByOrderId(2L)).thenReturn(Optional.of(payment));
        final ValidatePaymentUseCase validator = new ValidatePaymentUseCase(payments);
        assertThatThrownBy(() -> validator.validateForApprove(2L, BigDecimal.valueOf(8800))).isInstanceOf(CoreException.class);
        when(payments.findByOrderId(2L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> validator.validateForFail(2L)).isInstanceOf(CoreException.class);
    }

    @Test
    void 성공은_주문과_상품_혜택_취소잔액_거래이력을_연결한다() {
        final Order order = order();
        final Payment payment = payment(10L);
        when(orders.execute("order", OrderStatus.CREATED)).thenReturn(order);
        when(payments.findByOrderId(2L)).thenReturn(Optional.of(payment));
        assertThat(complete().execute("order", "test-external")).isEqualTo(3L);
        assertThat(order.status()).isEqualTo(OrderStatus.PAID);
        assertThat(order.items()).allSatisfy((final OrderItem item) -> assertThat(item.status()).isEqualTo(OrderItemStatus.PAID));
        assertThat(payment.state()).isEqualTo(PaymentState.SUCCESS);
        verify(coupons).execute(10L);
        verify(deduct).execute(1L, PointType.PAYMENT, 3L, BigDecimal.valueOf(1200));
        verify(earn).execute(1L, PointType.PAYMENT, 3L, PointAmount.PAYMENT);
        verify(cancel).execute(2L, BigDecimal.valueOf(8800), BigDecimal.valueOf(1200), BigDecimal.ZERO);
        final ArgumentCaptor<PaymentHistory> captured = ArgumentCaptor.forClass(PaymentHistory.class);
        verify(histories).save(captured.capture());
        assertThat(captured.getValue().type()).isEqualTo(TransactionType.PAYMENT);
        assertThat(captured.getValue().externalPaymentKey()).isEqualTo("test-external");
        assertThat(captured.getValue().occurredAt()).isEqualTo(payment.paidAt());
    }

    @Test
    void 쿠폰_없는_성공은_쿠폰_사용을_생략한다() {
        when(orders.execute("order", OrderStatus.CREATED)).thenReturn(order());
        when(payments.findByOrderId(2L)).thenReturn(Optional.of(payment(-1L)));
        complete().execute("order", "test-external");
        verifyNoInteractions(coupons);
    }

    @Test
    void 혜택_처리_실패는_전파하고_후속_거래이력은_저장하지_않는다() {
        when(orders.execute("order", OrderStatus.CREATED)).thenReturn(order());
        when(payments.findByOrderId(2L)).thenReturn(Optional.of(payment(10L)));
        doThrow(new CoreException(ErrorCode.OWNED_COUPON_INVALID_USAGE)).when(coupons).execute(10L);
        assertThatThrownBy(() -> complete().execute("order", "test-external")).isInstanceOf(CoreException.class);
        verifyNoInteractions(deduct, earn, cancel, histories);
    }

    @Test
    void 실패는_마이너스_1_거래이력만_저장하고_READY와_CREATED를_유지한다() {
        final Order order = order();
        final Payment payment = payment(10L);
        when(orders.execute("order", OrderStatus.CREATED)).thenReturn(order);
        when(payments.findByOrderId(2L)).thenReturn(Optional.of(payment));
        new FailPaymentUseCase(payments, orders, histories).execute("order", "FAIL", "test failure");
        final ArgumentCaptor<PaymentHistory> captured = ArgumentCaptor.forClass(PaymentHistory.class);
        verify(histories).save(captured.capture());
        assertThat(captured.getValue().type()).isEqualTo(TransactionType.PAYMENT_FAIL);
        assertThat(captured.getValue().paidAmount()).isEqualByComparingTo("-1");
        assertThat(captured.getValue().message()).isEqualTo("[FAIL] test failure");
        assertThat(payment.state()).isEqualTo(PaymentState.READY);
        assertThat(order.status()).isEqualTo(OrderStatus.CREATED);
        verifyNoInteractions(coupons, deduct, earn, cancel);
    }

    private CompletePaymentUseCase complete() {
        return new CompletePaymentUseCase(payments, orders, coupons, deduct, earn, cancel, histories);
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
