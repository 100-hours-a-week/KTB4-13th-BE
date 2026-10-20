package com.book.core.order.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.address.application.result.GetAddressItemResult;
import com.book.core.address.application.usecase.GetAddressUseCase;
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
import com.book.core.order.application.usecase.CreateOrderUseCase;
import com.book.core.order.application.usecase.ExpireOrderUseCase;
import com.book.core.order.application.usecase.GetExpiredOrderKeysUseCase;
import com.book.core.order.application.usecase.GetReservedOrderQuantitiesUseCase;
import com.book.core.order.application.usecase.CancelOrderUseCase;
import com.book.core.order.application.usecase.GetOrderDetailUseCase;
import com.book.core.order.application.usecase.GetOrdersUseCase;
import com.book.core.order.domain.Order;
import com.book.core.product.application.command.GetProductDetailCommand;
import com.book.core.product.application.command.ProductQuantitiesCommand;
import com.book.core.product.application.result.GetProductDetailResult;
import com.book.core.product.application.usecase.GetProductDetailUseCase;
import com.book.core.product.application.usecase.ReleaseProductInventoryUseCase;
import com.book.core.product.application.usecase.ReserveProductInventoryUseCase;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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
    private final GetOrderDetailUseCase getOrderDetailUseCase = mock(GetOrderDetailUseCase.class);
    private final ReserveProductInventoryUseCase reserveProductInventoryUseCase = mock(ReserveProductInventoryUseCase.class);
    private final ReleaseProductInventoryUseCase releaseProductInventoryUseCase = mock(ReleaseProductInventoryUseCase.class);
    private final GetReservedOrderQuantitiesUseCase getReservedOrderQuantitiesUseCase = mock(GetReservedOrderQuantitiesUseCase.class);
    private final ExpireOrderUseCase expireOrderUseCase = mock(ExpireOrderUseCase.class);
    private final OrderService service = new OrderService(getAddressUseCase, getCartUseCase, getProductDetailUseCase, createOrderUseCase,
        cancelOrderUseCase, getOrdersUseCase, getOrderDetailUseCase, reserveProductInventoryUseCase, releaseProductInventoryUseCase,
        getReservedOrderQuantitiesUseCase, mock(GetExpiredOrderKeysUseCase.class), expireOrderUseCase);

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
    void 같은_상품의_주문_수량을_합산해_예약하고_10분_뒤_만료되도록_주문을_저장한다() {
        final LocalDateTime before = LocalDateTime.now();

        service.createOrder(command(new CreateOrderItemCommand(701L, 2), new CreateOrderItemCommand(701L, 3)));

        verify(reserveProductInventoryUseCase).execute(new ProductQuantitiesCommand(Map.of(701L, 5)));
        assertThat(savedOrder().expiresAt()).isBetween(before.plusMinutes(10), LocalDateTime.now().plusMinutes(10));
    }

    @Test
    void 재고_예약에_실패하면_주문을_저장하지_않는다() {
        doThrow(new CoreException(ErrorCode.INSUFFICIENT_PRODUCT_STOCK)).when(reserveProductInventoryUseCase).execute(any());

        assertThatThrownBy(() -> service.createOrder(command(new CreateOrderItemCommand(701L, 2)))).isInstanceOf(CoreException.class);
        verifyNoInteractions(createOrderUseCase);
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

        when(getReservedOrderQuantitiesUseCase.execute("order_test")).thenReturn(Map.of(701L, 2));

        service.cancelOrder(command);

        verify(cancelOrderUseCase).execute(command);
        verify(releaseProductInventoryUseCase).execute(new ProductQuantitiesCommand(Map.of(701L, 2)));
    }

    @Test
    void 예약이_없는_주문은_취소하거나_만료하지_못해도_재고를_반환하지_않는다() {
        when(getReservedOrderQuantitiesUseCase.execute("order_test")).thenReturn(Map.of());
        when(expireOrderUseCase.execute(any(), any())).thenReturn(Map.of());

        service.cancelOrder(new CancelOrderCommand(42L, "order_test"));
        service.expireOrder("order_test");

        verifyNoInteractions(releaseProductInventoryUseCase);
    }

    @Test
    void 만료된_주문의_예약_수량을_반환한다() {
        when(expireOrderUseCase.execute(any(), any())).thenReturn(Map.of(701L, 3));

        service.expireOrder("order_test");

        verify(releaseProductInventoryUseCase).execute(new ProductQuantitiesCommand(Map.of(701L, 3)));
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
    void 주문_상세_조회를_UseCase에_위임한다() {
        final GetOrderCommand command = new GetOrderCommand(42L, "order_test");
        final GetOrderResult result = mock(GetOrderResult.class);
        when(getOrderDetailUseCase.execute(command)).thenReturn(result);

        assertThat(service.getOrder(command)).isSameAs(result);
        verify(getOrderDetailUseCase).execute(command);
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
