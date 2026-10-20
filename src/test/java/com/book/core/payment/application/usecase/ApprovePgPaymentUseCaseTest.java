package com.book.core.payment.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.order.application.usecase.GetPaymentOrderUseCase;
import com.book.core.order.domain.Order;
import com.book.core.order.domain.OrderStatus;
import com.book.core.payment.application.command.ApprovePaymentCommand;
import com.book.core.payment.application.port.PaymentClient;
import com.book.core.payment.application.result.PgPaymentResult;
import com.book.core.payment.domain.PgOperation;
import com.book.core.payment.domain.PgOperationType;
import com.book.core.payment.domain.enums.PaymentMethod;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ApprovePgPaymentUseCaseTest {
    private final GetPgOperationUseCase operations = mock(GetPgOperationUseCase.class);
    private final GetPaymentOrderUseCase orders = mock(GetPaymentOrderUseCase.class);
    private final ValidatePaymentUseCase validate = mock(ValidatePaymentUseCase.class);
    private final PaymentClient client = mock(PaymentClient.class);
    private final CompletePaymentUseCase complete = mock(CompletePaymentUseCase.class);
    private final ApprovePgPaymentUseCase useCase = new ApprovePgPaymentUseCase(operations, orders, validate, client, complete);
    private final ApprovePaymentCommand command = new ApprovePaymentCommand("order", "key", BigDecimal.TEN);
    private final PgOperation operation = new PgOperation("order", "request", "input", PgOperationType.APPROVE);

    private void prepare(final ApprovePaymentCommand input) {
        when(operations.execute(input.operation())).thenReturn(operation);
        when(orders.execute("order", OrderStatus.CREATED)).thenReturn(Order.create(1L, "order", null));
        when(complete.execute(eq("order"), any())).thenReturn(3L);
    }

    @Test
    void 인증완료_결제를_승인하고_실제_승인번호로_내부_처리한다() {
        prepare(command);
        when(client.find("key")).thenReturn(result("IN_PROGRESS", "order"));
        when(client.approve("key", "order", BigDecimal.TEN, operation.pgIdempotencyKey())).thenReturn(result("DONE", "order"));
        assertThat(useCase.execute(command).paymentId()).isEqualTo(3L);
        assertThat(operation.completed()).isTrue();
        verify(complete).execute("order",
            new com.book.core.payment.application.result.ApprovedPaymentResult("key", PaymentMethod.CARD, "1234"));
    }

    @Test
    void 승인전_카드정보가_없어도_인증완료_결제를_승인한다() {
        prepare(command);
        when(client.find("key"))
            .thenReturn(new PgPaymentResult("key", "order", BigDecimal.TEN, BigDecimal.TEN, "IN_PROGRESS", null, null, List.of()));
        when(client.approve("key", "order", BigDecimal.TEN, operation.pgIdempotencyKey())).thenReturn(result("DONE", "order"));

        assertThat(useCase.execute(command).paymentId()).isEqualTo(3L);
        verify(client).approve("key", "order", BigDecimal.TEN, operation.pgIdempotencyKey());
        verify(complete).execute("order",
            new com.book.core.payment.application.result.ApprovedPaymentResult("key", PaymentMethod.CARD, "1234"));
        assertThat(operation.completed()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"method", "approveCode", "blankApproveCode"})
    void 승인후_결제수단이나_승인번호가_없으면_내부_결제를_완료하지_않는다(final String missingField) {
        prepare(command);
        when(client.find("key"))
            .thenReturn(new PgPaymentResult("key", "order", BigDecimal.TEN, BigDecimal.TEN, "IN_PROGRESS", null, null, List.of()));
        final PaymentMethod method = "method".equals(missingField) ? null : PaymentMethod.CARD;
        final String approveCode = switch (missingField) {
            case "approveCode" -> null;
            case "blankApproveCode" -> " ";
            default -> "1234";
        };
        when(client.approve("key", "order", BigDecimal.TEN, operation.pgIdempotencyKey()))
            .thenReturn(new PgPaymentResult("key", "order", BigDecimal.TEN, BigDecimal.TEN, "DONE", method, approveCode, List.of()));

        assertThatThrownBy(() -> useCase.execute(command)).isInstanceOfSatisfying(CoreException.class,
            exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.PG_RESPONSE_MISMATCH));
        verify(client).approve("key", "order", BigDecimal.TEN, operation.pgIdempotencyKey());
        verifyNoInteractions(complete);
        assertThat(operation.completed()).isFalse();
    }

    @Test
    void PG성공후_DB실패_재요청은_조회결과로_내부_완료만_재개한다() {
        prepare(command);
        when(client.find("key")).thenReturn(result("DONE", "order"));
        assertThat(useCase.execute(command).paymentId()).isEqualTo(3L);
        verify(client, never()).approve(any(), any(), any(), any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ABORTED", "EXPIRED"})
    void 토스에서_종료된_상태만_결제_거절로_처리한다(final String status) {
        prepare(command);
        when(client.find("key")).thenReturn(result(status, "order"));
        assertThatThrownBy(() -> useCase.execute(command)).isInstanceOfSatisfying(CoreException.class,
            exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.PG_REJECTED));
    }

    @Test
    void READY는_인증전_상태라_요청을_미확정으로_유지한다() {
        prepare(command);
        when(client.find("key")).thenReturn(result("READY", "order"));
        assertThatThrownBy(() -> useCase.execute(command)).isInstanceOfSatisfying(CoreException.class,
            exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.PG_PENDING));
        assertThat(operation.rejected()).isFalse();
        assertThat(operation.completed()).isFalse();
        verifyNoInteractions(complete);
    }

    @Test
    void 알_수_없는_토스_상태는_응답_불일치로_처리한다() {
        prepare(command);
        when(client.find("key")).thenReturn(result("FUTURE_STATUS", "order"));
        assertThatThrownBy(() -> useCase.execute(command)).isInstanceOfSatisfying(CoreException.class,
            exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.PG_RESPONSE_MISMATCH));
    }

    @Test
    void 완료된_요청은_다시_승인하거나_포인트를_반영하지_않는다() {
        prepare(command);
        operation.complete(3L);
        assertThat(useCase.execute(command).paymentId()).isEqualTo(3L);
        verifyNoInteractions(client, complete, validate);
    }

    @Test
    void 외부_주문이_다르면_내부_완료하지_않는다() {
        prepare(command);
        when(client.find("key")).thenReturn(result("DONE", "other"));
        assertThatThrownBy(() -> useCase.execute(command)).isInstanceOf(CoreException.class);
        verifyNoInteractions(complete);
        assertThat(operation.completed()).isFalse();
    }

    @Test
    void 영원_결제는_PG를_호출하지_않는다() {
        final var zero = new ApprovePaymentCommand("order", "", BigDecimal.ZERO);
        prepare(zero);
        assertThat(useCase.execute(zero).paymentId()).isEqualTo(3L);
        verifyNoInteractions(client);
    }

    private static PgPaymentResult result(final String state, final String orderKey) {
        return new PgPaymentResult("key", orderKey, BigDecimal.TEN, BigDecimal.TEN, state, PaymentMethod.CARD, "1234", List.of());
    }
}
