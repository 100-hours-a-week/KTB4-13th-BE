package com.book.core.cart.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.book.core.cart.application.command.CartAddCommand;
import com.book.core.cart.application.usecase.CartAddUseCase;
import org.junit.jupiter.api.Test;

class CartUseCaseTest {
    private final MemoryCartRepository repository = new MemoryCartRepository();

    @Test
    void 같은_상품을_반복_추가하면_요청한_수량으로_대체한다() {
        final var add = new CartAddUseCase(repository);
        final var request = new CartAddCommand(1L, 20L, 2);
        add.execute(request);
        add.execute(request);
        assertThat(repository.findByUserId(1L).orElseThrow().items().getFirst().quantity())
                .isEqualTo(2);
    }
}
