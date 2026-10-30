package com.book.core.payment.infrastructure.scheduler;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.book.core.payment.application.command.ApprovePaymentCommand;
import com.book.core.payment.application.port.PgOperationRepositoryPort;
import com.book.core.payment.application.service.PaymentService;
import com.book.core.payment.application.usecase.MarkPgOperationUncertainUseCase;
import com.book.core.payment.application.usecase.CompensatePgApprovalUseCase;
import com.book.core.payment.domain.PgOperation;
import com.book.core.payment.domain.PgOperationType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.Test;

class PgApprovalRecoverySchedulerTest {
    private final PgOperationRepositoryPort operations = mock(PgOperationRepositoryPort.class);
    private final PaymentService payments = mock(PaymentService.class);
    private final MarkPgOperationUncertainUseCase mark = mock(MarkPgOperationUncertainUseCase.class);
    private final CompensatePgApprovalUseCase compensate = mock(CompensatePgApprovalUseCase.class);
    private final PgApprovalRecoveryScheduler scheduler = new PgApprovalRecoveryScheduler(operations, payments, mark, compensate);

    @Test
    void 불명확한_결제는_원래_시도_정보로_복구한다() {
        final PgOperation operation = operation();
        operation.recordApprovalDetails("payment-key", BigDecimal.TEN, "pg-order", 123L);
        operation.markUncertain(Instant.now().minusSeconds(5));
        when(operations.findPendingApprovals()).thenReturn(List.of(operation));

        scheduler.recover();

        verify(mark).execute(operation, false);
        verify(payments).success(new ApprovePaymentCommand("order", "pg-order", "payment-key", BigDecimal.TEN, 123L));
    }

    @Test
    void 결과가_십분동안_불명확하면_재시도보다_운영확인을_요청한다() {
        final PgOperation operation = operation();
        operation.recordApprovalDetails("payment-key", BigDecimal.TEN, "pg-order", 123L);
        operation.markUncertain(Instant.now().minus(11, ChronoUnit.MINUTES));
        when(operations.findPendingApprovals()).thenReturn(List.of(operation));

        scheduler.recover();

        verify(mark).execute(operation, false);
        verifyNoInteractions(payments);
    }

    @Test
    void 확인에_필요한_정보가_없는_기존_작업은_즉시_운영확인한다() {
        final PgOperation operation = operation();
        when(operations.findPendingApprovals()).thenReturn(List.of(operation));

        scheduler.recover();

        verify(mark).execute(operation, true);
        verifyNoInteractions(payments);
    }

    @Test
    void 보상취소가_불명확하면_승인하지_않고_취소결과를_복구한다() {
        final PgOperation operation = operation();
        operation.recordApprovalDetails("payment-key", BigDecimal.TEN, "pg-order", 123L);
        operation.requestCompensation();
        operation.markUncertain(Instant.now().minusSeconds(5));
        when(operations.findPendingApprovals()).thenReturn(List.of(operation));

        scheduler.recover();

        verify(compensate).execute("order", "request");
        verifyNoInteractions(payments);
    }

    private static PgOperation operation() {
        return new PgOperation("order", "request", "input", PgOperationType.APPROVE);
    }
}
