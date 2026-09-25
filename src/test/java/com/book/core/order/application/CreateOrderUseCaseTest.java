package com.book.core.order.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.address.application.command.GetAddressesCommand;
import com.book.core.address.application.result.GetAddressItemResult;
import com.book.core.address.application.result.GetAddressesResult;
import com.book.core.address.application.usecase.GetAddressesUseCase;
import com.book.core.cart.application.command.GetCartCommand;
import com.book.core.cart.application.result.GetCartItemResult;
import com.book.core.cart.application.result.GetCartResult;
import com.book.core.cart.application.usecase.GetCartUseCase;
import com.book.core.order.application.command.CreateOrderCommand;
import com.book.core.order.application.command.CreateOrderItemCommand;
import com.book.core.order.application.port.OrderRepositoryPort;
import com.book.core.order.application.usecase.CreateOrderUseCase;
import com.book.core.order.domain.Order;
import com.book.core.order.domain.OrderItemStatus;
import com.book.core.order.domain.OrderStatus;
import com.book.core.product.application.result.GetProductDetailResult;
import com.book.core.product.application.usecase.GetProductDetailUseCase;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

class CreateOrderUseCaseTest {
    private final GetAddressesUseCase getAddressesUseCase = mock(GetAddressesUseCase.class);
    private final GetCartUseCase getCartUseCase = mock(GetCartUseCase.class);
    private final GetProductDetailUseCase getProductDetailUseCase = mock(GetProductDetailUseCase.class);
    private final OrderRepositoryPort orderRepository = mock(OrderRepositoryPort.class);
    private final CreateOrderUseCase useCase =
            new CreateOrderUseCase(getAddressesUseCase, getCartUseCase, getProductDetailUseCase, orderRepository);

    @BeforeEach
    void setUp() {
        when(getAddressesUseCase.execute(any())).thenReturn(new GetAddressesResult(List.of(address(101L, "06236"))));
        when(getCartUseCase.execute(any())).thenReturn(new GetCartResult(List.of(new GetCartItemResult(17L, 701L, 4))));
        when(getProductDetailUseCase.execute(any())).thenReturn(product(701L, "17.25", 10));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void 활성_장바구니_상품과_배송지로_현재_할인가_기준_주문을_생성한다() {
        final var result = useCase.execute(command(101L, 701L, 2));

        assertThat(result.orderKey()).startsWith("order_");
        final var order = org.mockito.ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(order.capture());
        assertThat(order.getValue().userId()).isEqualTo(42L);
        assertThat(order.getValue().status()).isEqualTo(OrderStatus.CREATED);
        assertThat(order.getValue().totalPrice()).isEqualByComparingTo("34.50");
        assertThat(order.getValue().address().postalCode()).isEqualTo("06236");
        assertThat(order.getValue().items()).singleElement().satisfies(item -> {
            assertThat(item.productId()).isEqualTo(701L);
            assertThat(item.quantity()).isEqualTo(2);
            assertThat(item.unitPrice()).isEqualByComparingTo("17.25");
            assertThat(item.totalPrice()).isEqualByComparingTo("34.50");
            assertThat(item.status()).isEqualTo(OrderItemStatus.CREATED);
        });
        verify(getAddressesUseCase).execute(new GetAddressesCommand(42L));
        verify(getCartUseCase).execute(new GetCartCommand(42L));
    }

    @Test
    void 요청한_배송지가_회원_활성_배송지에_없으면_주문을_거부한다() {
        when(getAddressesUseCase.execute(any())).thenReturn(new GetAddressesResult(List.of()));

        assertThatThrownBy(() -> useCase.execute(command(999L, 701L, 1)))
                .isInstanceOf(CoreException.class)
                .extracting(exception -> ((CoreException) exception).errorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
        verifyNoInteractions(getCartUseCase, getProductDetailUseCase, orderRepository);
    }

    @Test
    void 비활성_장바구니나_장바구니에_없는_상품은_주문할_수_없다() {
        when(getCartUseCase.execute(any())).thenReturn(GetCartResult.empty());

        assertThatThrownBy(() -> useCase.execute(command(101L, 701L, 1)))
                .isInstanceOf(CoreException.class)
                .extracting(exception -> ((CoreException) exception).errorCode())
                .isEqualTo(ErrorCode.PRODUCT_MISMATCH_IN_ORDER);
        verifyNoInteractions(getProductDetailUseCase, orderRepository);
    }

    @Test
    void 요청_수량이_현재_재고를_초과하면_주문을_저장하지_않는다() {
        when(getProductDetailUseCase.execute(any())).thenReturn(product(701L, "17.25", 1));

        assertThatThrownBy(() -> useCase.execute(command(101L, 701L, 2)))
                .isInstanceOf(CoreException.class)
                .extracting(exception -> ((CoreException) exception).errorCode())
                .isEqualTo(ErrorCode.PRODUCT_MISMATCH_IN_ORDER);
        verifyNoInteractions(orderRepository);
    }

    @Test
    void 같은_상품이_여러_줄이면_합산_수량으로_재고를_검증한다() {
        when(getProductDetailUseCase.execute(any())).thenReturn(product(701L, "17.25", 3));
        final var command = new CreateOrderCommand(
                42L, 101L, List.of(new CreateOrderItemCommand(701L, 2), new CreateOrderItemCommand(701L, 2)));

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(CoreException.class)
                .extracting(exception -> ((CoreException) exception).errorCode())
                .isEqualTo(ErrorCode.PRODUCT_MISMATCH_IN_ORDER);
        verifyNoInteractions(orderRepository);
    }

    @Test
    void 비활성_상품과_도서_조회_오류를_주문_생성에_전파한다() {
        when(getProductDetailUseCase.execute(any())).thenThrow(new CoreException(ErrorCode.PRODUCT_NOT_FOUND));

        assertThatThrownBy(() -> useCase.execute(command(101L, 701L, 1)))
                .isInstanceOf(CoreException.class)
                .extracting(exception -> ((CoreException) exception).errorCode())
                .isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);
        verifyNoInteractions(orderRepository);
    }

    @Test
    void 주문_스냅샷_우편번호가_다섯_자를_넘으면_주문을_저장하지_않는다() {
        when(getAddressesUseCase.execute(any())).thenReturn(new GetAddressesResult(List.of(address(101L, "123456"))));

        assertThatThrownBy(() -> useCase.execute(command(101L, 701L, 1)))
                .isInstanceOf(CoreException.class)
                .extracting(exception -> ((CoreException) exception).errorCode())
                .isEqualTo(ErrorCode.INVALID_REQUEST);
        verifyNoInteractions(orderRepository);
    }

    @Test
    void 생성은_쓰기_트랜잭션에서_실행한다() throws NoSuchMethodException {
        final var method = CreateOrderUseCase.class.getMethod("execute", CreateOrderCommand.class);

        assertThat(method.getAnnotation(Transactional.class)).isNotNull();
        assertThat(method.getAnnotation(Transactional.class).readOnly()).isFalse();
    }

    private static CreateOrderCommand command(final Long addressId, final Long productId, final int quantity) {
        return new CreateOrderCommand(42L, addressId, List.of(new CreateOrderItemCommand(productId, quantity)));
    }

    private static GetAddressItemResult address(final Long addressId, final String postalCode) {
        return new GetAddressItemResult(addressId, "집", postalCode, "서울 주소", "101호", true);
    }

    private static GetProductDetailResult product(final Long productId, final String discountedPrice, final int stock) {
        return new GetProductDetailResult(
                productId,
                "상품",
                null,
                "저자",
                "출판사",
                LocalDate.of(2026, 1, 1),
                new BigDecimal("20.00"),
                new BigDecimal(discountedPrice),
                0L,
                BigDecimal.ZERO,
                stock,
                List.of());
    }
}
