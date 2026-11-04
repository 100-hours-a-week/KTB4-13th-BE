package com.book.core.payment.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.book.core.order.application.usecase.LockPaymentOrderUseCase;
import com.book.core.order.domain.Order;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.payment.application.port.PaymentAttemptRepositoryPort;
import com.book.core.payment.application.port.PaymentRepositoryPort;
import com.book.core.payment.application.port.PgOperationRepositoryPort;
import com.book.core.payment.domain.Payment;
import com.book.core.payment.domain.PaymentAttempt;
import com.book.core.payment.domain.PgOperation;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class RecordPaymentFailureCallbackUseCaseTest {
    private final LockPaymentOrderUseCase lock = mock(LockPaymentOrderUseCase.class);
    private final ValidatePaymentAttemptUseCase validateAttempt = mock(ValidatePaymentAttemptUseCase.class);
    private final ValidatePaymentUseCase validatePayment = mock(ValidatePaymentUseCase.class);
    private final FailPaymentUseCase fail = mock(FailPaymentUseCase.class);
    private final PaymentRepositoryPort payments = mock(PaymentRepositoryPort.class);
    private final PaymentAttemptRepositoryPort attempts = mock(PaymentAttemptRepositoryPort.class);
    private final PgOperationRepositoryPort operations = mock(PgOperationRepositoryPort.class);
    private final ReleaseActivePaymentReservationUseCase release = mock(ReleaseActivePaymentReservationUseCase.class);
    private final RecordPaymentFailureCallbackUseCase useCase =
        new RecordPaymentFailureCallbackUseCase(lock, validateAttempt, validatePayment, fail, payments, attempts, operations, release);

    @Test
    void 식별정보_없는_실패_콜백은_예약을_유지하고_운영_확인_상태를_저장한다() {
        final Order order = Order.create(1L, "order", null);
        ReflectionTestUtils.setField(order, "id", 2L);
        final Payment payment = mock(Payment.class);
        final PaymentAttempt attempt = new PaymentAttempt(3L, "order", "payment_external", "request-hash", "input-hash");
        ReflectionTestUtils.setField(attempt, "id", 4L);
        when(lock.execute("order")).thenReturn(order);
        when(payments.findByOrderId(2L)).thenReturn(Optional.of(payment));
        when(payment.paidAmount()).thenReturn(BigDecimal.TEN);
        when(attempts.findById(4L)).thenReturn(Optional.of(attempt));
        when(operations.existsPending("order")).thenReturn(false);

        assertThat(useCase.execute(1L, "order", 4L, "FAIL", "failure")).isTrue();

        final ArgumentCaptor<PgOperation> operation = ArgumentCaptor.forClass(PgOperation.class);
        verify(operations).save(operation.capture());
        assertThat(operation.getValue().reviewRequired()).isTrue();
        assertThat(operation.getValue().uncertainAt()).isNotNull();
        assertThat(operation.getValue().attemptId()).isEqualTo(4L);
        assertThat(operation.getValue().pgOrderId()).isEqualTo("payment_external");
        assertThat(operation.getValue().externalPaymentKey()).isNull();
        verify(validateAttempt).execute("order", 4L);
    }

    @Test
    void 이미_확인_중인_PG_작업이_있으면_새_작업을_중복_생성하지_않는다() {
        final Order order = Order.create(1L, "order", null);
        ReflectionTestUtils.setField(order, "id", 2L);
        when(lock.execute("order")).thenReturn(order);
        when(operations.existsPending("order")).thenReturn(true);

        assertThat(useCase.execute(1L, "order", 4L, "FAIL", "failure")).isTrue();

        verify(operations, never()).save(any());
        verify(validateAttempt).execute("order", 4L);
    }

    @Test
    void 다른_회원의_실패_콜백은_잠금_안에서_거부하고_기록을_변경하지_않는다() {
        final Order order = Order.create(1L, "order", null);
        when(lock.execute("order")).thenReturn(order);

        assertThatThrownBy(() -> useCase.execute(2L, "order", 4L, "FAIL", "failure")).isInstanceOfSatisfying(CoreException.class,
            error -> assertThat(error.errorCode()).isEqualTo(ErrorCode.FORBIDDEN));
        verifyNoInteractions(validateAttempt, validatePayment, fail, payments, attempts, operations, release);
    }

    @Test
    void PG_작업이_없는_0원_결제는_시도_실패와_예약_해제를_함께_처리한다() {
        final Order order = Order.create(1L, "order", null);
        ReflectionTestUtils.setField(order, "id", 2L);
        final Payment payment = mock(Payment.class);
        when(lock.execute("order")).thenReturn(order);
        when(payments.findByOrderId(2L)).thenReturn(Optional.of(payment));
        when(payment.paidAmount()).thenReturn(BigDecimal.ZERO);

        assertThat(useCase.execute(1L, "order", 4L, "FAIL", "failure")).isFalse();
        verify(fail).execute("order", "FAIL", "failure");
        verify(release).execute("order");
        verify(operations, never()).save(any());
    }

    @Test
    void 영원이어도_이미_처리중인_작업이_있으면_예약을_해제하지_않는다() {
        final Order order = Order.create(1L, "order", null);
        ReflectionTestUtils.setField(order, "id", 2L);
        when(lock.execute("order")).thenReturn(order);
        when(operations.existsPending("order")).thenReturn(true);

        assertThat(useCase.execute(1L, "order", 4L, "FAIL", "failure")).isTrue();
        verifyNoInteractions(release);
    }
}
