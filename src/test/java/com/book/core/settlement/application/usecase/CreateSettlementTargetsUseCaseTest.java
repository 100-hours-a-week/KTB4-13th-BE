package com.book.core.settlement.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.book.core.cancel.domain.Cancel;
import com.book.core.cancel.domain.enums.CancelType;
import com.book.core.merchant.application.usecase.GetMerchantProductMappingsUseCase;
import com.book.core.merchant.domain.MerchantProductMapping;
import com.book.core.order.application.usecase.GetOrderItemsForSettlementUseCase;
import com.book.core.order.domain.Order;
import com.book.core.order.domain.OrderItem;
import com.book.core.payment.domain.Payment;
import com.book.core.payment.domain.enums.TransactionType;
import com.book.core.settlement.application.port.SettlementTargetRepositoryPort;
import com.book.core.settlement.domain.SettlementTarget;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CreateSettlementTargetsUseCaseTest {
    private final SettlementTargetRepositoryPort repository = mock(SettlementTargetRepositoryPort.class);
    private final GetOrderItemsForSettlementUseCase orders = mock(GetOrderItemsForSettlementUseCase.class);
    private final GetMerchantProductMappingsUseCase merchants = mock(GetMerchantProductMappingsUseCase.class);
    private final CreateSettlementTargetsUseCase useCase = new CreateSettlementTargetsUseCase(repository, orders, merchants);
    private final LocalDate date = LocalDate.of(2026, 10, 16);

    @Test
    void 결제는_매핑된_상품의_주문금액을_할인과_관계없이_양수로_적재한다() {
        final var payment = mock(Payment.class);
        when(payment.id()).thenReturn(20L);
        when(payment.orderId()).thenReturn(1L);
        final var item = item(2L, 3L);
        final var unmapped = item(4L, 5L);
        when(orders.byOrderIds(anyCollection())).thenReturn(List.of(item, unmapped));
        when(merchants.execute(anyCollection())).thenReturn(List.of(new MerchantProductMapping(3L, 7L)));
        useCase.processPayments(date, List.of(payment));
        final var target = captured().getFirst();
        assertThat(target.merchantId()).isEqualTo(7L);
        assertThat(target.transactionType()).isEqualTo(TransactionType.PAYMENT);
        assertThat(target.transactionId()).isEqualTo(20L);
        assertThat(target.orderId()).isEqualTo(1L);
        assertThat(target.productId()).isEqualTo(3L);
        assertThat(target.quantity()).isEqualTo(3L);
        assertThat(target.targetAmount()).isEqualByComparingTo("300");
        assertThat(target.settlementDate()).isEqualTo(date);
    }

    @Test
    void 전체취소와_부분취소는_각각_전체금액과_취소수량금액을_음수로_적재한다() {
        final var item = item(2L, 3L);
        when(orders.byOrderIds(anyCollection())).thenReturn(List.of(item));
        when(orders.byOrderItemIds(anyCollection())).thenReturn(List.of(item));
        when(merchants.execute(anyCollection())).thenReturn(List.of(new MerchantProductMapping(3L, 7L)));
        final var all = cancel(10L, CancelType.ALL, 2L, 3);
        final var partial = cancel(11L, CancelType.PARTIAL, 2L, 1);
        final var missing = cancel(12L, CancelType.PARTIAL, 999L, 1);
        useCase.processCancels(date, List.of(all, partial, missing));
        final var targets = captured();
        assertThat(targets).hasSize(2);
        assertThat(targets.getFirst().transactionType()).isEqualTo(TransactionType.CANCEL);
        assertThat(targets.getFirst().targetAmount()).isEqualByComparingTo("-300");
        assertThat(targets.getLast().transactionType()).isEqualTo(TransactionType.PARTIAL_CANCEL);
        assertThat(targets.getLast().quantity()).isEqualTo(1L);
        assertThat(targets.getLast().totalPrice()).isEqualByComparingTo("100");
        assertThat(targets.getLast().targetAmount()).isEqualByComparingTo("-100");
        assertThat(targets.getLast().transactionId()).isEqualTo(11L);
    }

    @Test
    void 동일상품의_여러_매핑은_참고처럼_마지막_매핑을_사용한다() {
        final var item = item(2L, 3L);
        when(orders.byOrderIds(anyCollection())).thenReturn(List.of(item));
        when(merchants.execute(anyCollection()))
            .thenReturn(List.of(new MerchantProductMapping(3L, 7L), new MerchantProductMapping(3L, 8L)));
        final var payment = mock(Payment.class);
        when(payment.orderId()).thenReturn(1L);
        when(payment.id()).thenReturn(20L);
        useCase.processPayments(date, List.of(payment));
        assertThat(captured().getFirst().merchantId()).isEqualTo(8L);
    }

    @Test
    void 빈취소는_빈대상을_저장한다() {
        when(orders.byOrderIds(anyCollection())).thenReturn(List.of());
        when(merchants.execute(anyCollection())).thenReturn(List.of());
        useCase.processCancels(date, List.of());
        verify(repository).saveAll(List.of());
    }

    private List<SettlementTarget> captured() {
        final ArgumentCaptor<List<SettlementTarget>> captor = ArgumentCaptor.captor();
        verify(repository).saveAll(captor.capture());
        return captor.getValue();
    }

    private OrderItem item(final Long id, final Long productId) {
        final var item = mock(OrderItem.class);
        final var order = mock(Order.class);
        when(order.id()).thenReturn(1L);
        when(item.order()).thenReturn(order);
        when(item.id()).thenReturn(id);
        when(item.productId()).thenReturn(productId);
        when(item.quantity()).thenReturn(3);
        when(item.unitPrice()).thenReturn(BigDecimal.valueOf(100));
        when(item.totalPrice()).thenReturn(BigDecimal.valueOf(300));
        return item;
    }

    private Cancel cancel(final Long id, final CancelType type, final Long orderItemId, final long quantity) {
        final var cancel = mock(Cancel.class);
        when(cancel.id()).thenReturn(id);
        when(cancel.type()).thenReturn(type);
        when(cancel.orderId()).thenReturn(1L);
        when(cancel.orderItemId()).thenReturn(orderItemId);
        when(cancel.canceledQuantity()).thenReturn(quantity);
        return cancel;
    }
}
