package com.book.core.payment.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.book.core.order.application.usecase.LockPaymentOrderUseCase;
import com.book.core.payment.application.port.PgOperationRepositoryPort;
import com.book.core.payment.domain.PgOperation;
import com.book.core.payment.domain.PgOperationType;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MarkPgOperationUncertainUseCaseTest {
    private final LockPaymentOrderUseCase lockOrder = mock(LockPaymentOrderUseCase.class);
    private final PgOperationRepositoryPort operations = mock(PgOperationRepositoryPort.class);
    private final MarkPgOperationUncertainUseCase useCase = new MarkPgOperationUncertainUseCase(lockOrder, operations);

    @Test
    void 최초_불명확시각을_유지하고_십분이_지나면_운영확인으로_전환한다() {
        final PgOperation operation = new PgOperation("order", "request", "input", PgOperationType.APPROVE);
        final Instant first = Instant.now().minus(11, ChronoUnit.MINUTES);
        operation.markUncertain(first);
        when(operations.findByRequestHash("request")).thenReturn(Optional.of(operation));

        useCase.execute(operation, false);

        assertThat(operation.uncertainAt()).isEqualTo(first);
        assertThat(operation.reviewRequired()).isTrue();
    }

    @Test
    void 이미_완료된_요청은_운영확인으로_변경하지_않는다() {
        final PgOperation operation = new PgOperation("order", "request", "input", PgOperationType.APPROVE);
        operation.complete(1L);
        when(operations.findByRequestHash("request")).thenReturn(Optional.of(operation));

        useCase.execute(operation, true);

        assertThat(operation.uncertainAt()).isNull();
        assertThat(operation.reviewRequired()).isFalse();
    }
}
