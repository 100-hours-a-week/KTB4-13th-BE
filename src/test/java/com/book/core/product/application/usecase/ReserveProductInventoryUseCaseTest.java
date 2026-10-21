package com.book.core.product.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.product.application.command.ProductQuantitiesCommand;
import com.book.core.product.application.port.ProductInventoryRepositoryPort;
import java.util.LinkedHashMap;
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
    @InjectMocks
    ReserveProductInventoryUseCase useCase;

    @Test
    void 예약으로_품절_임박에_진입한_상품의_남은_수량만_반환한다() {
        final Map<Long, Integer> quantities = new LinkedHashMap<>();
        quantities.put(10L, 2);
        quantities.put(11L, 1);
        when(inventoryRepository.reserve(10L, 2)).thenReturn(true);
        when(inventoryRepository.availableQuantity(10L)).thenReturn(3);
        when(inventoryRepository.reserve(11L, 1)).thenReturn(true);
        when(inventoryRepository.availableQuantity(11L)).thenReturn(2);

        assertThat(useCase.execute(new ProductQuantitiesCommand(quantities))).containsExactly(Map.entry(10L, 3));
    }

    @Test
    void 예약에_실패하면_재고_부족_예외를_던진다() {
        when(inventoryRepository.reserve(10L, 6)).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(new ProductQuantitiesCommand(Map.of(10L, 6)))).isInstanceOfSatisfying(CoreException.class,
            (final CoreException exception) -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.INSUFFICIENT_PRODUCT_STOCK));
    }
}
