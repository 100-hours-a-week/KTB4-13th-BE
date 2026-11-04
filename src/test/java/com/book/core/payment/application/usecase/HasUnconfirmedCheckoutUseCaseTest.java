package com.book.core.payment.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.book.core.payment.application.port.PaymentAttemptRepositoryPort;
import com.book.core.payment.application.port.PaymentRepositoryPort;
import com.book.core.payment.application.port.PgOperationRepositoryPort;
import com.book.core.payment.domain.Payment;
import com.book.core.payment.domain.PaymentAttempt;
import com.book.core.payment.domain.PgOperation;
import com.book.core.payment.domain.enums.PaymentState;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class HasUnconfirmedCheckoutUseCaseTest {
    private final PaymentRepositoryPort payments = mock(PaymentRepositoryPort.class);
    private final PaymentAttemptRepositoryPort attempts = mock(PaymentAttemptRepositoryPort.class);
    private final PgOperationRepositoryPort operations = mock(PgOperationRepositoryPort.class);
    private final HasUnconfirmedCheckoutUseCase useCase = new HasUnconfirmedCheckoutUseCase(payments, attempts, operations);

    @Test
    void 양수_READY_시도는_미확정으로_판단하고_운영확인_기록을_남긴다() {
        final Payment payment = payment(BigDecimal.TEN);
        final PaymentAttempt attempt = new PaymentAttempt(10L, "order-1", "pg-order-1", "request", "input");
        ReflectionTestUtils.setField(attempt, "id", 11L);
        when(payments.findByOrderId(1L)).thenReturn(Optional.of(payment));
        when(attempts.findLatestByPaymentId(10L)).thenReturn(Optional.of(attempt));

        assertThat(useCase.execute(1L)).isTrue();
        useCase.requireReview("order-1", 1L);

        final ArgumentCaptor<PgOperation> record = ArgumentCaptor.forClass(PgOperation.class);
        verify(operations).save(record.capture());
        assertThat(record.getValue().reviewRequired()).isTrue();
        assertThat(record.getValue().blocksOrder()).isTrue();
        assertThat(record.getValue().attemptId()).isEqualTo(11L);
        assertThat(record.getValue().pgOrderId()).isEqualTo("pg-order-1");
    }

    @Test
    void 최종_실패한_시도와_영원_결제는_주문_해제를_보류하지_않는다() {
        final Payment payment = payment(BigDecimal.TEN);
        final PaymentAttempt attempt = new PaymentAttempt(10L, "order-1", "pg-order-1", "request", "input");
        attempt.fail();
        when(payments.findByOrderId(1L)).thenReturn(Optional.of(payment));
        when(attempts.findLatestByPaymentId(10L)).thenReturn(Optional.of(attempt));
        assertThat(useCase.execute(1L)).isFalse();

        when(payments.findByOrderId(2L)).thenReturn(Optional.of(payment(BigDecimal.ZERO)));
        assertThat(useCase.execute(2L)).isFalse();
        verifyNoInteractions(operations);
    }

    @Test
    void 기존_시도_정보가_없는_READY_양수_결제는_미확정으로_보존한다() {
        when(payments.findByOrderId(1L)).thenReturn(Optional.of(payment(BigDecimal.TEN)));
        assertThat(useCase.execute(1L)).isTrue();
        useCase.requireReview("order-1", 1L);
        final ArgumentCaptor<PgOperation> record = ArgumentCaptor.forClass(PgOperation.class);
        verify(operations).save(record.capture());
        assertThat(record.getValue().reviewRequired()).isTrue();
        assertThat(record.getValue().attemptId()).isNull();
    }

    @Test
    void 이미_확인_중인_PG작업이_있으면_운영확인_기록을_중복하지_않는다() {
        when(operations.existsPending("order-1")).thenReturn(true);
        useCase.requireReview("order-1", 1L);
        org.mockito.Mockito.verify(operations, org.mockito.Mockito.never()).save(any());
        verifyNoInteractions(payments, attempts);
    }

    private static Payment payment(final BigDecimal amount) {
        final Payment payment = new Payment(1L, 1L, amount, -1L, BigDecimal.ZERO, BigDecimal.ZERO, 1L, amount, PaymentState.READY);
        ReflectionTestUtils.setField(payment, "id", 10L);
        return payment;
    }
}
