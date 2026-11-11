package com.book.core.coupon.application.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.book.core.coupon.application.port.IssuanceDltLogRepositoryPort;
import com.book.core.coupon.application.port.IssuanceDltLogRepositoryPort.ReplayRequest;
import com.book.core.coupon.application.port.IssuanceRequestPublisher;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;

class ReplayIssuanceDltUseCaseTest {
    private final IssuanceDltLogRepositoryPort logs = mock(IssuanceDltLogRepositoryPort.class);
    private final IssuanceRequestPublisher publisher = mock(IssuanceRequestPublisher.class);
    private final ReplayIssuanceDltUseCase useCase = new ReplayIssuanceDltUseCase(logs, publisher);

    @Test
    void 선택된_실패_건이_없으면_발행하지_않는다() {
        when(logs.lockNextRequested()).thenReturn(Optional.empty());

        useCase.execute();

        verifyNoInteractions(publisher);
    }

    @Test
    void 발행_확인을_받은_뒤에만_재발행_완료를_기록한다() {
        when(logs.lockNextRequested()).thenReturn(Optional.of(new ReplayRequest(7L, "stream-id", "{}")));
        doThrow(new CompletionException(new TimeoutException())).doNothing().when(publisher).publishAndWait("stream-id", "{}");

        assertThatThrownBy(useCase::execute).isInstanceOf(CompletionException.class);
        useCase.execute();

        final var order = inOrder(logs, publisher);
        order.verify(logs).lockNextRequested();
        order.verify(publisher).publishAndWait("stream-id", "{}");
        order.verify(logs).lockNextRequested();
        order.verify(publisher).publishAndWait("stream-id", "{}");
        order.verify(logs).markReplayed(7L);
        order.verifyNoMoreInteractions();
    }
}
