package com.book.core.cart.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.cart.application.command.ApplyCartChangesCommand;
import com.book.core.cart.application.command.CartItemChange;
import com.book.core.cart.application.port.CartItemRepositoryPort;
import com.book.core.cart.application.port.CartRepositoryPort;
import com.book.core.cart.application.result.CartBatchChangeResult;
import com.book.core.cart.application.usecase.ApplyCartChangesUseCase;
import com.book.core.cart.domain.Cart;
import com.book.core.cart.domain.CartItem;
import com.book.core.product.application.result.CartChangeProductResult;
import com.book.core.product.application.usecase.GetProductsForCartChangeUseCase;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.LongStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ApplyCartChangesUseCaseTest {
    private static final Instant NOW = Instant.parse("2026-10-19T00:00:00Z");
    private static final BigDecimal PRICE = BigDecimal.valueOf(10000);
    private final CartRepositoryPort carts = mock(CartRepositoryPort.class);
    private final CartItemRepositoryPort items = mock(CartItemRepositoryPort.class);
    private final GetProductsForCartChangeUseCase products = mock(GetProductsForCartChangeUseCase.class);
    private final ApplyCartChangesUseCase useCase = new ApplyCartChangesUseCase(carts, items, products, Clock.fixed(NOW, ZoneOffset.UTC));

    @BeforeEach
    void prepare() {
        when(carts.findByUserIdForChange(7L)).thenReturn(Optional.of(new Cart(1L, 7L)));
        when(items.findAllByCartIdForChange(1L)).thenReturn(List.of());
    }

    @Test
    void 새_상품과_기존_상품과_삭제된_상품을_최종_수량으로_한번에_반영한다() {
        final CartItem existing = CartItem.from(1L, 20L, 3);
        final CartItem deleted = CartItem.from(1L, 30L, 4);
        deleted.delete();
        when(items.findAllByCartIdForChange(1L)).thenReturn(List.of(existing, deleted));
        when(products.execute(List.of(10L, 20L, 30L)))
            .thenReturn(Map.of(10L, product(10L, 10, true), 20L, product(20L, 10, true), 30L, product(30L, 10, true)));

        final CartBatchChangeResult result = execute(List.of(set(10L, 0), set(20L, 3), set(30L, 0)));

        assertThat(result).isEqualTo(new CartBatchChangeResult(NOW, null, false));
        assertThat(existing.quantity()).isEqualTo(2);
        assertThat(deleted.quantity()).isEqualTo(2);
        assertThat(deleted.isActive()).isTrue();
        verify(items).save(argThat((final CartItem item) -> item.productId().equals(10L) && item.quantity() == 2));
        verify(products).execute(List.of(10L, 20L, 30L));
    }

    @Test
    void 삭제만_요청하면_상품을_조회하지_않는다() {
        final CartItem existing = CartItem.from(1L, 20L, 3);
        when(items.findAllByCartIdForChange(1L)).thenReturn(List.of(existing));

        assertThat(execute(List.of(new CartItemChange(20L, 3, null, null))).appliedAt()).isEqualTo(NOW);

        assertThat(existing.isActive()).isFalse();
        verify(products, never()).execute(any());
    }

    @Test
    void 나중_상품의_수량이_달라졌으면_앞_상품도_변경하지_않는다() {
        final CartItem first = CartItem.from(1L, 10L, 1);
        final CartItem second = CartItem.from(1L, 20L, 4);
        when(items.findAllByCartIdForChange(1L)).thenReturn(List.of(first, second));
        when(products.execute(List.of(10L, 20L))).thenReturn(Map.of(10L, product(10L, 10, true), 20L, product(20L, 10, true)));

        assertThat(execute(List.of(set(10L, 1), set(20L, 3))).rejection()).isEqualTo(ErrorCode.CART_STATE_CHANGED);

        assertThat(first.quantity()).isEqualTo(1);
        assertThat(second.quantity()).isEqualTo(4);
        verify(items, never()).save(any());
    }

    @Test
    void 가격이_달라지면_저장하지_않는다() {
        when(products.execute(List.of(10L))).thenReturn(Map.of(10L, new CartChangeProductResult(10L, PRICE.add(BigDecimal.ONE), 10, true)));
        assertThat(execute(List.of(set(10L, 0))).rejection()).isEqualTo(ErrorCode.CART_PRICE_CHANGED);
        verify(items, never()).save(any());
    }

    @Test
    void 재고가_부족하면_저장하지_않는다() {
        when(products.execute(List.of(10L))).thenReturn(Map.of(10L, product(10L, 1, true)));
        assertThat(execute(List.of(set(10L, 0))).rejection()).isEqualTo(ErrorCode.INSUFFICIENT_PRODUCT_STOCK);
        verify(items, never()).save(any());
    }

    @Test
    void 비활성_상품이면_저장하지_않는다() {
        when(products.execute(List.of(10L))).thenReturn(Map.of(10L, product(10L, 10, false)));
        assertThat(execute(List.of(set(10L, 0))).rejection()).isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);
        verify(items, never()).save(any());
    }

    @Test
    void 누락된_상품이면_저장하지_않는다() {
        when(products.execute(List.of(10L))).thenReturn(Map.of());
        assertThat(execute(List.of(set(10L, 0))).rejection()).isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);
    }

    @Test
    void 삼십종을_넘기는_추가는_전체_거절한다() {
        final List<CartItem> existing = LongStream.rangeClosed(100, 129).mapToObj((final long id) -> CartItem.from(1L, id, 1)).toList();
        when(items.findAllByCartIdForChange(1L)).thenReturn(existing);
        when(products.execute(List.of(10L))).thenReturn(Map.of(10L, product(10L, 10, true)));
        assertThat(execute(List.of(set(10L, 0))).rejection()).isEqualTo(ErrorCode.CART_ITEM_LIMIT_EXCEEDED);
        verify(items, never()).save(any());
    }

    @Test
    void 유효_기한과_같은_시각이면_적용하지_않는다() {
        when(products.execute(List.of(10L))).thenReturn(Map.of(10L, product(10L, 10, true)));
        assertThat(useCase.execute(new ApplyCartChangesCommand(7L, List.of(set(10L, 0)), NOW)).expired()).isTrue();
        verify(items, never()).save(any());
    }

    @Test
    void 다른_사용자의_장바구니로_대체하지_않는다() {
        when(carts.findByUserIdForChange(7L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> execute(List.of(set(10L, 0)))).isInstanceOfSatisfying(CoreException.class,
            (final CoreException exception) -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.CART_NOT_FOUND));
        verify(items, never()).findAllByCartIdForChange(any());
    }

    private CartBatchChangeResult execute(final List<CartItemChange> changes) {
        return useCase.execute(new ApplyCartChangesCommand(7L, changes, NOW.plusSeconds(10)));
    }

    private CartItemChange set(final long id, final int expectedQuantity) {
        return new CartItemChange(id, expectedQuantity, 2, PRICE);
    }

    private CartChangeProductResult product(final long id, final int stock, final boolean active) {
        return new CartChangeProductResult(id, PRICE, stock, active);
    }
}
