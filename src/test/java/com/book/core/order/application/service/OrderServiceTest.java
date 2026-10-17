package com.book.core.order.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.address.application.result.GetAddressItemResult;
import com.book.core.address.application.usecase.GetAddressUseCase;
import com.book.core.cancel.application.usecase.GetCancelBalanceUseCase;
import com.book.core.cancel.application.usecase.GetCancelsUseCase;
import com.book.core.cancel.domain.Cancel;
import com.book.core.cancel.domain.CancelBalance;
import com.book.core.cancel.domain.enums.CancelType;
import com.book.core.cart.application.result.GetCartItemResult;
import com.book.core.cart.application.result.GetCartResult;
import com.book.core.cart.application.usecase.GetCartUseCase;
import com.book.core.order.application.command.CancelOrderCommand;
import com.book.core.order.application.command.CreateOrderCommand;
import com.book.core.order.application.command.CreateOrderItemCommand;
import com.book.core.order.application.command.GetOrderCommand;
import com.book.core.order.application.command.GetOrdersCommand;
import com.book.core.order.application.result.GetOrderResult;
import com.book.core.order.application.result.GetOrdersResult;
import com.book.core.order.application.result.CreateOrderResult;
import com.book.core.order.application.result.OrderCancellationItemResult;
import com.book.core.order.application.result.OrderCancellationResult;
import com.book.core.order.application.result.OrderItemResult;
import com.book.core.order.application.result.OrderPaymentResult;
import com.book.core.order.application.usecase.CreateOrderUseCase;
import com.book.core.order.application.usecase.CancelOrderUseCase;
import com.book.core.order.application.usecase.GetOrderUseCase;
import com.book.core.order.application.usecase.GetOrdersUseCase;
import com.book.core.order.domain.Order;
import com.book.core.order.domain.OrderItemStatus;
import com.book.core.order.domain.OrderStatus;
import com.book.core.payment.application.usecase.GetPaymentUseCase;
import com.book.core.payment.domain.Payment;
import com.book.core.payment.domain.enums.PaymentMethod;
import com.book.core.payment.domain.enums.PaymentState;
import com.book.core.product.application.command.GetProductDetailCommand;
import com.book.core.product.application.result.GetProductDetailResult;
import com.book.core.product.application.usecase.GetProductDetailUseCase;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class OrderServiceTest {
    private final GetAddressUseCase getAddressUseCase = mock(GetAddressUseCase.class);
    private final GetCartUseCase getCartUseCase = mock(GetCartUseCase.class);
    private final GetProductDetailUseCase getProductDetailUseCase = mock(GetProductDetailUseCase.class);
    private final CreateOrderUseCase createOrderUseCase = mock(CreateOrderUseCase.class);
    private final CancelOrderUseCase cancelOrderUseCase = mock(CancelOrderUseCase.class);
    private final GetOrdersUseCase getOrdersUseCase = mock(GetOrdersUseCase.class);
    private final GetOrderUseCase getOrderUseCase = mock(GetOrderUseCase.class);
    private final GetPaymentUseCase getPaymentUseCase = mock(GetPaymentUseCase.class);
    private final GetCancelBalanceUseCase getCancelBalanceUseCase = mock(GetCancelBalanceUseCase.class);
    private final GetCancelsUseCase getCancelsUseCase = mock(GetCancelsUseCase.class);
    private final OrderService service = new OrderService(getAddressUseCase, getCartUseCase, getProductDetailUseCase, createOrderUseCase,
        cancelOrderUseCase, getOrdersUseCase, getOrderUseCase, getPaymentUseCase, getCancelBalanceUseCase, getCancelsUseCase);

    @BeforeEach
    void setUp() {
        when(getAddressUseCase.execute(42L)).thenReturn(Optional.of(address()));
        when(getCartUseCase.execute(42L)).thenReturn(new GetCartResult(List
            .of(new GetCartItemResult(17L, 701L, "장바구니 상품", "thumbnail.jpg", new BigDecimal("20.00"), new BigDecimal("17.25"), 4, true))));
        when(getProductDetailUseCase.execute(any())).thenReturn(product(10));
        when(createOrderUseCase.execute(any(Order.class))).thenAnswer(invocation -> {
            final Order order = invocation.getArgument(0);
            return new CreateOrderResult(order.key(), order.address() != null, order.totalPrice(), List.of());
        });
    }

    @Test
    void 회원의_기본_배송지와_장바구니_상품의_서버_가격으로_주문_스냅샷을_생성한다() {
        final CreateOrderResult result = service.createOrder(command(new CreateOrderItemCommand(701L, 2)));

        final Order order = savedOrder();
        assertThat(order.userId()).isEqualTo(42L);
        assertThat(order.address().postalCode()).isEqualTo("06236");
        assertThat(order.address().address()).isEqualTo("서울 주소");
        assertThat(order.totalPrice()).isEqualByComparingTo("34.50");
        assertThat(order.items()).singleElement().satisfies(item -> {
            assertThat(item.productId()).isEqualTo(701L);
            assertThat(item.itemName()).isEqualTo("상품 701");
            assertThat(item.thumbnailUrl()).isEqualTo("https://example.com/book.jpg");
            assertThat(item.author()).isEqualTo("저자");
            assertThat(item.salePrice()).isEqualByComparingTo("20.00");
            assertThat(item.unitPrice()).isEqualByComparingTo("17.25");
            assertThat(item.quantity()).isEqualTo(2);
            assertThat(item.totalPrice()).isEqualByComparingTo("34.50");
        });
        verify(getAddressUseCase).execute(42L);
        verify(getCartUseCase).execute(42L);
        verify(getProductDetailUseCase).execute(GetProductDetailCommand.of(701L));
    }

    @Test
    void 기본_배송지가_없어도_주문_스냅샷을_생성한다() {
        when(getAddressUseCase.execute(42L)).thenReturn(Optional.empty());

        final CreateOrderResult result = service.createOrder(command(new CreateOrderItemCommand(701L, 2)));

        final Order order = savedOrder();
        assertThat(order.address()).isNull();
        assertThat(order.totalPrice()).isEqualByComparingTo("34.50");
        assertThat(order.items()).hasSize(1);
        assertThat(result.canProceedToPayment()).isFalse();
        verify(getCartUseCase).execute(42L);
        verify(getProductDetailUseCase).execute(GetProductDetailCommand.of(701L));
    }

    @Test
    void 장바구니에_없는_상품은_주문하지_않는다() {
        when(getCartUseCase.execute(42L)).thenReturn(GetCartResult.empty());

        assertThatThrownBy(() -> service.createOrder(command(new CreateOrderItemCommand(999L, 1)))).isInstanceOf(CoreException.class)
            .extracting(exception -> ((CoreException) exception).errorCode()).isEqualTo(ErrorCode.PRODUCT_MISMATCH_IN_ORDER);
        verifyNoInteractions(getProductDetailUseCase, createOrderUseCase);
    }

    @Test
    void 현재_재고가_요청_수량보다_적으면_주문을_저장하지_않는다() {
        when(getProductDetailUseCase.execute(any())).thenReturn(product(1));

        assertThatThrownBy(() -> service.createOrder(command(new CreateOrderItemCommand(701L, 2)))).isInstanceOf(CoreException.class)
            .extracting(exception -> ((CoreException) exception).errorCode()).isEqualTo(ErrorCode.INSUFFICIENT_PRODUCT_STOCK);
        verifyNoInteractions(createOrderUseCase);
    }

    @Test
    void 같은_상품이_여러_줄이면_합산_수량으로_재고를_검증한다() {
        when(getProductDetailUseCase.execute(any())).thenReturn(product(3));

        assertThatThrownBy(() -> service.createOrder(command(new CreateOrderItemCommand(701L, 2), new CreateOrderItemCommand(701L, 2))))
            .isInstanceOf(CoreException.class).extracting(exception -> ((CoreException) exception).errorCode())
            .isEqualTo(ErrorCode.INSUFFICIENT_PRODUCT_STOCK);
        verifyNoInteractions(createOrderUseCase);
    }

    @Test
    void 활성_상품을_찾지_못하면_주문을_저장하지_않는다() {
        when(getProductDetailUseCase.execute(any())).thenThrow(new CoreException(ErrorCode.PRODUCT_NOT_FOUND));

        assertThatThrownBy(() -> service.createOrder(command(new CreateOrderItemCommand(701L, 1)))).isInstanceOf(CoreException.class)
            .extracting(exception -> ((CoreException) exception).errorCode()).isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);
        verifyNoInteractions(createOrderUseCase);
    }

    @Test
    void 주문_취소_요청을_취소_UseCase로_전달한다() {
        final CancelOrderCommand command = new CancelOrderCommand(42L, "order_test");

        service.cancelOrder(command);

        verify(cancelOrderUseCase).execute(command);
    }

    @Test
    void 주문_목록_조회를_UseCase에_위임한다() {
        final GetOrdersCommand command = new GetOrdersCommand(42L, null, LocalDate.of(2025, 9, 28), LocalDate.of(2026, 9, 28), null, 20);
        final GetOrdersResult result = GetOrdersResult.of(List.of(), null);
        when(getOrdersUseCase.execute(command)).thenReturn(result);

        assertThat(service.getOrders(command)).isSameAs(result);
        verify(getOrdersUseCase).execute(command);
    }

    @Test
    void 미결제_주문_상세는_결제_정보를_조회하지_않고_payment_없이_빈_취소_이력으로_반환한다() {
        final GetOrderCommand command = new GetOrderCommand(42L, "order_test");
        final GetOrderResult result = GetOrderResult.from(Order.create(42L, "order_test", null));
        when(getOrderUseCase.execute(command)).thenReturn(result);

        assertThat(service.getOrder(command)).isSameAs(result).satisfies((final GetOrderResult detail) -> {
            assertThat(detail.payment()).isNull();
            assertThat(detail.cancellations()).isEmpty();
        });
        verifyNoInteractions(getPaymentUseCase, getCancelBalanceUseCase, getCancelsUseCase);
    }

    @Test
    void 결제_주문_상세는_결제_요약과_취소_기록의_대상_상품과_수량을_구성한다() {
        final GetOrderCommand command = new GetOrderCommand(42L, "order_test");
        final Payment payment = payment();
        when(getOrderUseCase.execute(command)).thenReturn(orderDetail(LocalDateTime.of(2026, 9, 28, 10, 0)));
        when(getPaymentUseCase.execute("order_test")).thenReturn(payment);
        when(getCancelBalanceUseCase.execute(7L)).thenReturn(new CancelBalance(7L, new BigDecimal("6000.00"), BigDecimal.ZERO,
            BigDecimal.ZERO, new BigDecimal("3000.00"), new BigDecimal("500.00"), BigDecimal.ZERO));
        final LocalDateTime partialCanceledAt = LocalDateTime.of(2026, 9, 29, 10, 0);
        final LocalDateTime allCanceledAt = LocalDateTime.of(2026, 9, 30, 10, 0);
        when(getCancelsUseCase.execute(7L)).thenReturn(List.of(cancel(CancelType.ALL, -1L, -1L, "7000.00", allCanceledAt),
            cancel(CancelType.PARTIAL, 12L, 1L, "2000.00", partialCanceledAt)));

        final GetOrderResult result = service.getOrder(command);

        assertThat(result.payment()).isEqualTo(new OrderPaymentResult(new BigDecimal("1000.00"), new BigDecimal("500.00"),
            new BigDecimal("9000.00"), PaymentMethod.CARD, payment.paidAt(), new BigDecimal("3000.00"), new BigDecimal("6000.00")));
        assertThat(result.items()).isEqualTo(orderDetail(null).items());
        assertThat(result.cancellations()).containsExactly(
            new OrderCancellationResult(CancelType.ALL, new BigDecimal("7000.00"), allCanceledAt,
                List.of(new OrderCancellationItemResult(11L, 701L, "상품 701", 2L),
                    new OrderCancellationItemResult(12L, 702L, "상품 702", 3L))),
            new OrderCancellationResult(CancelType.PARTIAL, new BigDecimal("2000.00"), partialCanceledAt,
                List.of(new OrderCancellationItemResult(12L, 702L, "상품 702", 1L))));
    }

    @Test
    void 취소_기록의_주문상품_참조가_없으면_정합성_오류다() {
        final GetOrderCommand command = new GetOrderCommand(42L, "order_test");
        when(getOrderUseCase.execute(command)).thenReturn(orderDetail(LocalDateTime.of(2026, 9, 28, 10, 0)));
        when(getPaymentUseCase.execute("order_test")).thenReturn(payment());
        when(getCancelBalanceUseCase.execute(7L)).thenReturn(
            new CancelBalance(7L, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
        when(getCancelsUseCase.execute(7L))
            .thenReturn(List.of(cancel(CancelType.PARTIAL, 99L, 1L, "1000.00", LocalDateTime.of(2026, 9, 29, 10, 0))));

        assertOrderDataInconsistent(command);
    }

    @Test
    void 결제_주문의_Payment가_없으면_취소_잔액을_조회하지_않고_정합성_오류다() {
        final GetOrderCommand command = new GetOrderCommand(42L, "order_test");
        when(getOrderUseCase.execute(command)).thenReturn(orderDetail(LocalDateTime.of(2026, 9, 28, 10, 0)));
        when(getPaymentUseCase.execute("order_test")).thenThrow(new CoreException(ErrorCode.NOT_FOUND_DATA));

        assertOrderDataInconsistent(command);
        verifyNoInteractions(getCancelBalanceUseCase);
    }

    @Test
    void 결제_주문의_Payment가_승인_완료_상태가_아니면_취소_잔액을_조회하지_않고_정합성_오류다() {
        final GetOrderCommand command = new GetOrderCommand(42L, "order_test");
        when(getOrderUseCase.execute(command)).thenReturn(orderDetail(LocalDateTime.of(2026, 9, 28, 10, 0)));
        when(getPaymentUseCase.execute("order_test")).thenReturn(new Payment(42L, 7L, new BigDecimal("10500.00"), 3L,
            new BigDecimal("1000.00"), new BigDecimal("500.00"), 42L, new BigDecimal("9000.00"), PaymentState.READY));

        assertOrderDataInconsistent(command);
        verifyNoInteractions(getCancelBalanceUseCase);
    }

    @Test
    void 결제_주문의_취소_잔액이_없으면_정합성_오류다() {
        final GetOrderCommand command = new GetOrderCommand(42L, "order_test");
        when(getOrderUseCase.execute(command)).thenReturn(orderDetail(LocalDateTime.of(2026, 9, 28, 10, 0)));
        when(getPaymentUseCase.execute("order_test")).thenReturn(payment());
        when(getCancelBalanceUseCase.execute(7L)).thenThrow(new CoreException(ErrorCode.NOT_FOUND_DATA));

        assertOrderDataInconsistent(command);
    }

    private void assertOrderDataInconsistent(final GetOrderCommand command) {
        assertThatThrownBy(() -> service.getOrder(command)).isInstanceOf(CoreException.class)
            .extracting(exception -> ((CoreException) exception).errorCode()).isEqualTo(ErrorCode.ORDER_DATA_INCONSISTENT);
    }

    private static GetOrderResult orderDetail(final LocalDateTime paidAt) {
        return new GetOrderResult("order_test", OrderStatus.PARTIAL_CANCELED, new BigDecimal("10000.00"),
            LocalDateTime.of(2026, 9, 28, 9, 0), paidAt,
            List.of(new OrderItemResult(11L, 701L, "상품 701", null, "저자", BigDecimal.TEN, BigDecimal.TEN, 2, 0L, OrderItemStatus.PAID),
                new OrderItemResult(12L, 702L, "상품 702", null, "저자", BigDecimal.TEN, BigDecimal.TEN, 3, 1L,
                    OrderItemStatus.PARTIAL_CANCELED)),
            null, null, List.of());
    }

    private static Cancel cancel(final CancelType type, final long orderItemId, final long quantity, final String refundedAmount,
        final LocalDateTime canceledAt) {
        final BigDecimal amount = new BigDecimal(refundedAmount);
        return new Cancel(42L, type, 7L, orderItemId, 3L, BigDecimal.TEN, 0L, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.TEN, quantity,
            amount, BigDecimal.ZERO, BigDecimal.ZERO, "cancel_key", canceledAt);
    }

    private static Payment payment() {
        final Payment payment = new Payment(42L, 7L, new BigDecimal("10500.00"), 3L, new BigDecimal("1000.00"), new BigDecimal("500.00"),
            42L, new BigDecimal("9000.00"), PaymentState.READY);
        payment.success("payment_key", PaymentMethod.CARD, "approve");
        return payment;
    }

    private Order savedOrder() {
        final ArgumentCaptor<Order> order = ArgumentCaptor.forClass(Order.class);
        verify(createOrderUseCase).execute(order.capture());
        return order.getValue();
    }

    private static CreateOrderCommand command(final CreateOrderItemCommand... items) {
        return new CreateOrderCommand(42L, List.of(items));
    }

    private static GetAddressItemResult address() {
        return new GetAddressItemResult(101L, "집", "06236", "서울 주소", "101호", true);
    }

    private static GetProductDetailResult product(final int stock) {
        return new GetProductDetailResult(701L, "상품 701", "https://example.com/book.jpg", "저자", "출판사", LocalDate.of(2026, 1, 1), "소설", null,
            new BigDecimal("20.00"), new BigDecimal("17.25"), 0L, BigDecimal.ZERO, stock, List.of());
    }
}
