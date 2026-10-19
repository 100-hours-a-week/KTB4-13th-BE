package com.book.core.order.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.cancel.application.port.CancelBalanceRepositoryPort;
import com.book.core.cancel.application.port.CancelRepositoryPort;
import com.book.core.cancel.domain.CancelBalance;
import com.book.core.order.application.command.GetOrderCommand;
import com.book.core.order.application.port.OrderRepositoryPort;
import com.book.core.order.application.result.GetOrderResult;
import com.book.core.order.application.result.OrderPaymentResult;
import com.book.core.order.domain.Order;
import com.book.core.payment.application.port.PaymentRepositoryPort;
import com.book.core.payment.domain.Payment;
import com.book.core.payment.domain.enums.PaymentMethod;
import com.book.core.payment.domain.enums.PaymentState;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class GetOrderDetailUseCaseTest {
    private final OrderRepositoryPort orders = mock(OrderRepositoryPort.class);
    private final PaymentRepositoryPort payments = mock(PaymentRepositoryPort.class);
    private final CancelBalanceRepositoryPort balances = mock(CancelBalanceRepositoryPort.class);
    private final CancelRepositoryPort cancellations = mock(CancelRepositoryPort.class);
    private final GetOrderDetailUseCase useCase = new GetOrderDetailUseCase(orders, payments, balances, cancellations);
    private final GetOrderCommand command = new GetOrderCommand(42L, "order_test");

    @Test
    void 미결제_주문은_결제_정보를_조회하지_않는다() {
        order(null);
        final GetOrderResult result = useCase.execute(command);
        assertThat(result.payment()).isNull();
        verifyNoInteractions(payments, balances);
    }

    @Test
    void 결제_주문은_Payment와_취소_잔액의_저장값으로_응답한다() {
        order(LocalDateTime.of(2026, 9, 28, 10, 0));
        final Payment payment = payment();
        when(payments.findByOrderKey("order_test")).thenReturn(Optional.of(payment));
        when(balances.findByOrderId(7L)).thenReturn(Optional.of(new CancelBalance(7L, new BigDecimal("6000.00"), BigDecimal.ZERO,
            BigDecimal.ZERO, new BigDecimal("3000.00"), new BigDecimal("500.00"), BigDecimal.ZERO)));

        final GetOrderResult result = useCase.execute(command);
        assertThat(result.payment()).isEqualTo(new OrderPaymentResult(new BigDecimal("1000.00"), new BigDecimal("500.00"),
            new BigDecimal("9000.00"), PaymentMethod.CARD, payment.paidAt(), new BigDecimal("3000.00"), new BigDecimal("6000.00")));
    }

    @Test
    void 결제_데이터가_없으면_정합성_오류다() {
        order(LocalDateTime.of(2026, 9, 28, 10, 0));
        assertInconsistent();
        verifyNoInteractions(balances);
    }

    @Test
    void 결제_미승인이면_정합성_오류다() {
        order(LocalDateTime.of(2026, 9, 28, 10, 0));
        when(payments.findByOrderKey("order_test")).thenReturn(Optional.of(new Payment(42L, 7L, new BigDecimal("10500.00"), 3L,
            new BigDecimal("1000.00"), new BigDecimal("500.00"), 42L, new BigDecimal("9000.00"), PaymentState.READY)));
        assertInconsistent();
        verifyNoInteractions(balances);
    }

    @Test
    void 환불_잔액이_없으면_정합성_오류다() {
        order(LocalDateTime.of(2026, 9, 28, 10, 0));
        when(payments.findByOrderKey("order_test")).thenReturn(Optional.of(payment()));
        assertInconsistent();
    }

    @Test
    void 다른_회원의_주문은_조회할_수_없다() {
        final Order order = mock(Order.class);
        when(order.userId()).thenReturn(11L);
        when(orders.findActiveOrderByKey("order_test")).thenReturn(Optional.of(order));
        assertThatThrownBy(() -> useCase.execute(command)).isInstanceOf(CoreException.class)
            .extracting(exception -> ((CoreException) exception).errorCode()).isEqualTo(ErrorCode.FORBIDDEN);
        verifyNoInteractions(payments, balances);
    }

    private void assertInconsistent() {
        assertThatThrownBy(() -> useCase.execute(command)).isInstanceOf(CoreException.class)
            .extracting(exception -> ((CoreException) exception).errorCode()).isEqualTo(ErrorCode.ORDER_DATA_INCONSISTENT);
    }

    private void order(final LocalDateTime paidAt) {
        final Order order = mock(Order.class);
        when(order.userId()).thenReturn(42L);
        when(order.key()).thenReturn("order_test");
        when(order.paidAt()).thenReturn(paidAt);
        when(order.items()).thenReturn(List.of());
        when(orders.findActiveOrderByKey("order_test")).thenReturn(Optional.of(order));
    }

    private static Payment payment() {
        final Payment payment = new Payment(42L, 7L, new BigDecimal("10500.00"), 3L, new BigDecimal("1000.00"), new BigDecimal("500.00"),
            42L, new BigDecimal("9000.00"), PaymentState.READY);
        payment.success("payment_key", PaymentMethod.CARD, "approve");
        return payment;
    }
}
