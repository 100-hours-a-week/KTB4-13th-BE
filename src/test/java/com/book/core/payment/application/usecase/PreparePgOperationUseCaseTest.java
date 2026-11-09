package com.book.core.payment.application.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.cancel.application.usecase.CalculatePartialCancelUseCase;
import com.book.core.cancel.application.usecase.GetCancelBalanceUseCase;
import com.book.core.cancel.domain.CancelBalance;
import com.book.core.cancel.domain.CancelCalculated;
import com.book.core.cancel.domain.PartialCancelAction;
import com.book.core.order.application.usecase.LockPaymentOrderUseCase;
import com.book.core.order.domain.Order;
import com.book.core.payment.application.command.PreparePgOperationCommand;
import com.book.core.payment.application.command.ApprovePaymentCommand;
import com.book.core.payment.application.port.PgOperationRepositoryPort;
import com.book.core.payment.application.port.PaymentRepositoryPort;
import com.book.core.payment.application.port.PaymentAttemptRepositoryPort;
import com.book.core.payment.domain.Payment;
import com.book.core.payment.domain.PaymentAttempt;
import com.book.core.payment.domain.PaymentAttemptState;
import com.book.core.payment.domain.enums.PaymentState;
import com.book.core.payment.domain.PgOperation;
import com.book.core.payment.domain.PgOperationType;
import java.util.Optional;
import java.util.HashMap;
import java.util.Map;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class PreparePgOperationUseCaseTest {
    private final LockPaymentOrderUseCase lock = mock(LockPaymentOrderUseCase.class);
    private final PgOperationRepositoryPort operations = mock(PgOperationRepositoryPort.class);
    private final PaymentRepositoryPort payments = mock(PaymentRepositoryPort.class);
    private final PaymentAttemptRepositoryPort attempts = mock(PaymentAttemptRepositoryPort.class);
    private final CalculatePartialCancelUseCase calculatePartialCancel = mock(CalculatePartialCancelUseCase.class);
    private final GetCancelBalanceUseCase getCancelBalance = mock(GetCancelBalanceUseCase.class);
    private final PreparePgOperationUseCase useCase = new PreparePgOperationUseCase(lock, operations, mock(ValidatePaymentUseCase.class),
        mock(ValidatePaymentAttemptUseCase.class), payments, attempts,
        mock(com.book.core.cancel.application.usecase.ValidateCancelUseCase.class), calculatePartialCancel, getCancelBalance);
    private final PreparePgOperationCommand command =
        new PreparePgOperationCommand("order", 1L, PgOperationType.CANCEL, "request", "input", null, null, 0);

    @Test
    void 주문을_잠근_후_요청을_저장한다() {
        when(lock.execute("order")).thenReturn(Order.create(1L, "order", null));
        when(operations.findByRequestHash(command.requestHash())).thenReturn(Optional.empty());
        final Payment payment = mock(Payment.class);
        when(payment.usedPoint()).thenReturn(BigDecimal.ZERO);
        when(payments.findByOrderId(null)).thenReturn(Optional.of(payment));
        when(getCancelBalance.execute(null)).thenReturn(balance(BigDecimal.TEN));
        useCase.execute(command);
        verify(operations).save(any());
    }

    @Test
    void 전체취소_복원포인트가_소수이면_미확정_PG작업을_만들지_않는다() {
        when(lock.execute("order")).thenReturn(Order.create(1L, "order", null));
        final Payment payment = mock(Payment.class);
        when(payment.usedPoint()).thenReturn(new BigDecimal("0.5"));
        when(payments.findByOrderId(null)).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> useCase.execute(command)).isInstanceOf(CoreException.class);
        verify(operations, never()).save(any());
    }

    @Test
    void 부분취소_복원포인트가_소수이면_미확정_PG작업을_만들지_않는다() {
        final PreparePgOperationCommand partial =
            new PreparePgOperationCommand("order", 1L, PgOperationType.PARTIAL_CANCEL, "partial", "input", null, 2L, 1);
        when(lock.execute("order")).thenReturn(Order.create(1L, "order", null));
        when(calculatePartialCancel.execute(new PartialCancelAction("order", 2L, 1)))
            .thenReturn(new CancelCalculated(BigDecimal.ONE, BigDecimal.ZERO, new BigDecimal("0.5"), false));

        assertThatThrownBy(() -> useCase.execute(partial)).isInstanceOf(CoreException.class);
        verify(operations, never()).save(any());
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"10.50", "-1"})
    void 전체취소_현금환불이_소수이거나_음수이면_PG작업을_만들지_않는다(final String amount) {
        when(lock.execute("order")).thenReturn(Order.create(1L, "order", null));
        final Payment payment = mock(Payment.class);
        when(payment.usedPoint()).thenReturn(BigDecimal.ZERO);
        when(payments.findByOrderId(null)).thenReturn(Optional.of(payment));
        when(getCancelBalance.execute(null)).thenReturn(balance(new BigDecimal(amount)));

        assertThatThrownBy(() -> useCase.execute(command)).isInstanceOfSatisfying(CoreException.class,
            (final CoreException exception) -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.PAYMENT_INVALID_AMOUNT));
        verify(operations, never()).save(any());
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"10.50", "-1"})
    void 부분취소_현금환불이_소수이거나_음수이면_PG작업을_만들지_않는다(final String amount) {
        final PreparePgOperationCommand partial =
            new PreparePgOperationCommand("order", 1L, PgOperationType.PARTIAL_CANCEL, "partial", "input", null, 2L, 1);
        when(lock.execute("order")).thenReturn(Order.create(1L, "order", null));
        when(calculatePartialCancel.execute(new PartialCancelAction("order", 2L, 1)))
            .thenReturn(new CancelCalculated(new BigDecimal(amount), BigDecimal.ZERO, BigDecimal.ZERO, false));
        when(getCancelBalance.execute(null)).thenReturn(balance(BigDecimal.valueOf(100)));

        assertThatThrownBy(() -> useCase.execute(partial)).isInstanceOfSatisfying(CoreException.class,
            (final CoreException exception) -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.PAYMENT_INVALID_AMOUNT));
        verify(operations, never()).save(any());
    }

    @Test
    void 같은_요청의_재전송은_새로_저장하지_않는다() {
        when(lock.execute("order")).thenReturn(Order.create(1L, "order", null));
        when(operations.findByRequestHash(command.requestHash()))
            .thenReturn(Optional.of(new PgOperation("order", command.requestHash(), command.inputHash(), PgOperationType.CANCEL)));
        useCase.execute(command);
        verify(operations, never()).save(any());
    }

    @Test
    void 미완료_요청이_있으면_다른_취소를_차단한다() {
        when(lock.execute("order")).thenReturn(Order.create(1L, "order", null));
        when(operations.findByRequestHash(command.requestHash())).thenReturn(Optional.empty());
        when(operations.existsPending("order")).thenReturn(true);
        assertThatThrownBy(() -> useCase.execute(command)).isInstanceOf(CoreException.class);
        verify(operations, never()).save(any());
    }

    @Test
    void 다른_사용자의_주문에는_요청을_저장하지_않는다() {
        when(lock.execute("order")).thenReturn(Order.create(2L, "order", null));
        assertThatThrownBy(() -> useCase.execute(command)).isInstanceOf(CoreException.class);
        verify(operations, never()).save(any());
    }

    @Test
    void 기한_후_처음_도착한_PG콜백은_운영확인_상태로_영속화한다() {
        final Order order = expiredOrder();
        final PreparePgOperationCommand late = new ApprovePaymentCommand("order", "pg-order", "key", BigDecimal.TEN, 1L).operation();
        when(lock.execute("order")).thenReturn(order);
        when(operations.findByRequestHash(late.requestHash())).thenReturn(Optional.empty());

        useCase.execute(late);

        final ArgumentCaptor<PgOperation> saved = ArgumentCaptor.forClass(PgOperation.class);
        verify(operations).save(saved.capture());
        assertThat(saved.getValue().reviewRequired()).isTrue();
        assertThat(saved.getValue().uncertainAt()).isNotNull();
        assertThat(saved.getValue().pgOrderId()).isEqualTo("pg-order");
        assertThat(saved.getValue().attemptId()).isEqualTo(1L);
        assertThat(saved.getValue().completed()).isFalse();
        assertThat(saved.getValue().rejected()).isFalse();
    }

    @Test
    void 기한_후_처음_도착한_영원결제는_PG작업을_만들지_않는다() {
        final PreparePgOperationCommand late = new ApprovePaymentCommand("order", "pg-order", "", BigDecimal.ZERO, 1L).operation();
        when(lock.execute("order")).thenReturn(expiredOrder());
        when(operations.findByRequestHash(late.requestHash())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(late)).isInstanceOf(CoreException.class);

        verify(operations, never()).save(any());
    }

    @Test
    void 주문만료가_먼저_확정된_뒤의_PG콜백도_운영확인으로_보존한다() {
        final Order order = expiredOrder();
        order.expire(LocalDateTime.now());
        final PreparePgOperationCommand late = new ApprovePaymentCommand("order", "pg-order", "key", BigDecimal.TEN, 1L).operation();
        when(lock.execute("order")).thenReturn(order);
        when(operations.findByRequestHash(late.requestHash())).thenReturn(Optional.empty());

        useCase.execute(late);

        final ArgumentCaptor<PgOperation> saved = ArgumentCaptor.forClass(PgOperation.class);
        verify(operations).save(saved.capture());
        assertThat(saved.getValue().reviewRequired()).isTrue();
        assertThat(saved.getValue().completed()).isFalse();
        assertThat(saved.getValue().blocksOrder()).isFalse();
    }

    @Test
    void 만료_후_이미_결제완료된_주문의_콜백은_별도_검토하고_취소를_막지_않는다() {
        final Order order = expiredOrder();
        order.paid();
        final PreparePgOperationCommand late = new ApprovePaymentCommand("order", "pg-order", "key", BigDecimal.TEN, 1L).operation();
        when(lock.execute("order")).thenReturn(order);
        when(operations.findByRequestHash(late.requestHash())).thenReturn(Optional.empty());

        useCase.execute(late);

        final ArgumentCaptor<PgOperation> saved = ArgumentCaptor.forClass(PgOperation.class);
        verify(operations).save(saved.capture());
        assertThat(saved.getValue().reviewRequired()).isTrue();
        assertThat(saved.getValue().blocksOrder()).isFalse();
    }

    @Test
    void 만료_후_과거_시도의_콜백은_새로운_시도를_막지_않는다() {
        final Order order = expiredOrder();
        final PreparePgOperationCommand late = new ApprovePaymentCommand("order", "old-pg", "key", BigDecimal.TEN, 1L).operation();
        final Payment payment = mock(Payment.class);
        final PaymentAttempt latest = mock(PaymentAttempt.class);
        when(lock.execute("order")).thenReturn(order);
        when(operations.findByRequestHash(late.requestHash())).thenReturn(Optional.empty());
        when(payments.findByOrderId(order.id())).thenReturn(Optional.of(payment));
        when(payment.state()).thenReturn(PaymentState.READY);
        when(payment.paidAmount()).thenReturn(BigDecimal.TEN);
        when(payment.id()).thenReturn(100L);
        when(attempts.findLatestByPaymentId(100L)).thenReturn(Optional.of(latest));
        when(latest.id()).thenReturn(2L);

        useCase.execute(late);

        final ArgumentCaptor<PgOperation> saved = ArgumentCaptor.forClass(PgOperation.class);
        verify(operations).save(saved.capture());
        assertThat(saved.getValue().reviewRequired()).isTrue();
        assertThat(saved.getValue().blocksOrder()).isFalse();
    }

    @Test
    void 만료_전이어도_금액이_다른_콜백은_현재_주문을_막지_않는다() {
        final Order order = Order.create(1L, "order", null);
        order.reserveUntil(LocalDateTime.now().plusMinutes(5));
        final PreparePgOperationCommand wrongAmount = new ApprovePaymentCommand("order", "pg-order", "key", BigDecimal.ONE, 1L).operation();
        setupCurrentAttempt(order);
        when(lock.execute("order")).thenReturn(order);
        when(operations.findByRequestHash(wrongAmount.requestHash())).thenReturn(Optional.empty());

        useCase.execute(wrongAmount);

        final ArgumentCaptor<PgOperation> saved = ArgumentCaptor.forClass(PgOperation.class);
        verify(operations).save(saved.capture());
        assertThat(saved.getValue().reviewRequired()).isTrue();
        assertThat(saved.getValue().blocksOrder()).isFalse();
    }

    @Test
    void 만료_후_현재_준비중인_시도의_콜백은_예약을_보호한다() {
        final Order order = expiredOrder();
        final PreparePgOperationCommand late = new ApprovePaymentCommand("order", "pg-order", "key", BigDecimal.TEN, 1L).operation();
        final Payment payment = mock(Payment.class);
        final PaymentAttempt latest = mock(PaymentAttempt.class);
        when(lock.execute("order")).thenReturn(order);
        when(operations.findByRequestHash(late.requestHash())).thenReturn(Optional.empty());
        when(payments.findByOrderId(order.id())).thenReturn(Optional.of(payment));
        when(payment.state()).thenReturn(PaymentState.READY);
        when(payment.paidAmount()).thenReturn(BigDecimal.TEN);
        when(payment.id()).thenReturn(100L);
        when(attempts.findLatestByPaymentId(100L)).thenReturn(Optional.of(latest));
        when(latest.id()).thenReturn(1L);
        when(latest.state()).thenReturn(PaymentAttemptState.READY);
        when(latest.orderKey()).thenReturn("order");
        when(latest.pgOrderId()).thenReturn("pg-order");

        useCase.execute(late);

        final ArgumentCaptor<PgOperation> saved = ArgumentCaptor.forClass(PgOperation.class);
        verify(operations).save(saved.capture());
        assertThat(saved.getValue().reviewRequired()).isTrue();
        assertThat(saved.getValue().blocksOrder()).isTrue();
    }

    @Test
    void 실패콜백_후_같은_시도의_성공콜백은_기존_확인작업에_결제키만_연결한다() {
        final Order order = Order.create(1L, "order", null);
        order.reserveUntil(LocalDateTime.now().plusMinutes(5));
        final PreparePgOperationCommand success = new ApprovePaymentCommand("order", "pg-order", "key", BigDecimal.TEN, 1L).operation();
        final PgOperation unresolved = unidentified(1L, "pg-order", BigDecimal.TEN);
        final Instant firstUncertain = unresolved.uncertainAt();
        final String pgIdempotencyKey = unresolved.pgIdempotencyKey();
        setupCurrentAttempt(order);
        when(lock.execute("order")).thenReturn(order);
        when(operations.findByRequestHash(success.requestHash())).thenReturn(Optional.empty(), Optional.of(unresolved));
        when(operations.findByRequestHash(unidentifiedHash(1L))).thenReturn(Optional.of(unresolved));

        useCase.execute(success);
        useCase.execute(success);

        assertThat(unresolved.externalPaymentKey()).isEqualTo("key");
        assertThat(unresolved.requestHash()).isEqualTo(success.requestHash());
        assertThat(unresolved.inputHash()).isEqualTo(success.inputHash());
        assertThat(unresolved.pgIdempotencyKey()).isEqualTo(pgIdempotencyKey);
        assertThat(unresolved.uncertainAt()).isEqualTo(firstUncertain);
        assertThat(unresolved.reviewRequired()).isTrue();
        assertThat(unresolved.blocksOrder()).isTrue();
        assertThat(unresolved.completed()).isFalse();
        assertThat(unresolved.rejected()).isFalse();
        verify(operations, never()).save(any());
    }

    @Test
    void 미확인_작업의_주문번호가_다르면_덮어쓰지_않고_콜백을_격리한다() {
        final Order order = Order.create(1L, "order", null);
        order.reserveUntil(LocalDateTime.now().plusMinutes(5));
        final PreparePgOperationCommand success = new ApprovePaymentCommand("order", "pg-order", "key", BigDecimal.TEN, 1L).operation();
        final PgOperation unresolved = unidentified(1L, "different-pg", BigDecimal.TEN);
        setupCurrentAttempt(order);
        when(lock.execute("order")).thenReturn(order);
        when(operations.findByRequestHash(success.requestHash())).thenReturn(Optional.empty());
        when(operations.findByRequestHash(unidentifiedHash(1L))).thenReturn(Optional.of(unresolved));

        useCase.execute(success);

        assertThat(unresolved.externalPaymentKey()).isNull();
        assertThat(unresolved.requestHash()).isEqualTo(unidentifiedHash(1L));
        assertThat(unresolved.reviewRequired()).isTrue();
        final ArgumentCaptor<PgOperation> saved = ArgumentCaptor.forClass(PgOperation.class);
        verify(operations).save(saved.capture());
        assertThat(saved.getValue().externalPaymentKey()).isEqualTo("key");
        assertThat(saved.getValue().reviewRequired()).isTrue();
        assertThat(saved.getValue().blocksOrder()).isFalse();
    }

    @Test
    void 잘못된_금액_콜백을_격리한_뒤에도_같은_결제키의_정상_콜백을_기존_작업에_연결한다() {
        final Order order = Order.create(1L, "order", null);
        order.reserveUntil(LocalDateTime.now().plusMinutes(5));
        final PreparePgOperationCommand correct = new ApprovePaymentCommand("order", "pg-order", "key", BigDecimal.TEN, 1L).operation();
        final PreparePgOperationCommand incorrect = new ApprovePaymentCommand("order", "pg-order", "key", BigDecimal.ONE, 1L).operation();
        final PgOperation unresolved = unidentified(1L, "pg-order", BigDecimal.TEN);
        final String pgIdempotencyKey = unresolved.pgIdempotencyKey();
        final Map<String, PgOperation> stored = new HashMap<>();
        stored.put(unidentifiedHash(1L), unresolved);
        setupCurrentAttempt(order);
        when(lock.execute("order")).thenReturn(order);
        when(operations.findByRequestHash(any())).thenAnswer(invocation -> Optional.ofNullable(stored.get(invocation.getArgument(0))));
        when(operations.save(any())).thenAnswer(invocation -> {
            final PgOperation saved = invocation.getArgument(0);
            stored.put(saved.requestHash(), saved);
            return saved;
        });

        useCase.execute(incorrect);
        assertThat(stored.get(incorrect.isolatedApprovalHash()).reviewRequired()).isTrue();
        assertThat(stored.get(incorrect.isolatedApprovalHash()).blocksOrder()).isFalse();
        assertThat(stored).doesNotContainKey(correct.requestHash());

        useCase.execute(correct);
        assertThat(unresolved.externalPaymentKey()).isEqualTo("key");
        assertThat(unresolved.requestHash()).isEqualTo(correct.requestHash());
        assertThat(unresolved.pgIdempotencyKey()).isEqualTo(pgIdempotencyKey);
        assertThat(unresolved.reviewRequired()).isTrue();
        stored.remove(unidentifiedHash(1L));
        stored.put(unresolved.requestHash(), unresolved);

        useCase.execute(incorrect);
        assertThat(stored).hasSize(2);
        assertThat(unresolved.inputHash()).isEqualTo(correct.inputHash());
        assertThat(unresolved.reviewRequired()).isTrue();
    }

    @Test
    void 다른_금액이나_결제키의_격리_요청은_각각_독립적으로_식별한다() {
        final PreparePgOperationCommand first = new ApprovePaymentCommand("order", "pg-order", "first-key", BigDecimal.TEN, 1L).operation();
        final PreparePgOperationCommand otherAmount =
            new ApprovePaymentCommand("order", "pg-order", "first-key", BigDecimal.ONE, 1L).operation();
        final PreparePgOperationCommand otherKey =
            new ApprovePaymentCommand("order", "pg-order", "second-key", BigDecimal.TEN, 1L).operation();

        assertThat(first.requestHash()).isEqualTo(otherAmount.requestHash());
        assertThat(first.isolatedApprovalHash()).isNotEqualTo(otherAmount.isolatedApprovalHash());
        assertThat(first.isolatedApprovalHash()).isNotEqualTo(otherKey.isolatedApprovalHash());
        assertThat(first.isolatedApprovalHash()).isEqualTo(first.isolatedApprovalHash());
    }

    private static String unidentifiedHash(final long attemptId) {
        return PgOperation.hash("unidentified-payment-attempt:" + attemptId);
    }

    private static PgOperation unidentified(final long attemptId, final String pgOrderId, final BigDecimal amount) {
        final String hash = unidentifiedHash(attemptId);
        final PgOperation operation = new PgOperation("order", hash, hash, PgOperationType.APPROVE);
        operation.recordUnidentifiedApprovalDetails(amount, pgOrderId, attemptId);
        operation.markUncertain(Instant.now().minusSeconds(1));
        operation.requireReview();
        return operation;
    }

    @Test
    void 기한_전에_시작된_기존_승인작업은_기한_후에도_계속_확인할_수_있다() {
        final PreparePgOperationCommand retry = new ApprovePaymentCommand("order", "pg-order", "key", BigDecimal.TEN, 1L).operation();
        final PgOperation operation = new PgOperation("order", retry.requestHash(), retry.inputHash(), PgOperationType.APPROVE);
        ReflectionTestUtils.setField(operation, "requestedAt", Instant.now().minusSeconds(20));
        final Order order = expiredOrder();
        setupCurrentAttempt(order);
        when(lock.execute("order")).thenReturn(order);
        when(operations.findByRequestHash(retry.requestHash())).thenReturn(Optional.of(operation));

        useCase.execute(retry);

        assertThat(operation.reviewRequired()).isFalse();
        verify(operations, never()).save(any());
    }

    @Test
    void 기한_후_시작된_기존_승인작업도_운영확인으로_전환한다() {
        final PreparePgOperationCommand retry = new ApprovePaymentCommand("order", "pg-order", "key", BigDecimal.TEN, 1L).operation();
        final PgOperation operation = new PgOperation("order", retry.requestHash(), retry.inputHash(), PgOperationType.APPROVE);
        final Order order = expiredOrder();
        setupCurrentAttempt(order);
        when(lock.execute("order")).thenReturn(order);
        when(operations.findByRequestHash(retry.requestHash())).thenReturn(Optional.of(operation));

        useCase.execute(retry);

        assertThat(operation.reviewRequired()).isTrue();
        verify(operations, never()).save(any());
    }

    private static CancelBalance balance(final BigDecimal refundable) {
        return new CancelBalance(1L, refundable, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
    }

    private static Order expiredOrder() {
        final Order order = Order.create(1L, "order", null);
        order.reserveUntil(LocalDateTime.now().minusSeconds(10));
        return order;
    }

    private void setupCurrentAttempt(final Order order) {
        final Payment payment = mock(Payment.class);
        final PaymentAttempt latest = mock(PaymentAttempt.class);
        when(payments.findByOrderId(order.id())).thenReturn(Optional.of(payment));
        when(payment.state()).thenReturn(PaymentState.READY);
        when(payment.paidAmount()).thenReturn(BigDecimal.TEN);
        when(payment.id()).thenReturn(100L);
        when(attempts.findLatestByPaymentId(100L)).thenReturn(Optional.of(latest));
        when(latest.id()).thenReturn(1L);
        when(latest.state()).thenReturn(PaymentAttemptState.READY);
        when(latest.orderKey()).thenReturn("order");
        when(latest.pgOrderId()).thenReturn("pg-order");
    }
}
