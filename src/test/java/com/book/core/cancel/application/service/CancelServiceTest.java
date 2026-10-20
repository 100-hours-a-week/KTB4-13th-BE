package com.book.core.cancel.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.cancel.application.command.CancelCommand;
import com.book.core.cancel.application.command.PartialCancelCommand;
import com.book.core.cancel.application.usecase.CancelPgPaymentUseCase;
import com.book.core.cancel.application.usecase.PartialCancelPgPaymentUseCase;
import com.book.core.cancel.domain.CancelAction;
import com.book.core.cancel.domain.PartialCancelAction;
import com.book.core.payment.application.usecase.PreparePgOperationUseCase;
import com.book.core.payment.application.usecase.RejectPgOperationUseCase;
import org.junit.jupiter.api.Test;

class CancelServiceTest {
    private final CancelPgPaymentUseCase cancel = mock(CancelPgPaymentUseCase.class);
    private final PartialCancelPgPaymentUseCase partial = mock(PartialCancelPgPaymentUseCase.class);
    private final PreparePgOperationUseCase prepare = mock(PreparePgOperationUseCase.class);
    private final RejectPgOperationUseCase reject = mock(RejectPgOperationUseCase.class);
    private final CancelService service = new CancelService(cancel, prepare, reject, partial);

    @Test
    void 전체취소는_요청을_먼저_저장하고_PG흐름을_실행한다() {
        final var command = new CancelCommand(1L, new CancelAction("order"), "request");
        when(cancel.execute(command)).thenReturn(10L);
        assertThat(service.cancel(command)).isEqualTo(10L);
        final var sequence = inOrder(prepare, cancel);
        sequence.verify(prepare).execute(command.operation());
        sequence.verify(cancel).execute(command);
        verifyNoInteractions(partial);
    }

    @Test
    void 부분취소는_요청을_먼저_저장하고_PG흐름을_실행한다() {
        final var command = new PartialCancelCommand(1L, new PartialCancelAction("order", 2L, 1L), "request");
        when(partial.execute(command)).thenReturn(10L);
        assertThat(service.partialCancel(command)).isEqualTo(10L);
        final var sequence = inOrder(prepare, partial);
        sequence.verify(prepare).execute(command.operation());
        sequence.verify(partial).execute(command);
    }

    @Test
    void 준비실패는_PG를_호출하지_않는다() {
        doThrow(new CoreException(ErrorCode.PG_PENDING)).when(prepare).execute(any());
        assertThatThrownBy(() -> service.partialCancel(new PartialCancelCommand(1L, new PartialCancelAction("order", 2L, 1L), "request")))
            .isInstanceOf(CoreException.class);
        verifyNoInteractions(partial, cancel, reject);
    }

    @Test
    void 확정_거절만_기록하고_타임아웃은_미완료로_둔다() {
        final var command = new CancelCommand(1L, new CancelAction("order"), "request");
        when(cancel.execute(command)).thenThrow(new CoreException(ErrorCode.PG_REJECTED));
        assertThatThrownBy(() -> service.cancel(command)).isInstanceOf(CoreException.class);
        verify(reject).execute(command.operation());
    }
}
