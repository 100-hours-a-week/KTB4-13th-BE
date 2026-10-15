package com.book.core.cart.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.book.core.cart.application.port.CartRepositoryPort;
import com.book.core.cart.application.usecase.RecoverCartUseCase;
import com.book.core.cart.domain.Cart;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class RecoverCartUseCaseTest {
    private final CartRepositoryPort repository = mock(CartRepositoryPort.class);

    @Test
    void 장바구니가_없으면_생성한다() {
        when(repository.existsByUserId(42L)).thenReturn(false);

        final boolean recovered = new RecoverCartUseCase(repository).execute(42L);

        final ArgumentCaptor<Cart> captor = ArgumentCaptor.forClass(Cart.class);
        verify(repository).save(captor.capture());
        assertThat(recovered).isTrue();
        assertThat(captor.getValue().userId()).isEqualTo(42L);
    }

    @Test
    void 장바구니가_이미_있으면_생성하지_않는다() {
        when(repository.existsByUserId(42L)).thenReturn(true);

        final boolean recovered = new RecoverCartUseCase(repository).execute(42L);

        assertThat(recovered).isFalse();
        verify(repository, never()).save(any(Cart.class));
    }
}
