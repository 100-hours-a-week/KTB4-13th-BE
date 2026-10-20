package com.book.core.settlement.application.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.book.common.paging.SliceResult;
import com.book.core.cancel.application.usecase.GetCancelsForSettlementUseCase;
import com.book.core.cancel.domain.Cancel;
import com.book.core.payment.application.usecase.GetPaymentsForSettlementUseCase;
import com.book.core.payment.domain.Payment;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class LoadSettlementTargetsUseCaseTest {
    private final GetPaymentsForSettlementUseCase payments = mock(GetPaymentsForSettlementUseCase.class);
    private final GetCancelsForSettlementUseCase cancels = mock(GetCancelsForSettlementUseCase.class);
    private final CreateSettlementTargetsUseCase create = mock(CreateSettlementTargetsUseCase.class);
    private final LoadSettlementTargetsUseCase useCase = new LoadSettlementTargetsUseCase(payments, cancels, create);
    private final LocalDate date = LocalDate.of(2026, 10, 16);

    @Test
    void 결제_취소를_각각_천건씩_읽고_페이지_실패후에도_계속한다() {
        final LocalDateTime from = date.minusDays(1).atStartOfDay();
        final LocalDateTime to = date.atStartOfDay().minusNanos(1);
        final Payment payment = mock(Payment.class);
        final Cancel cancel = mock(Cancel.class);
        when(payments.execute(from, to, 0, 1000)).thenReturn(new SliceResult<>(List.of(payment), true));
        when(payments.execute(from, to, 1, 1000)).thenReturn(new SliceResult<>(List.of(), false));
        when(cancels.execute(from, to, 0, 1000)).thenReturn(new SliceResult<>(List.of(cancel), true));
        when(cancels.execute(from, to, 1, 1000)).thenReturn(new SliceResult<>(List.of(), false));
        doThrow(new IllegalStateException("test payment")).when(create).processPayments(date, List.of(payment));
        doThrow(new IllegalStateException("test cancel")).when(create).processCancels(date, List.of(cancel));
        useCase.execute(date, from, to);
        final InOrder order = inOrder(payments, cancels, create);
        order.verify(payments).execute(from, to, 0, 1000);
        order.verify(create).processPayments(date, List.of(payment));
        order.verify(payments).execute(from, to, 1, 1000);
        order.verify(create).processPayments(date, List.of());
        order.verify(cancels).execute(from, to, 0, 1000);
        order.verify(create).processCancels(date, List.of(cancel));
        order.verify(cancels).execute(from, to, 1, 1000);
        order.verify(create).processCancels(date, List.of());
    }

    @Test
    void 결제_조회_실패는_전파하고_취소_적재를_시작하지_않는다() {
        final LocalDateTime from = date.atStartOfDay();
        final LocalDateTime to = date.plusDays(1).atStartOfDay();
        final IllegalStateException failure = new IllegalStateException("payment read failed");
        when(payments.execute(from, to, 0, 1000)).thenThrow(failure);

        assertThatThrownBy(() -> useCase.execute(date, from, to)).isSameAs(failure);
        verifyNoInteractions(create, cancels);
    }

}
