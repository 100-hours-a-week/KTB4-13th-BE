package com.book.core.inventory.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ProductInventoryTest {
    @Test
    void 판매가능재고는_보유수량에서_예약수량을_뺀다() {
        final ProductInventory inventory = new ProductInventory(1L, 20, 3, 3, 20);

        assertThat(inventory.availableQuantity()).isEqualTo(17);
    }

    @Test
    void 신규_상품은_기본_재고로_초기화한다() {
        final ProductInventory inventory = ProductInventory.initial(1L);

        assertThat(inventory.onHandQuantity()).isEqualTo(20);
        assertThat(inventory.reservedQuantity()).isZero();
        assertThat(inventory.reorderPoint()).isEqualTo(3);
        assertThat(inventory.targetStock()).isEqualTo(20);
    }

    @Test
    void 보유재고보다_예약수량이_많을_수_없다() {
        assertThatThrownBy(() -> new ProductInventory(1L, 2, 3, 3, 20))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 목표재고는_발주기준보다_커야_한다() {
        assertThatThrownBy(() -> new ProductInventory(1L, 20, 0, 3, 3))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
