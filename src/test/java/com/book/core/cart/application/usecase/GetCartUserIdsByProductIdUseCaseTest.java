package com.book.core.cart.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.book.core.cart.application.port.CartItemRepositoryPort;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetCartUserIdsByProductIdUseCaseTest {
    @Mock
    CartItemRepositoryPort cartItemRepository;
    @InjectMocks
    GetCartUserIdsByProductIdUseCase useCase;

    @Test
    void 상품을_담은_활성_장바구니_사용자를_조회한다() {
        when(cartItemRepository.findActiveUserIdsByProductId(10L)).thenReturn(List.of(1L, 2L));

        assertThat(useCase.execute(10L)).containsExactly(1L, 2L);
        verify(cartItemRepository).findActiveUserIdsByProductId(10L);
    }
}
