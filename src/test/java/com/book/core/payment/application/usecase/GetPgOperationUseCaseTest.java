package com.book.core.payment.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.book.core.order.application.usecase.LockPaymentOrderUseCase;
import com.book.core.payment.application.command.ApprovePaymentCommand;
import com.book.core.payment.application.command.PreparePgOperationCommand;
import com.book.core.payment.application.port.PgOperationRepositoryPort;
import com.book.core.payment.domain.PgOperation;
import com.book.core.payment.domain.PgOperationType;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class GetPgOperationUseCaseTest {
    private final LockPaymentOrderUseCase lock = mock(LockPaymentOrderUseCase.class);
    private final PgOperationRepositoryPort operations = mock(PgOperationRepositoryPort.class);
    private final GetPgOperationUseCase useCase = new GetPgOperationUseCase(lock, operations);

    @Test
    void 같은_결제키의_다른_금액은_기존_승인작업이_아닌_격리된_확인작업을_조회한다() {
        final PreparePgOperationCommand correct = new ApprovePaymentCommand("order", "pg-order", "key", BigDecimal.TEN, 1L).operation();
        final PreparePgOperationCommand incorrect = new ApprovePaymentCommand("order", "pg-order", "key", BigDecimal.ONE, 1L).operation();
        final PgOperation regular = new PgOperation("order", correct.requestHash(), correct.inputHash(), PgOperationType.APPROVE);
        final PgOperation isolated =
            new PgOperation("order", incorrect.isolatedApprovalHash(), incorrect.inputHash(), PgOperationType.APPROVE);
        isolated.isolateFromOrder();
        isolated.requireReview();
        when(operations.findByRequestHash(correct.requestHash())).thenReturn(Optional.of(regular));
        when(operations.findByRequestHash(incorrect.isolatedApprovalHash())).thenReturn(Optional.of(isolated));

        assertThat(useCase.execute(incorrect)).isSameAs(isolated);
        assertThat(isolated.reviewRequired()).isTrue();
        assertThat(regular.reviewRequired()).isFalse();
    }

    @Test
    void 기존_요청과_입력이_같으면_기존_PG작업을_그대로_재조회한다() {
        final PreparePgOperationCommand command = new ApprovePaymentCommand("order", "pg-order", "key", BigDecimal.TEN, 1L).operation();
        final PgOperation regular = new PgOperation("order", command.requestHash(), command.inputHash(), PgOperationType.APPROVE);
        when(operations.findByRequestHash(command.requestHash())).thenReturn(Optional.of(regular));

        assertThat(useCase.execute(command)).isSameAs(regular);
        verify(operations, never()).findByRequestHash(command.isolatedApprovalHash());
    }
}
