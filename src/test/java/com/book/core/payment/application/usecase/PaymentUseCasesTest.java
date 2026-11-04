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
import com.book.core.payment.application.port.PaymentAttemptRepositoryPort;
import com.book.core.payment.application.port.PgOperationRepositoryPort;
import com.book.core.order.application.usecase.LockPaymentOrderUseCase;
import com.book.core.payment.domain.PaymentAttempt;
import com.book.core.payment.domain.PaymentAttemptState;
import com.book.core.point.application.usecase.ReservePaymentPointsUseCase;
import com.book.core.point.application.usecase.ConfirmPaymentPointsUseCase;
import java.time.LocalDateTime;
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
    private final ConfirmPaymentPointsUseCase confirm = mock(ConfirmPaymentPointsUseCase.class);
    private final ReservePaymentPointsUseCase reserve = mock(ReservePaymentPointsUseCase.class);
    private final LockPaymentOrderUseCase lockOrder = mock(LockPaymentOrderUseCase.class);
    private final ValidatePaymentAttemptUseCase validateAttempt = mock(ValidatePaymentAttemptUseCase.class);
    private final PaymentAttemptRepositoryPort attempts = mock(PaymentAttemptRepositoryPort.class);
    private final PgOperationRepositoryPort operations = mock(PgOperationRepositoryPort.class);
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
        when(lockOrder.execute("order")).thenReturn(order);
        when(attempts.save(any())).thenAnswer((final InvocationOnMock invocation) -> {
            final PaymentAttempt attempt = invocation.getArgument(0);
            ReflectionTestUtils.setField(attempt, "id", 4L);
            return attempt;
        });
        assertThat(new CreatePaymentUseCase(payments, attempts, lockOrder, reserve, operations)
            .execute(order, 99L, PaymentDiscount.EMPTY, "request-hash", "hash").paymentId()).isEqualTo(3L);
        verify(reserve).execute(1L, 4L, BigDecimal.ZERO, 3L);
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
        when(lockOrder.execute("order")).thenReturn(order());
        assertThatThrownBy(() -> new CreatePaymentUseCase(payments, attempts, lockOrder, reserve, operations).execute(order(), 1L,
            PaymentDiscount.EMPTY, "request-hash", "hash")).isInstanceOfSatisfying(CoreException.class,
                (final CoreException e) -> assertThat(e.errorCode()).isEqualTo(ErrorCode.ORDER_ALREADY_PAID));
    }

    @Test
    void 동일한_시도_요청은_포인트를_중복_예약하지_않는다() {
        final Order order = order();
        final Payment payment = payment(-1L);
        final PaymentAttempt attempt = new PaymentAttempt(3L, "order", "payment_old", "request-hash", "hash");
        ReflectionTestUtils.setField(attempt, "id", 4L);
        when(lockOrder.execute("order")).thenReturn(order);
        when(payments.findByOrderId(2L)).thenReturn(Optional.of(payment));
        when(attempts.findLatestByPaymentId(3L)).thenReturn(Optional.of(attempt));
        when(attempts.findByRequestHash("request-hash")).thenReturn(Optional.of(attempt));

        final var result = new CreatePaymentUseCase(payments, attempts, lockOrder, reserve, operations).execute(order, 1L,
            PaymentDiscount.EMPTY, "request-hash", "hash");
        assertThat(result.paymentId()).isEqualTo(3L);
        assertThat(result.pgOrderId()).isEqualTo("payment_old");
        verifyNoInteractions(reserve);
    }

    @Test
    void 다른_입력의_중복_생성과_기한_지난_주문은_거부한다() {
        final Order order = order();
        final Payment payment = payment(-1L);
        final PaymentAttempt attempt = new PaymentAttempt(3L, "order", "payment_old", "request-hash", "hash");
        ReflectionTestUtils.setField(attempt, "id", 4L);
        when(lockOrder.execute("order")).thenReturn(order);
        when(payments.findByOrderId(2L)).thenReturn(Optional.of(payment));
        when(attempts.findLatestByPaymentId(3L)).thenReturn(Optional.of(attempt));
        when(attempts.findByRequestHash("request-hash")).thenReturn(Optional.of(attempt));
        final CreatePaymentUseCase useCase = new CreatePaymentUseCase(payments, attempts, lockOrder, reserve, operations);

        assertThatThrownBy(() -> useCase.execute(order, 1L, PaymentDiscount.EMPTY, "request-hash", "other")).isInstanceOfSatisfying(
            CoreException.class, (final CoreException error) -> assertThat(error.errorCode()).isEqualTo(ErrorCode.PG_REQUEST_CONFLICT));
        ReflectionTestUtils.setField(order, "expiresAt", LocalDateTime.now().minusSeconds(1));
        assertThatThrownBy(() -> useCase.findExisting("order", 1L, "request-hash", "hash")).isInstanceOf(CoreException.class);
        verifyNoInteractions(reserve);
    }

    @Test
    void 이전_실패_시도의_늦은_콜백은_최신_시도로_검증되지_않는다() {
        final Order order = order();
        final Payment payment = payment(-1L);
        final PaymentAttempt latest = new PaymentAttempt(3L, "order", "payment_new", "request-hash", "hash");
        ReflectionTestUtils.setField(latest, "id", 5L);
        when(lockOrder.execute("order")).thenReturn(order);
        when(payments.findByOrderId(2L)).thenReturn(Optional.of(payment));
        when(attempts.findLatestByPaymentId(3L)).thenReturn(Optional.of(latest));
        final ValidatePaymentAttemptUseCase useCase = new ValidatePaymentAttemptUseCase(lockOrder, payments, attempts);

        assertThatThrownBy(() -> useCase.execute("order", 4L)).isInstanceOf(CoreException.class);
        useCase.execute("order", 5L);
        assertThat(latest.state()).isEqualTo(PaymentAttemptState.READY);
    }

    @Test
    void 최종_실패_후_이전_키_재전송은_새_예약을_만들지_않고_새_키만_재시도한다() {
        final Order order = order();
        final Payment payment = payment(-1L);
        final PaymentAttempt old = new PaymentAttempt(3L, "order", "payment_old", "old-key", "hash");
        ReflectionTestUtils.setField(old, "id", 4L);
        old.fail();
        when(lockOrder.execute("order")).thenReturn(order);
        when(payments.findByOrderId(2L)).thenReturn(Optional.of(payment));
        when(attempts.findByRequestHash("old-key")).thenReturn(Optional.of(old));
        when(attempts.findLatestByPaymentId(3L)).thenReturn(Optional.of(old));
        when(attempts.save(any())).thenAnswer((final InvocationOnMock invocation) -> {
            final PaymentAttempt attempt = invocation.getArgument(0);
            ReflectionTestUtils.setField(attempt, "id", 5L);
            return attempt;
        });
        final CreatePaymentUseCase useCase = new CreatePaymentUseCase(payments, attempts, lockOrder, reserve, operations);

        assertThatThrownBy(() -> useCase.execute(order, 1L, PaymentDiscount.EMPTY, "old-key", "hash")).isInstanceOfSatisfying(
            CoreException.class, (final CoreException error) -> assertThat(error.errorCode()).isEqualTo(ErrorCode.PAYMENT_INVALID_STATE));
        verifyNoInteractions(reserve);
        final var retry = useCase.execute(order, 1L, PaymentDiscount.EMPTY, "new-key", "hash");
        assertThat(retry.pgOrderId()).isNotEqualTo("payment_old");
        verify(reserve).execute(1L, 5L, BigDecimal.ZERO, 3L);
    }

    @Test
    void 승인_금액은_스케일_차이를_허용하고_다른_금액은_거부한다() {
        when(payments.findByOrderId(2L)).thenReturn(Optional.of(payment(-1L)));
        final ValidatePaymentUseCase validator = new ValidatePaymentUseCase(payments);
        validator.validateForApprove(2L, BigDecimal.valueOf(8800));
        validator.validateForApprove(2L, new BigDecimal("8800.00"));
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
        assertThat(complete().execute("order", 4L, new com.book.core.payment.application.result.ApprovedPaymentResult("test-external",
            com.book.core.payment.domain.enums.PaymentMethod.CARD, "12345678"))).isEqualTo(3L);
        assertThat(order.status()).isEqualTo(OrderStatus.PAID);
        assertThat(order.items()).allSatisfy((final OrderItem item) -> assertThat(item.status()).isEqualTo(OrderItemStatus.PAID));
        assertThat(payment.state()).isEqualTo(PaymentState.SUCCESS);
        verify(coupons).execute(10L);
        verify(confirm).execute(1L, 4L);
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
        complete().execute("order", 4L, new com.book.core.payment.application.result.ApprovedPaymentResult("test-external",
            com.book.core.payment.domain.enums.PaymentMethod.CARD, "12345678"));
        verifyNoInteractions(coupons);
    }

    @Test
    void 혜택_처리_실패는_전파하고_후속_거래이력은_저장하지_않는다() {
        when(orders.execute("order", OrderStatus.CREATED)).thenReturn(order());
        when(payments.findByOrderId(2L)).thenReturn(Optional.of(payment(10L)));
        doThrow(new CoreException(ErrorCode.OWNED_COUPON_INVALID_USAGE)).when(coupons).execute(10L);
        assertThatThrownBy(
            () -> complete().execute("order", 4L, new com.book.core.payment.application.result.ApprovedPaymentResult("test-external",
                com.book.core.payment.domain.enums.PaymentMethod.CARD, "12345678")))
            .isInstanceOf(CoreException.class);
        verifyNoInteractions(confirm, earn, cancel, histories);
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
        final PaymentAttempt attempt = new PaymentAttempt(3L, "order", "payment_attempt", "request-hash", "hash");
        ReflectionTestUtils.setField(attempt, "id", 4L);
        when(attempts.findById(4L)).thenReturn(Optional.of(attempt));
        return new CompletePaymentUseCase(payments, orders, coupons, confirm, validateAttempt, attempts, earn, cancel, histories);
    }

    private static Order order() {
        final Order order = Order.create(1L, "order", null);
        ReflectionTestUtils.setField(order, "id", 2L);
        order.reserveUntil(LocalDateTime.now().plusMinutes(10));
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
