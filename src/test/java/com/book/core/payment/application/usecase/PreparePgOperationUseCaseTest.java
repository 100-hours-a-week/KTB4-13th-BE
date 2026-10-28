package com.book.core.payment.application.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.cancel.application.usecase.CalculatePartialCancelUseCase;
import com.book.core.cancel.application.usecase.GetCancelBalanceUseCase;
import com.book.core.cancel.domain.CancelBalance;
import com.book.core.cancel.domain.CancelCalculated;
import com.book.core.cancel.domain.PartialCancelAction;
import com.book.core.order.application.usecase.LockPaymentOrderUseCase;
import com.book.core.order.domain.Order;
import com.book.core.payment.application.command.PreparePgOperationCommand;
import com.book.core.payment.application.port.PgOperationRepositoryPort;
import com.book.core.payment.application.port.PaymentRepositoryPort;
import com.book.core.payment.domain.Payment;
import com.book.core.payment.domain.enums.PaymentState;
import com.book.core.payment.domain.PgOperation;
import com.book.core.payment.domain.PgOperationType;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

class PreparePgOperationUseCaseTest {
    private final LockPaymentOrderUseCase lock = mock(LockPaymentOrderUseCase.class);
    private final PgOperationRepositoryPort operations = mock(PgOperationRepositoryPort.class);
    private final PaymentRepositoryPort payments = mock(PaymentRepositoryPort.class);
    private final CalculatePartialCancelUseCase calculate = mock(CalculatePartialCancelUseCase.class);
    private final GetCancelBalanceUseCase balances = mock(GetCancelBalanceUseCase.class);
    private final PreparePgOperationUseCase useCase = new PreparePgOperationUseCase(lock, operations, mock(ValidatePaymentUseCase.class),
        mock(com.book.core.cancel.application.usecase.ValidateCancelUseCase.class), payments, calculate, balances);
    private final PreparePgOperationCommand command =
        new PreparePgOperationCommand("order", 1L, PgOperationType.CANCEL, "request", "input", null, null, 0);

    @Test
    void 주문을_잠근_후_요청을_저장한다() {
        prepareFull(BigDecimal.ZERO, BigDecimal.TEN);
        when(operations.findByRequestHash(command.requestHash())).thenReturn(Optional.empty());
        useCase.execute(command);
        verify(operations).save(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.5", "-1"})
    void 전체취소에서_유효하지_않은_복원포인트면_PG작업을_생성하지_않는다(final String amount) {
        prepareFull(new BigDecimal(amount), BigDecimal.TEN);
        assertThatThrownBy(() -> useCase.execute(command)).isInstanceOfSatisfying(CoreException.class,
            (final CoreException error) -> assertThat(error.errorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
        verify(operations, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"10.5", "-1"})
    void 전체취소에서_유효하지_않은_환불잔액이면_PG작업을_생성하지_않는다(final String amount) {
        prepareFull(BigDecimal.ZERO, new BigDecimal(amount));
        assertThatThrownBy(() -> useCase.execute(command)).isInstanceOfSatisfying(CoreException.class,
            (final CoreException error) -> assertThat(error.errorCode()).isEqualTo(ErrorCode.PAYMENT_INVALID_AMOUNT));
        verify(operations, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.5", "-1"})
    void 부분취소에서_유효하지_않은_복원포인트면_PG작업을_생성하지_않는다(final String amount) {
        final var partial = partial();
        preparePartial(new BigDecimal(amount), BigDecimal.ONE, BigDecimal.TEN);
        assertThatThrownBy(() -> useCase.execute(partial)).isInstanceOf(CoreException.class);
        verify(operations, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.5", "-1", "11"})
    void 부분취소에서_현금_환불액이_유효하지_않으면_PG작업을_생성하지_않는다(final String amount) {
        final var partial = partial();
        preparePartial(BigDecimal.ZERO, new BigDecimal(amount), BigDecimal.TEN);
        assertThatThrownBy(() -> useCase.execute(partial)).isInstanceOfSatisfying(CoreException.class,
            (final CoreException error) -> assertThat(error.errorCode()).isEqualTo(ErrorCode.PAYMENT_INVALID_AMOUNT));
        verify(operations, never()).save(any());
    }

    @Test
    void 정수_환불액과_정수_소수표현은_PG작업을_생성한다() {
        final var partial = partial();
        preparePartial(new BigDecimal("1.00"), new BigDecimal("3.00"), new BigDecimal("10.00"));
        useCase.execute(partial);
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

    private void prepareFull(final BigDecimal point, final BigDecimal cash) {
        final Order order = order();
        when(payments.findByOrderId(2L)).thenReturn(
            Optional.of(new Payment(1L, 2L, BigDecimal.TEN, -1L, BigDecimal.ZERO, point, 1L, BigDecimal.TEN, PaymentState.SUCCESS)));
        when(balances.execute(2L)).thenReturn(balance(cash));
        when(lock.execute("order")).thenReturn(order);
    }

    private void preparePartial(final BigDecimal point, final BigDecimal cash, final BigDecimal refundable) {
        when(lock.execute("order")).thenReturn(order());
        when(calculate.execute(new PartialCancelAction("order", 4L, 1L)))
            .thenReturn(new CancelCalculated(cash, BigDecimal.ZERO, point, false));
        when(balances.execute(2L)).thenReturn(balance(refundable));
    }

    private static CancelBalance balance(final BigDecimal cash) {
        return new CancelBalance(2L, cash, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
    }

    private static Order order() {
        final Order order = Order.create(1L, "order", null);
        ReflectionTestUtils.setField(order, "id", 2L);
        return order;
    }

    private static PreparePgOperationCommand partial() {
        return new PreparePgOperationCommand("order", 1L, PgOperationType.PARTIAL_CANCEL, "partial", "input", null, 4L, 1);
    }
}
