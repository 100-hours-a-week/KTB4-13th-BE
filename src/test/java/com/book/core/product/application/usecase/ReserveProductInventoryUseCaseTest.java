package com.book.core.product.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.cart.application.usecase.GetCartUserIdsByProductIdUseCase;
import com.book.core.product.application.command.ProductQuantitiesCommand;
import com.book.core.product.application.port.ProductInventoryRepositoryPort;
import com.book.core.product.application.result.ProductInventoryReservationResult;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReserveProductInventoryUseCaseTest {
    @Mock
    ProductInventoryRepositoryPort inventoryRepository;
    @Mock
    GetCartUserIdsByProductIdUseCase getCartUserIdsByProductIdUseCase;
    @InjectMocks
    ReserveProductInventoryUseCase useCase;

    @Test
    void 일반_예약은_부족_진입_결과를_만들지_않는다() {
        when(inventoryRepository.reserve(10L, 2)).thenReturn(ProductInventoryReservationResult.success());

        assertThat(useCase.execute(command(10L, 2))).isEmpty();
        verifyNoInteractions(getCartUserIdsByProductIdUseCase);
    }

    @Test
    void 부족_진입은_당시_장바구니_사용자를_결과에_포함한다() {
        when(inventoryRepository.reserve(10L, 3)).thenReturn(ProductInventoryReservationResult.lowStock(2, 4));
        when(getCartUserIdsByProductIdUseCase.execute(10L)).thenReturn(List.of(1L, 2L));

        assertThat(useCase.execute(command(10L, 3))).singleElement().satisfies(result -> {
            assertThat(result.productId()).isEqualTo(10L);
            assertThat(result.availableQuantity()).isEqualTo(2);
            assertThat(result.lowStockCycle()).isEqualTo(4);
            assertThat(result.userIds()).containsExactly(1L, 2L);
        });
    }

    @Test
    void 예약_실패는_재고_부족으로_처리하고_수신자를_조회하지_않는다() {
        when(inventoryRepository.reserve(10L, 6)).thenReturn(ProductInventoryReservationResult.insufficient());

        assertThatThrownBy(() -> useCase.execute(command(10L, 6))).isInstanceOfSatisfying(CoreException.class,
            exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.INSUFFICIENT_PRODUCT_STOCK));
        verifyNoInteractions(getCartUserIdsByProductIdUseCase);
    }

    private static ProductQuantitiesCommand command(final long productId, final int quantity) {
        return new ProductQuantitiesCommand(Map.of(productId, quantity));
    }
}
