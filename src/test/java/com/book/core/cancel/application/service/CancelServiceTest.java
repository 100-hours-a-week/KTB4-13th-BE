package com.book.core.cancel.application.service;

import org.mockito.InOrder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.book.core.cancel.application.command.CancelCommand;
import com.book.core.cancel.application.usecase.CancelPaymentUseCase;
import com.book.core.cancel.application.usecase.ValidateCancelUseCase;
import com.book.core.cancel.domain.CancelAction;
import org.junit.jupiter.api.Test;

class CancelServiceTest {
    private final ValidateCancelUseCase validate = mock(ValidateCancelUseCase.class);
    private final CancelPaymentUseCase cancel = mock(CancelPaymentUseCase.class);
    private final CancelService service = new CancelService(validate, cancel);

    @Test
    void 전체취소는_검증_후_쓰기를_실행한다() {
        final CancelAction action = new CancelAction("order");
        when(cancel.execute(action)).thenReturn(10L);
        assertThat(service.cancel(new CancelCommand(1L, action))).isEqualTo(10L);
        final InOrder sequence = inOrder(validate, cancel);
        sequence.verify(validate).validate(1L, action);
        sequence.verify(cancel).execute(action);
    }

}
