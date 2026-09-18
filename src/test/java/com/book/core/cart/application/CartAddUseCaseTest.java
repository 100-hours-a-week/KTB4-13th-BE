package com.book.core.cart.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.common.exception.CoreException;
import com.book.core.cart.application.command.CartAddCommand;
import com.book.core.cart.application.usecase.CartAddUseCase;
import org.junit.jupiter.api.Test;

class CartAddUseCaseTest {
    private final InMemoryCartRepository repository = new InMemoryCartRepository();
    private final CartAddUseCase useCase = new CartAddUseCase(repository);

    @Test
    void 같은_상품_추가_요청은_수량을_대체한다() {
        useCase.execute(new CartAddCommand(1L, 20L, 2));
        useCase.execute(new CartAddCommand(1L, 20L, 4));

        assertThat(repository.cart(1L).items())
                .extracting(item -> item.quantity())
                .containsExactly(4);
    }

    @Test
    void 첫_추가_요청은_장바구니를_생성하고_상품을_저장한다() {
        useCase.execute(new CartAddCommand(1L, 20L, 2));

        assertThat(repository.cart(1L).items()).singleElement().satisfies(item -> {
            assertThat(item.productId()).isEqualTo(20L);
            assertThat(item.quantity()).isEqualTo(2);
        });
    }

    @Test
    void Command는_양수_ID와_유효한_수량을_요구한다() {
        assertThatThrownBy(() -> new CartAddCommand(0L, 20L, 1)).isInstanceOf(CoreException.class);
        assertThatThrownBy(() -> new CartAddCommand(1L, 0L, 1)).isInstanceOf(CoreException.class);
        assertThatThrownBy(() -> new CartAddCommand(1L, 20L, 0)).isInstanceOf(CoreException.class);
        assertThatThrownBy(() -> new CartAddCommand(1L, 20L, 501)).isInstanceOf(CoreException.class);
    }
}
