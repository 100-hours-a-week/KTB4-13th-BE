package com.book.core.cancel.application.usecase;

import org.mockito.invocation.InvocationOnMock;

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
import com.book.core.cancel.application.port.CancelBalanceRepositoryPort;
import com.book.core.cancel.application.port.CancelRepositoryPort;
import com.book.core.cancel.domain.Cancel;
import com.book.core.cancel.domain.CancelAction;
import com.book.core.cancel.domain.CancelBalance;
import com.book.core.cancel.domain.enums.CancelType;
import com.book.core.coupon.application.usecase.RevertOwnedCouponUseCase;
import com.book.core.order.application.usecase.GetOrderForCancelUseCase;
import com.book.core.order.domain.Order;
import com.book.core.order.domain.OrderStatus;
import com.book.core.payment.application.usecase.GetPaymentUseCase;
import com.book.core.payment.application.usecase.RecordPaymentHistoryUseCase;
import com.book.core.payment.domain.Payment;
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
import org.springframework.test.util.ReflectionTestUtils;

class CancelUseCasesTest {
    private final GetOrderForCancelUseCase orders = mock(GetOrderForCancelUseCase.class);
    private final GetPaymentUseCase payments = mock(GetPaymentUseCase.class);
    private final CancelBalanceRepositoryPort balances = mock(CancelBalanceRepositoryPort.class);
    private final CancelRepositoryPort cancels = mock(CancelRepositoryPort.class);
    private final RevertOwnedCouponUseCase coupons = mock(RevertOwnedCouponUseCase.class);
    private final EarnPointUseCase earn = mock(EarnPointUseCase.class);
    private final DeductPointUseCase deduct = mock(DeductPointUseCase.class);
    private final RecordPaymentHistoryUseCase histories = mock(RecordPaymentHistoryUseCase.class);

    @Test
    void 전체취소는_쿠폰과_포인트를_복원하고_적립포인트를_회수하며_이력을_기록한다() {
        final Order order = order(3);
        final CancelBalance balance = prepare(order, 10L);
        assertThat(full().execute(new CancelAction("order"))).isEqualTo(5L);
        assertThat(order.status()).isEqualTo(OrderStatus.CANCELED);
        assertThat(balance.cancellablePaidAmount()).isEqualByComparingTo("0");
        verify(coupons).execute(10L);
        verify(earn).execute(1L, PointType.PAYMENT, 3L, BigDecimal.valueOf(1200));
        verify(deduct).execute(1L, PointType.PAYMENT, 3L, PointAmount.PAYMENT);
        final ArgumentCaptor<Cancel> capture = ArgumentCaptor.forClass(Cancel.class);
        verify(cancels).save(capture.capture());
        assertThat(capture.getValue().type()).isEqualTo(CancelType.ALL);
        assertThat(capture.getValue().orderItemId()).isEqualTo(-1L);
        assertThat(capture.getValue().canceledQuantity()).isEqualTo(-1L);
        final ArgumentCaptor<PaymentHistory> history = ArgumentCaptor.forClass(PaymentHistory.class);
        verify(histories).execute(history.capture());
        assertThat(history.getValue().type()).isEqualTo(TransactionType.CANCEL);
        assertThat(history.getValue().occurredAt()).isEqualTo(capture.getValue().canceledAt());
    }

    @Test
    void 전체취소는_쿠폰이_없으면_복원을_생략하고_부분취소된_주문은_거부한다() {
        final Order order = order(3);
        prepare(order, -1L);
        full().execute(new CancelAction("order"));
        verifyNoInteractions(coupons);
        ReflectionTestUtils.setField(order, "status", OrderStatus.PARTIAL_CANCELED);
        assertThatThrownBy(() -> full().execute(new CancelAction("order"))).isInstanceOf(CoreException.class);
    }

    @Test
    void 쿠폰_복원_실패는_전파하고_후속_포인트와_취소기록은_실행하지_않는다() {
        prepare(order(1), 10L);
        doThrow(new CoreException(ErrorCode.OWNED_COUPON_INVALID_USAGE)).when(coupons).execute(10L);
        assertThatThrownBy(() -> full().execute(new CancelAction("order"))).isInstanceOf(CoreException.class);
        verifyNoInteractions(earn, deduct, histories);
    }

    private CancelPaymentUseCase full() {
        return new CancelPaymentUseCase(orders, payments, balances, coupons, earn, deduct, cancels, histories);
    }

    private CancelBalance prepare(final Order order, final Long ownedCouponId) {
        final Payment payment = new Payment(1L, 2L, BigDecimal.valueOf(10000), ownedCouponId, BigDecimal.valueOf(1000),
            BigDecimal.valueOf(1200), 9L, BigDecimal.valueOf(7800), PaymentState.READY);
        ReflectionTestUtils.setField(payment, "id", 3L);
        payment.success("test-pg", PaymentMethod.CARD, "test");
        final CancelBalance balance = new CancelBalance(2L, BigDecimal.valueOf(7800), BigDecimal.valueOf(1200), BigDecimal.valueOf(1000),
            BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        when(orders.execute("order")).thenReturn(order);
        when(payments.execute(2L)).thenReturn(payment);
        when(balances.findByOrderId(2L)).thenReturn(Optional.of(balance));
        when(cancels.save(any())).thenAnswer((final InvocationOnMock invocation) -> {
            final Cancel cancel = invocation.getArgument(0);
            ReflectionTestUtils.setField(cancel, "id", 5L);
            return cancel;
        });
        return balance;
    }

    private static Order order(final int quantity) {
        final Order order = Order.create(1L, "order", null);
        ReflectionTestUtils.setField(order, "id", 2L);
        order.addItem(10L, "책", "", "저자", BigDecimal.valueOf(1000), BigDecimal.valueOf(1000), quantity);
        ReflectionTestUtils.setField(order.items().getFirst(), "id", 4L);
        order.paid();
        return order;
    }
}
