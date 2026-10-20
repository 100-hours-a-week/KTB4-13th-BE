package com.book.core.cancel.application.usecase;

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
import com.book.core.cancel.domain.CancelCalculated;
import com.book.core.cancel.domain.PartialCancelAction;
import com.book.core.cancel.domain.enums.CancelType;
import com.book.core.coupon.application.usecase.RevertOwnedCouponUseCase;
import com.book.core.order.application.usecase.GetOrderForCancelUseCase;
import com.book.core.order.application.usecase.GetOrderItemForCancelUseCase;
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
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.springframework.test.util.ReflectionTestUtils;

class CancelUseCasesTest {
    private final GetOrderForCancelUseCase orders = mock(GetOrderForCancelUseCase.class);
    private final GetOrderItemForCancelUseCase items = mock(GetOrderItemForCancelUseCase.class);
    private final GetPaymentUseCase payments = mock(GetPaymentUseCase.class);
    private final CancelBalanceRepositoryPort balances = mock(CancelBalanceRepositoryPort.class);
    private final CancelRepositoryPort cancels = mock(CancelRepositoryPort.class);
    private final RevertOwnedCouponUseCase coupons = mock(RevertOwnedCouponUseCase.class);
    private final EarnPointUseCase earn = mock(EarnPointUseCase.class);
    private final DeductPointUseCase deduct = mock(DeductPointUseCase.class);
    private final RecordPaymentHistoryUseCase histories = mock(RecordPaymentHistoryUseCase.class);
    private final Clock clock = Clock.fixed(Instant.EPOCH, ZoneId.of("Asia/Seoul"));

    @Test
    void 전체취소는_쿠폰과_포인트를_복원하고_적립포인트를_회수하며_이력을_기록한다() {
        final Order order = order(3);
        final CancelBalance balance = prepare(order, 10L);
        final OffsetDateTime pgCanceledAt = OffsetDateTime.parse("2026-10-20T01:00:00Z");
        assertThat(full().execute(new CancelAction("order"),
            new com.book.core.payment.application.result.CanceledPaymentResult("cancel-key", pgCanceledAt))).isEqualTo(5L);
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
        assertThat(capture.getValue().canceledAt()).isEqualTo(LocalDateTime.parse("2026-10-20T10:00:00"));
        final ArgumentCaptor<PaymentHistory> history = ArgumentCaptor.forClass(PaymentHistory.class);
        verify(histories).execute(history.capture());
        assertThat(history.getValue().type()).isEqualTo(TransactionType.CANCEL);
        assertThat(history.getValue().occurredAt()).isEqualTo(capture.getValue().canceledAt());
    }

    @Test
    void 전체취소는_쿠폰이_없으면_복원을_생략하고_부분취소된_주문은_거부한다() {
        final Order order = order(3);
        prepare(order, -1L);
        full().execute(new CancelAction("order"),
            new com.book.core.payment.application.result.CanceledPaymentResult("cancel-key", java.time.OffsetDateTime.now()));
        verifyNoInteractions(coupons);
        order.partialCanceled();
        assertThatThrownBy(() -> full().execute(new CancelAction("order"),
            new com.book.core.payment.application.result.CanceledPaymentResult("cancel-key", java.time.OffsetDateTime.now())))
            .isInstanceOf(CoreException.class);
    }

    @Test
    void 첫_부분취소는_적립포인트를_회수하고_상품과_주문을_부분취소로_전환한다() {
        final Order order = order(3);
        prepare(order, -1L);
        final CancelCalculated calculated = new CancelCalculated(BigDecimal.valueOf(1000), BigDecimal.ZERO, BigDecimal.ZERO, false);
        final OffsetDateTime pgCanceledAt = OffsetDateTime.parse("2026-10-20T01:00:00Z");
        partial().execute(new PartialCancelAction("order", 4L, 1L), calculated,
            new com.book.core.payment.application.result.CanceledPaymentResult("cancel-key", pgCanceledAt));
        assertThat(order.status()).isEqualTo(OrderStatus.PARTIAL_CANCELED);
        assertThat(order.items().getFirst().canceledQuantity()).isEqualTo(1);
        verify(deduct).execute(1L, PointType.PAYMENT, 3L, PointAmount.PAYMENT);
        verifyNoInteractions(coupons, earn);
        final ArgumentCaptor<Cancel> capture = ArgumentCaptor.forClass(Cancel.class);
        verify(cancels).save(capture.capture());
        assertThat(capture.getValue().type()).isEqualTo(CancelType.PARTIAL);
        assertThat(capture.getValue().orderItemId()).isEqualTo(4L);
        assertThat(capture.getValue().canceledPaidAmount()).isEqualByComparingTo("1000");
        assertThat(capture.getValue().canceledAt()).isEqualTo(LocalDateTime.parse("2026-10-20T10:00:00"));
    }

    @Test
    void 후속_부분취소는_적립포인트를_다시_회수하지_않고_마지막_수량이면_주문을_전체취소한다() {
        final Order order = order(1);
        prepare(order, 10L);
        when(cancels.countByOrderId(2L)).thenReturn(1L);
        final CancelCalculated calculated = new CancelCalculated(BigDecimal.ZERO, BigDecimal.valueOf(1000), BigDecimal.valueOf(100), true);
        partial().execute(new PartialCancelAction("order", 4L, 1L), calculated,
            new com.book.core.payment.application.result.CanceledPaymentResult("cancel-key", java.time.OffsetDateTime.now()));
        assertThat(order.status()).isEqualTo(OrderStatus.CANCELED);
        verify(coupons).execute(10L);
        verify(earn).execute(1L, PointType.PAYMENT, 3L, BigDecimal.valueOf(100));
        verifyNoInteractions(deduct);
    }

    @Test
    void 쿠폰_복원_실패는_전파하고_후속_포인트와_취소기록은_실행하지_않는다() {
        prepare(order(1), 10L);
        doThrow(new CoreException(ErrorCode.OWNED_COUPON_INVALID_USAGE)).when(coupons).execute(10L);
        assertThatThrownBy(() -> full().execute(new CancelAction("order"),
            new com.book.core.payment.application.result.CanceledPaymentResult("cancel-key", java.time.OffsetDateTime.now())))
            .isInstanceOf(CoreException.class);
        verifyNoInteractions(earn, deduct, histories);
    }

    @Test
    void 검증은_주문_소유자_결제상태_상품소속과_잔여수량을_확인한다() {
        final Order order = order(3);
        prepare(order, -1L);
        final ValidateCancelUseCase validator = new ValidateCancelUseCase(orders, payments, items);
        validator.validate(1L, new CancelAction("order"));
        validator.validatePartial(1L, new PartialCancelAction("order", 4L, 3L));
        assertThatThrownBy(() -> validator.validate(9L, new CancelAction("order"))).isInstanceOf(CoreException.class);
        assertThatThrownBy(() -> validator.validatePartial(9L, new PartialCancelAction("order", 4L, 1L))).isInstanceOf(CoreException.class);
        assertThatThrownBy(() -> validator.validatePartial(1L, new PartialCancelAction("order", 4L, 4L))).isInstanceOf(CoreException.class);
        final Order other = order(1);
        ReflectionTestUtils.setField(other, "id", 99L);
        when(items.execute(4L)).thenReturn(other.items().getFirst());
        assertThatThrownBy(() -> validator.validatePartial(1L, new PartialCancelAction("order", 4L, 1L))).isInstanceOf(CoreException.class);
    }

    @Test
    void 결제_READY와_취소된_주문은_거부하고_부분취소_상태는_부분취소만_허용한다() {
        final Order order = order(3);
        prepare(order, -1L);
        final ValidateCancelUseCase validator = new ValidateCancelUseCase(orders, payments, items);
        order.partialCanceled();
        validator.validatePartial(1L, new PartialCancelAction("order", 4L, 1L));
        assertThatThrownBy(() -> validator.validate(1L, new CancelAction("order"))).isInstanceOf(CoreException.class);
        when(payments.execute("order"))
            .thenReturn(new Payment(1L, 2L, BigDecimal.TEN, -1L, BigDecimal.ZERO, BigDecimal.ZERO, 1L, BigDecimal.TEN, PaymentState.READY));
        assertThatThrownBy(() -> validator.validatePartial(1L, new PartialCancelAction("order", 4L, 1L))).isInstanceOf(CoreException.class);
        order.canceled();
        assertThatThrownBy(() -> validator.validatePartial(1L, new PartialCancelAction("order", 4L, 1L))).isInstanceOf(CoreException.class);
    }

    private CancelPaymentUseCase full() {
        return new CancelPaymentUseCase(orders, payments, balances, coupons, earn, deduct, cancels, histories, clock);
    }

    private PartialCancelPaymentUseCase partial() {
        return new PartialCancelPaymentUseCase(orders, payments, balances, coupons, earn, deduct, cancels, histories, items, clock);
    }

    private CancelBalance prepare(final Order order, final Long ownedCouponId) {
        final Payment payment = new Payment(1L, 2L, BigDecimal.valueOf(10000), ownedCouponId, BigDecimal.valueOf(1000),
            BigDecimal.valueOf(1200), 9L, BigDecimal.valueOf(7800), PaymentState.READY);
        ReflectionTestUtils.setField(payment, "id", 3L);
        payment.success("test-pg", PaymentMethod.CARD, "test");
        final CancelBalance balance = new CancelBalance(2L, BigDecimal.valueOf(7800), BigDecimal.valueOf(1200), BigDecimal.valueOf(1000),
            BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        when(orders.execute("order")).thenReturn(order);
        when(items.execute(4L)).thenReturn(order.items().getFirst());
        when(payments.execute("order")).thenReturn(payment);
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
