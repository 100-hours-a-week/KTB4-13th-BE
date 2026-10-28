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
import com.book.core.payment.application.port.PaymentClient;
import com.book.core.payment.application.port.PgOperationRepositoryPort;
import com.book.core.payment.application.result.PgPaymentResult;
import com.book.core.payment.domain.PgOperation;
import com.book.core.payment.domain.PgOperationType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CompensatePgApprovalUseCaseTest {
    private final LockPaymentOrderUseCase lock = mock(LockPaymentOrderUseCase.class);
    private final PgOperationRepositoryPort operations = mock(PgOperationRepositoryPort.class);
    private final ValidatePaymentAttemptUseCase validate = mock(ValidatePaymentAttemptUseCase.class);
    private final ReleaseActivePaymentReservationUseCase release = mock(ReleaseActivePaymentReservationUseCase.class);
    private final PaymentClient client = mock(PaymentClient.class);
    private final CompensatePgApprovalUseCase useCase = new CompensatePgApprovalUseCase(lock, operations, validate, release, client);

    @Test
    void 보상취소_확인_이후에만_예약을_해제한다() {
        final PgOperation operation = operation();
        when(client.find("key")).thenReturn(result("DONE", BigDecimal.TEN));
        when(client.cancel("key", BigDecimal.TEN, operation.compensationIdempotencyKey())).thenReturn(result("CANCELED", BigDecimal.ZERO));

        useCase.execute("order", "request");

        verify(release).execute("order");
        assertThat(operation.rejected()).isTrue();
    }

    @Test
    void 취소응답이_유실되면_예약을_유지하고_다음_조회에서_취소를_확인한다() {
        final PgOperation operation = operation();
        when(client.find("key")).thenReturn(result("DONE", BigDecimal.TEN), result("CANCELED", BigDecimal.ZERO));
        when(client.cancel("key", BigDecimal.TEN, operation.compensationIdempotencyKey())).thenThrow(new IllegalStateException("timeout"));

        assertThatThrownBy(() -> useCase.execute("order", "request")).isInstanceOf(IllegalStateException.class);
        assertThat(operation.rejected()).isFalse();
        verifyNoInteractions(release);

        useCase.execute("order", "request");
        verify(release).execute("order");
        verify(client).cancel("key", BigDecimal.TEN, operation.compensationIdempotencyKey());
        assertThat(operation.rejected()).isTrue();
    }

    @Test
    void 보상취소가_시작되지_않은_작업은_취소하지_않는다() {
        final PgOperation operation = new PgOperation("order", "request", "input", PgOperationType.APPROVE);
        when(operations.findByRequestHash("request")).thenReturn(Optional.of(operation));

        assertThatThrownBy(() -> useCase.execute("order", "request")).isInstanceOf(RuntimeException.class);
        verify(client, never()).cancel(any(), any(), any());
        verifyNoInteractions(release);
    }

    @Test
    void 보상취소_재요청은_같은_PG멱등키를_사용한다() {
        final PgOperation operation = operation();
        final String originalKey = operation.compensationIdempotencyKey();

        operation.requestCompensation();

        assertThat(operation.compensationIdempotencyKey()).isEqualTo(originalKey);
    }

    private PgOperation operation() {
        final PgOperation operation = new PgOperation("order", "request", "input", PgOperationType.APPROVE);
        operation.recordApprovalDetails("key", BigDecimal.TEN, "pg-order", 123L);
        operation.requestCompensation();
        when(operations.findByRequestHash("request")).thenReturn(Optional.of(operation));
        return operation;
    }

    private static PgPaymentResult result(final String status, final BigDecimal balance) {
        return new PgPaymentResult("key", "pg-order", BigDecimal.TEN, balance, status, null, null, List.of());
    }
}
