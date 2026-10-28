package com.book.core.cancel.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import com.book.common.exception.CoreException;
import com.book.core.cancel.application.command.CancelCommand;
import com.book.core.cancel.application.command.PartialCancelCommand;
import com.book.core.cancel.application.port.CancelBalanceRepositoryPort;
import com.book.core.cancel.domain.CancelAction;
import com.book.core.cancel.domain.CancelBalance;
import com.book.core.cancel.domain.CancelCalculated;
import com.book.core.cancel.domain.PartialCancelAction;
import com.book.core.payment.application.result.CanceledPaymentResult;
import com.book.core.payment.application.usecase.GetPaymentUseCase;
import com.book.core.payment.application.usecase.GetPgOperationUseCase;
import com.book.core.payment.application.usecase.RequestPgCancelUseCase;
import com.book.core.payment.domain.Payment;
import com.book.core.payment.domain.PgOperation;
import com.book.core.payment.domain.PgOperationType;
import com.book.core.payment.domain.enums.PaymentState;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CancelPgPaymentUseCaseTest {
    private final GetPgOperationUseCase operations = mock(GetPgOperationUseCase.class);
    private final ValidateCancelUseCase validate = mock(ValidateCancelUseCase.class);
    private final GetPaymentUseCase payments = mock(GetPaymentUseCase.class);
    private final CancelBalanceRepositoryPort balances = mock(CancelBalanceRepositoryPort.class);
    private final RequestPgCancelUseCase pg = mock(RequestPgCancelUseCase.class);
    private final CancelPaymentUseCase full = mock(CancelPaymentUseCase.class);
    private final PartialCancelPaymentUseCase partial = mock(PartialCancelPaymentUseCase.class);
    private final CalculatePartialCancelUseCase calculate = mock(CalculatePartialCancelUseCase.class);
    private final CancelPgPaymentUseCase fullUseCase = new CancelPgPaymentUseCase(operations, validate, payments, balances, pg, full);
    private final PartialCancelPgPaymentUseCase partialUseCase =
        new PartialCancelPgPaymentUseCase(operations, validate, calculate, payments, balances, pg, partial);
    private final CanceledPaymentResult result = new CanceledPaymentResult("cancel-key", OffsetDateTime.now());

    private void fixture() {
        when(payments.execute("order")).thenReturn(
            new Payment(1L, 2L, BigDecimal.TEN, -1L, BigDecimal.ZERO, BigDecimal.ZERO, 1L, BigDecimal.TEN, PaymentState.SUCCESS));
        when(balances.findByOrderId(2L)).thenReturn(Optional.of(
            new CancelBalance(2L, BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)));
        when(pg.execute(any(), any(), any(), any(), any())).thenReturn(result);
    }

    @Test
    void 전체취소_결과를_내부_반영하고_완료를_기록한다() {
        fixture();
        final var command = new CancelCommand(1L, new CancelAction("order"), "request");
        final var operation = new PgOperation("order", "request", "input", PgOperationType.CANCEL);
        when(operations.execute(command.operation())).thenReturn(operation);
        when(full.execute(command.action(), result)).thenReturn(42L);
        assertThat(fullUseCase.execute(command)).isEqualTo(42L);
        assertThat(operation.completed()).isTrue();
    }

    @Test
    void 부분취소_계산값과_PG결과를_함께_반영한다() {
        fixture();
        final var command = new PartialCancelCommand(1L, new PartialCancelAction("order", 4L, 1L), "request");
        final var operation = new PgOperation("order", "request", "input", PgOperationType.PARTIAL_CANCEL);
        when(operations.execute(command.operation())).thenReturn(operation);
        final var calculated = new CancelCalculated(BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO, false);
        when(calculate.execute(command.action())).thenReturn(calculated);
        when(partial.execute(command.action(), calculated, result)).thenReturn(42L);
        assertThat(partialUseCase.execute(command)).isEqualTo(42L);
        assertThat(operation.completed()).isTrue();
    }

    @Test
    void 재전송된_전체취소의_복원포인트가_소수라면_PG를_호출하지_않는다() {
        final var command = new CancelCommand(1L, new CancelAction("order"), "request");
        when(operations.execute(command.operation())).thenReturn(new PgOperation("order", "request", "input", PgOperationType.CANCEL));
        when(payments.execute("order")).thenReturn(
            new Payment(1L, 2L, BigDecimal.TEN, -1L, BigDecimal.ZERO, new BigDecimal("0.5"), 1L, BigDecimal.TEN, PaymentState.SUCCESS));
        assertThatThrownBy(() -> fullUseCase.execute(command)).isInstanceOf(CoreException.class);
        verifyNoInteractions(pg, full);
    }

    @Test
    void 재전송된_부분취소의_복원포인트가_소수라면_PG를_호출하지_않는다() {
        final var command = new PartialCancelCommand(1L, new PartialCancelAction("order", 4L, 1L), "request");
        when(operations.execute(command.operation()))
            .thenReturn(new PgOperation("order", "request", "input", PgOperationType.PARTIAL_CANCEL));
        when(calculate.execute(command.action()))
            .thenReturn(new CancelCalculated(BigDecimal.ONE, BigDecimal.ZERO, new BigDecimal("0.5"), false));
        assertThatThrownBy(() -> partialUseCase.execute(command)).isInstanceOf(CoreException.class);
        verifyNoInteractions(pg, partial);
    }

    @Test
    void 완료된_전체와_부분_취소는_검증과_환불을_반복하지_않는다() {
        final var command = new CancelCommand(1L, new CancelAction("order"), "full");
        final var partialCommand = new PartialCancelCommand(1L, new PartialCancelAction("order", 4L, 1L), "partial");
        final var fullOperation = new PgOperation("order", "full", "input", PgOperationType.CANCEL);
        final var partialOperation = new PgOperation("order", "partial", "input", PgOperationType.PARTIAL_CANCEL);
        fullOperation.complete(42L);
        partialOperation.complete(43L);
        when(operations.execute(command.operation())).thenReturn(fullOperation);
        when(operations.execute(partialCommand.operation())).thenReturn(partialOperation);
        assertThat(fullUseCase.execute(command)).isEqualTo(42L);
        assertThat(partialUseCase.execute(partialCommand)).isEqualTo(43L);
        verifyNoInteractions(validate, pg, full, partial, calculate, payments, balances);
    }
}
