package com.book.core.payment.application.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import com.book.common.exception.CoreException;
import com.book.core.order.application.usecase.LockPaymentOrderUseCase;
import com.book.core.order.domain.Order;
import com.book.core.payment.application.command.PreparePgOperationCommand;
import com.book.core.payment.application.port.PgOperationRepositoryPort;
import com.book.core.payment.domain.PgOperation;
import com.book.core.payment.domain.PgOperationType;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PreparePgOperationUseCaseTest {
    private final LockPaymentOrderUseCase lock = mock(LockPaymentOrderUseCase.class);
    private final PgOperationRepositoryPort operations = mock(PgOperationRepositoryPort.class);
    private final PreparePgOperationUseCase useCase = new PreparePgOperationUseCase(lock, operations, mock(ValidatePaymentUseCase.class),
        mock(com.book.core.cancel.application.usecase.ValidateCancelUseCase.class));
    private final PreparePgOperationCommand command =
        new PreparePgOperationCommand("order", 1L, PgOperationType.CANCEL, "request", "input", null, null, 0);

    @Test
    void 주문을_잠근_후_요청을_저장한다() {
        when(lock.execute("order")).thenReturn(Order.create(1L, "order", null));
        when(operations.findByRequestHash(command.requestHash())).thenReturn(Optional.empty());
        useCase.execute(command);
        verify(operations).save(any());
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
}
