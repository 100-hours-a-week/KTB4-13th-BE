package com.book.core.order.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.order.application.command.GetOrdersCommand;
import com.book.core.order.application.command.OrderListCursor;
import com.book.core.order.application.port.OrderRepositoryPort;
import com.book.core.order.application.usecase.GetOrdersUseCase;
import com.book.core.order.domain.Order;
import com.book.core.order.domain.OrderItem;
import com.book.core.order.domain.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.stream.Stream;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class GetOrdersUseCaseTest {
    private static final LocalDate FROM = LocalDate.of(2026, 9, 1);
    private static final LocalDate TO = LocalDate.of(2026, 9, 28);

    private final OrderRepositoryPort orderRepository = mock(OrderRepositoryPort.class);
    private final GetOrdersUseCase useCase = new GetOrdersUseCase(orderRepository);

    @Test
    void 결제일_필터를_시작일_0시_이상_종료일_다음날_0시_미만으로_조회한다() {
        final GetOrdersCommand command = new GetOrdersCommand(42L, OrderStatus.PAID, FROM, TO, null, 20);
        final Order order = order(2L, "order_new", LocalDateTime.of(2026, 9, 28, 10, 0));
        final LocalDateTime paidAtFrom = LocalDateTime.of(2026, 9, 1, 0, 0);
        final LocalDateTime paidAtBefore = LocalDateTime.of(2026, 9, 29, 0, 0);
        when(orderRepository.findPaidOrders(42L, OrderStatus.PAID, paidAtFrom, paidAtBefore, null, 21)).thenReturn(List.of(order));

        final var result = useCase.execute(command);

        assertThat(result.orders()).singleElement().satisfies((final var summary) -> {
            assertThat(summary.key()).isEqualTo("order_new");
            assertThat(summary.representativeBookName()).isEqualTo("대표 도서");
            assertThat(summary.itemCount()).isEqualTo(3);
            assertThat(summary.thumbnailUrl()).isEqualTo("https://img.test/1.jpg");
            assertThat(summary.paidAt()).isEqualTo(LocalDateTime.of(2026, 9, 28, 10, 0));
        });
        assertThat(result.nextCursor()).isNull();
        verify(orderRepository).findPaidOrders(42L, OrderStatus.PAID, paidAtFrom, paidAtBefore, null, 21);
    }

    @Test
    void 날짜_필터가_없으면_기간을_제한하지_않는다() {
        when(orderRepository.findPaidOrders(42L, null, null, null, null, 21)).thenReturn(List.of());

        useCase.execute(new GetOrdersCommand(42L, null, null, null, null, 20));

        verify(orderRepository).findPaidOrders(42L, null, null, null, null, 21);
    }

    @Test
    void limit보다_하나_더_조회해_마지막_노출_주문의_paidAt과_ID로_다음_커서를_만든다() {
        final GetOrdersCommand command = new GetOrdersCommand(42L, null, null, TO, null, 2);
        final LocalDateTime samePaidAt = LocalDateTime.of(2026, 9, 28, 10, 0, 0, 123_456_000);
        final List<Order> orders =
            List.of(order(3L, "order_3", samePaidAt), order(2L, "order_2", samePaidAt), order(1L, "order_1", samePaidAt));
        when(orderRepository.findPaidOrders(42L, null, null, LocalDateTime.of(2026, 9, 29, 0, 0), null, 3)).thenReturn(orders);

        final var result = useCase.execute(command);

        assertThat(result.orders()).extracting(order -> order.key()).containsExactly("order_3", "order_2");
        assertThat(OrderListCursor.parse(result.nextCursor(), null, null, TO)).isEqualTo(new OrderListCursor(samePaidAt, 2L));
    }

    @ParameterizedTest
    @MethodSource("invalidCommands")
    void 생성_상태_필터_역전된_기간_범위_밖_limit은_요청_오류다(final ThrowingCallable command) {
        assertInvalidRequest(command);
    }

    static Stream<ThrowingCallable> invalidCommands() {
        return Stream.of(() -> new GetOrdersCommand(42L, OrderStatus.CREATED, null, null, null, 20),
            () -> new GetOrdersCommand(42L, null, TO, FROM, null, 20), () -> new GetOrdersCommand(42L, null, null, null, null, 0),
            () -> new GetOrdersCommand(42L, null, null, null, null, 101),
            () -> new GetOrdersCommand(42L, null, null, LocalDate.of(9999, 12, 31), null, 20));
    }

    @Test
    void 같은_날짜의_시작일과_종료일과_최대_limit을_허용한다() {
        assertThatCode(() -> new GetOrdersCommand(42L, OrderStatus.CANCELED, TO, TO, null, 100)).doesNotThrowAnyException();
    }

    @Test
    void 필터가_다르거나_손상된_커서는_요청_오류다() {
        final String cursor = new OrderListCursor(LocalDateTime.of(2026, 9, 28, 10, 0), 2L).toToken(OrderStatus.PAID, FROM, TO);

        assertThat(OrderListCursor.parse(cursor, OrderStatus.PAID, FROM, TO))
            .isEqualTo(new OrderListCursor(LocalDateTime.of(2026, 9, 28, 10, 0), 2L));
        assertInvalidRequest(() -> OrderListCursor.parse(cursor, OrderStatus.CANCELED, FROM, TO));
        assertInvalidRequest(() -> OrderListCursor.parse(cursor, OrderStatus.PAID, null, TO));
        assertInvalidRequest(() -> OrderListCursor.parse(cursor, OrderStatus.PAID, FROM, TO.plusDays(1)));
        assertInvalidRequest(() -> OrderListCursor.parse("not+base64", null, null, null));
        assertInvalidRequest(() -> OrderListCursor.parse(encode("2026-09-28T10:00|abc|||"), null, null, null));
        assertInvalidRequest(() -> OrderListCursor.parse(encode("2026-09-28T10:00|2||"), null, null, null));
    }

    private static void assertInvalidRequest(final ThrowingCallable callable) {
        assertThatThrownBy(callable).isInstanceOf(CoreException.class).extracting(exception -> ((CoreException) exception).errorCode())
            .isEqualTo(ErrorCode.INVALID_REQUEST);
    }

    private static String encode(final String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes());
    }

    private static Order order(final Long id, final String key, final LocalDateTime paidAt) {
        final OrderItem representativeItem = mock(OrderItem.class);
        when(representativeItem.itemName()).thenReturn("대표 도서");
        when(representativeItem.thumbnailUrl()).thenReturn("https://img.test/1.jpg");
        final Order order = mock(Order.class);
        when(order.id()).thenReturn(id);
        when(order.key()).thenReturn(key);
        when(order.representativeItem()).thenReturn(representativeItem);
        when(order.itemCount()).thenReturn(3);
        when(order.status()).thenReturn(OrderStatus.PAID);
        when(order.totalPrice()).thenReturn(BigDecimal.TEN);
        when(order.paidAt()).thenReturn(paidAt);
        return order;
    }
}
