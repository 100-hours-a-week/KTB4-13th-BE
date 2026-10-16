package com.book.core.cancel.application.service;

import org.mockito.InOrder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.book.core.cancel.application.command.CancelCommand;
import com.book.core.cancel.application.command.PartialCancelCommand;
import com.book.core.cancel.application.usecase.CalculatePartialCancelUseCase;
import com.book.core.cancel.application.usecase.CancelPaymentUseCase;
import com.book.core.cancel.application.usecase.PartialCancelPaymentUseCase;
import com.book.core.cancel.application.usecase.ValidateCancelUseCase;
import com.book.core.cancel.domain.CancelAction;
import com.book.core.cancel.domain.CancelCalculated;
import com.book.core.cancel.domain.PartialCancelAction;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class CancelServiceTest {
    private final ValidateCancelUseCase validate = mock(ValidateCancelUseCase.class);
    private final CancelPaymentUseCase cancel = mock(CancelPaymentUseCase.class);
    private final CalculatePartialCancelUseCase calculate = mock(CalculatePartialCancelUseCase.class);
    private final PartialCancelPaymentUseCase partial = mock(PartialCancelPaymentUseCase.class);
    private final CancelService service = new CancelService(validate, cancel, calculate, partial);

    @Test
    void 전체취소는_검증_후_쓰기를_실행한다() {
        final CancelAction action = new CancelAction("order");
        when(cancel.execute(action)).thenReturn(10L);
        assertThat(service.cancel(new CancelCommand(1L, action))).isEqualTo(10L);
        final InOrder sequence = inOrder(validate, cancel);
        sequence.verify(validate).validate(1L, action);
        sequence.verify(cancel).execute(action);
        verifyNoInteractions(calculate, partial);
    }

    @Test
    void 부분취소는_검증_계산_쓰기_순서이며_계산값을_그대로_전달한다() {
        final PartialCancelAction action = new PartialCancelAction("order", 2L, 1L);
        final CancelCalculated calculated = new CancelCalculated(BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.ZERO, false);
        when(calculate.execute(action)).thenReturn(calculated);
        when(partial.execute(action, calculated)).thenReturn(10L);
        assertThat(service.partialCancel(new PartialCancelCommand(1L, action))).isEqualTo(10L);
        final InOrder sequence = inOrder(validate, calculate, partial);
        sequence.verify(validate).validatePartial(1L, action);
        sequence.verify(calculate).execute(action);
        sequence.verify(partial).execute(action, calculated);
    }

    @Test
    void 검증_실패는_계산과_쓰기를_실행하지_않는다() {
        final PartialCancelAction action = new PartialCancelAction("order", 2L, 1L);
        doThrow(new IllegalStateException("test")).when(validate).validatePartial(1L, action);
        assertThatThrownBy(() -> service.partialCancel(new PartialCancelCommand(1L, action))).isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(calculate, partial, cancel);
    }
}
