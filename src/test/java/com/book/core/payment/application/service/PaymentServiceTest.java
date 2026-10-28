package com.book.core.payment.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.coupon.application.usecase.FindOwnedCouponsForCheckoutUseCase;
import com.book.core.coupon.application.usecase.GetCouponTargetsUseCase;
import com.book.core.order.application.usecase.GetPaymentOrderUseCase;
import com.book.core.order.application.usecase.GetOrderUseCase;
import com.book.core.order.application.command.GetOrderCommand;
import com.book.core.order.domain.Order;
import com.book.core.payment.application.command.ApprovePaymentCommand;
import com.book.core.payment.application.command.CreatePaymentCommand;
import com.book.core.payment.application.command.FailPaymentCommand;
import com.book.core.payment.application.result.PaymentCompletionResult;
import com.book.core.payment.application.result.PaymentCreationResult;
import com.book.core.payment.application.usecase.ApprovePgPaymentUseCase;
import com.book.core.payment.application.usecase.CreatePaymentUseCase;
import com.book.core.payment.application.usecase.FailPaymentAttemptUseCase;
import com.book.core.payment.application.usecase.RecordPaymentFailureCallbackUseCase;
import com.book.core.payment.application.usecase.MarkPgOperationUncertainUseCase;
import com.book.core.payment.application.usecase.PostProcessPaymentUseCase;
import com.book.core.payment.application.usecase.PreparePgOperationUseCase;
import com.book.core.payment.application.usecase.ResolvePaymentAttemptUseCase;
import com.book.core.payment.domain.PaymentAttempt;
import com.book.core.payment.domain.PaymentDiscount;
import com.book.core.point.application.usecase.GetPointBalanceUseCase;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.test.util.ReflectionTestUtils;

class PaymentServiceTest {
    private final GetPaymentOrderUseCase orders = mock(GetPaymentOrderUseCase.class);
    private final GetOrderUseCase getOrder = mock(GetOrderUseCase.class);
    private final GetCouponTargetsUseCase targets = mock(GetCouponTargetsUseCase.class);
    private final FindOwnedCouponsForCheckoutUseCase coupons = mock(FindOwnedCouponsForCheckoutUseCase.class);
    private final GetPointBalanceUseCase balance = mock(GetPointBalanceUseCase.class);
    private final CreatePaymentUseCase create = mock(CreatePaymentUseCase.class);
    private final ApprovePgPaymentUseCase approve = mock(ApprovePgPaymentUseCase.class);
    private final PreparePgOperationUseCase prepare = mock(PreparePgOperationUseCase.class);
    private final FailPaymentAttemptUseCase reject = mock(FailPaymentAttemptUseCase.class);
    private final MarkPgOperationUncertainUseCase mark = mock(MarkPgOperationUncertainUseCase.class);
    private final ResolvePaymentAttemptUseCase resolve = mock(ResolvePaymentAttemptUseCase.class);
    private final RecordPaymentFailureCallbackUseCase fail = mock(RecordPaymentFailureCallbackUseCase.class);
    private final PostProcessPaymentUseCase post = mock(PostProcessPaymentUseCase.class);
    private final PaymentService service =
        new PaymentService(orders, getOrder, targets, coupons, balance, create, approve, prepare, reject, mark, resolve, fail, post);

    @Test
    void 결제_생성은_주문과_할인_값을_합쳐_시도_식별자를_반환한다() {
        final Order order = order();
        when(create.findExisting(eq("order"), eq(1L), any(), any())).thenReturn(Optional.empty());
        when(orders.execute(1L, "order")).thenReturn(order);
        when(targets.execute(List.of())).thenReturn(Set.of());
        when(coupons.execute(eq(1L), eq(Set.of()), any())).thenReturn(List.of());
        when(balance.execute(1L)).thenReturn(BigDecimal.ZERO);
        when(create.execute(eq(order), eq(1L), any(), any(), any())).thenReturn(new PaymentCreationResult(3L, "payment_attempt"));
        final PaymentCreationResult result =
            service.create(new CreatePaymentCommand(1L, "order", -1L, BigDecimal.valueOf(-1), "request-key"));
        assertThat(result).isEqualTo(new PaymentCreationResult(3L, "payment_attempt"));
        final ArgumentCaptor<PaymentDiscount> captured = ArgumentCaptor.forClass(PaymentDiscount.class);
        verify(create).execute(eq(order), eq(1L), captured.capture(), any(), any());
        assertThat(captured.getValue().paidAmount(BigDecimal.valueOf(10000))).isEqualByComparingTo("10001");
    }

    @Test
    void 동일한_결제_생성_재요청은_기존_시도를_반환한다() {
        final var result = new PaymentCreationResult(3L, "payment_attempt");
        when(create.findExisting(eq("order"), eq(1L), any(), any())).thenReturn(Optional.of(result));
        assertThat(service.create(new CreatePaymentCommand(1L, "order", -1L, BigDecimal.ZERO, "request-key"))).isEqualTo(result);
        verifyNoInteractions(balance, targets, coupons);
    }

    @Test
    void 성공_콜백의_PG주문번호를_내부_주문과_시도로_변환한다() {
        final PaymentAttempt attempt = attempt();
        when(resolve.execute("payment_attempt")).thenReturn(attempt);
        final var command = new ApprovePaymentCommand("order", "payment_attempt", "test-key", BigDecimal.TEN, 4L);
        when(approve.execute(command)).thenReturn(new PaymentCompletionResult(3L, true));
        assertThat(service.success(new ApprovePaymentCommand("payment_attempt", "test-key", BigDecimal.TEN))).isEqualTo(3L);
        final InOrder sequence = inOrder(prepare, approve, post);
        sequence.verify(prepare).execute(command.operation());
        sequence.verify(approve).execute(command);
        sequence.verify(post).execute(3L, "order");
    }

    @Test
    void 회원의_성공_콜백은_주문_소유권을_검증한_뒤_기존_결제_흐름을_실행한다() {
        when(resolve.execute("payment_attempt")).thenReturn(attempt());
        final var command = new ApprovePaymentCommand("order", "payment_attempt", "test-key", BigDecimal.TEN, 4L);
        when(approve.execute(command)).thenReturn(new PaymentCompletionResult(3L, true));

        assertThat(service.successForUser(1L, new ApprovePaymentCommand("payment_attempt", "test-key", BigDecimal.TEN))).isEqualTo(3L);
        verify(getOrder).execute(new GetOrderCommand(1L, "order"));
        verify(prepare).execute(command.operation());
    }

    @Test
    void 다른_회원의_성공_콜백은_PG_작업을_준비하지_않는다() {
        when(resolve.execute("payment_attempt")).thenReturn(attempt());
        doThrow(new CoreException(ErrorCode.FORBIDDEN)).when(getOrder).execute(new GetOrderCommand(2L, "order"));

        assertThatThrownBy(() -> service.successForUser(2L, new ApprovePaymentCommand("payment_attempt", "test-key", BigDecimal.TEN)))
            .isInstanceOfSatisfying(CoreException.class, error -> assertThat(error.errorCode()).isEqualTo(ErrorCode.FORBIDDEN));
        verifyNoInteractions(prepare, approve, mark, reject, post);
    }

    @Test
    void 미확정_PG결과는_예약을_해제하지_않고_확인_대상으로_기록한다() {
        final var command = new ApprovePaymentCommand("order", "payment_attempt", "test-key", BigDecimal.TEN, 4L);
        doThrow(new CoreException(ErrorCode.PG_PENDING)).when(approve).execute(command);
        assertThatThrownBy(() -> service.success(command)).isInstanceOf(CoreException.class);
        verify(mark).execute(command.operation(), false);
        verify(reject, never()).execute(any());
        verifyNoInteractions(post);
    }

    @Test
    void PG_최종_거절은_예약_해제_처리를_호출한다() {
        final var command = new ApprovePaymentCommand("order", "payment_attempt", "test-key", BigDecimal.TEN, 4L);
        doThrow(new CoreException(ErrorCode.PG_REJECTED)).when(approve).execute(command);
        assertThatThrownBy(() -> service.success(command)).isInstanceOf(CoreException.class);
        verify(reject).execute(command.operation());
        verifyNoInteractions(mark, post);
    }

    @Test
    void 실패_콜백은_확인_대상을_기록한_뒤_결제_확인중을_알린다() {
        when(resolve.execute("payment_attempt")).thenReturn(attempt());
        when(fail.execute(1L, "order", 4L, "FAIL", "test")).thenReturn(true);
        assertThatThrownBy(() -> service.fail(1L, new FailPaymentCommand("payment_attempt", "FAIL", "test")))
            .isInstanceOfSatisfying(CoreException.class, error -> assertThat(error.errorCode()).isEqualTo(ErrorCode.PG_PENDING));
        verify(fail).execute(1L, "order", 4L, "FAIL", "test");
        verifyNoInteractions(reject);
    }

    @Test
    void 실패_콜백_기록에_실패하면_확인중_응답으로_덮지_않는다() {
        when(resolve.execute("payment_attempt")).thenReturn(attempt());
        doThrow(new CoreException(ErrorCode.STORAGE_FAILURE)).when(fail).execute(1L, "order", 4L, "FAIL", "test");

        assertThatThrownBy(() -> service.fail(1L, new FailPaymentCommand("payment_attempt", "FAIL", "test")))
            .isInstanceOfSatisfying(CoreException.class, error -> assertThat(error.errorCode()).isEqualTo(ErrorCode.STORAGE_FAILURE));
        verifyNoInteractions(reject);
    }

    @Test
    void 영원_결제_실패가_확정된_경우에는_확인중_오류를_반환하지_않는다() {
        when(resolve.execute("payment_attempt")).thenReturn(attempt());
        when(fail.execute(1L, "order", 4L, "FAIL", "test")).thenReturn(false);

        service.fail(1L, new FailPaymentCommand("payment_attempt", "FAIL", "test"));
        verify(fail).execute(1L, "order", 4L, "FAIL", "test");
    }

    private static PaymentAttempt attempt() {
        final PaymentAttempt attempt = new PaymentAttempt(3L, "order", "payment_attempt", "request-hash", "hash");
        ReflectionTestUtils.setField(attempt, "id", 4L);
        return attempt;
    }

    private static Order order() {
        final Order order = Order.create(1L, "order", null);
        ReflectionTestUtils.setField(order, "id", 2L);
        return order;
    }
}
