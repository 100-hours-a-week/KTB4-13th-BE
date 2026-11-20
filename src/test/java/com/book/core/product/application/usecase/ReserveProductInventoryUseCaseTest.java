package com.book.core.product.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.product.application.command.ProductQuantitiesCommand;
import com.book.core.product.application.port.ProductInventoryRepositoryPort;
import com.book.core.product.application.result.ProductInventoryState;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReserveProductInventoryUseCaseTest {
    @Mock
    ProductInventoryRepositoryPort inventoryRepository;
    private ReserveProductInventoryUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ReserveProductInventoryUseCase(inventoryRepository, Clock.systemUTC());
    }

    @Test
    void 예약으로_품절_임박에_진입한_상품의_남은_수량만_반환한다() {
        final Map<Long, Integer> quantities = new LinkedHashMap<>();
        quantities.put(10L, 2);
        quantities.put(11L, 1);
        when(inventoryRepository.reserve(10L, 2)).thenReturn(true);
        when(inventoryRepository.state(10L)).thenReturn(new ProductInventoryState(3, 0));
        when(inventoryRepository.reserve(11L, 1)).thenReturn(true);
        when(inventoryRepository.state(11L)).thenReturn(new ProductInventoryState(2, 0));

        assertThat(useCase.execute(new ProductQuantitiesCommand(quantities))).containsExactly(Map.entry(10L, 3));
    }

    @Test
    void 예약에_실패하면_재고_부족_예외를_던진다() {
        when(inventoryRepository.reserve(10L, 6)).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(new ProductQuantitiesCommand(Map.of(10L, 6)))).isInstanceOfSatisfying(CoreException.class,
            (final CoreException exception) -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.INSUFFICIENT_PRODUCT_STOCK));
    }

    @Test
    void V3_임계치로_예약하면_5개_이하에_새로_진입한_상품과_버전을_반환한다() {
        when(inventoryRepository.reserve(10L, 1)).thenReturn(true);
        when(inventoryRepository.state(10L)).thenReturn(new ProductInventoryState(5, 2));
        when(inventoryRepository.reserve(11L, 1)).thenReturn(true);
        when(inventoryRepository.state(11L)).thenReturn(new ProductInventoryState(4, 2));
        when(inventoryRepository.reserve(12L, 3)).thenReturn(true);
        when(inventoryRepository.state(12L)).thenReturn(new ProductInventoryState(3, 2));

        final var entries = useCase.executeWithLowStockNotificationEntries(new ProductQuantitiesCommand(Map.of(10L, 1, 11L, 1, 12L, 3)), 5);
        assertThat(entries.keySet()).containsExactly(10L, 12L);
        assertThat(entries.get(10L).state()).isEqualTo(new ProductInventoryState(5, 2));
        assertThat(entries.get(12L).state()).isEqualTo(new ProductInventoryState(3, 2));
    }

    @Test
    void V3_예약에_실패하면_재고_부족_예외를_던진다() {
        when(inventoryRepository.reserve(10L, 7)).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(new ProductQuantitiesCommand(Map.of(10L, 7)), 5)).isInstanceOfSatisfying(
            CoreException.class,
            (final CoreException exception) -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.INSUFFICIENT_PRODUCT_STOCK));
    }
}
