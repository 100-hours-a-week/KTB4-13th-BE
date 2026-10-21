package com.book.core.product.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.book.core.book.domain.Book;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class ProductInventoryTest {
    @Test
    void 판매_가능_수량은_보유_재고에서_예약_재고를_뺀_값이다() {
        final ProductInventory inventory = inventory(20);
        ReflectionTestUtils.setField(inventory, "reservedQuantity", 3);

        assertThat(inventory.availableQuantity()).isEqualTo(17);
        assertThat(inventory.reorderPoint()).isEqualTo(3);
        assertThat(inventory.targetStock()).isEqualTo(20);
    }

    @Test
    void 보유_재고가_발주_기준_이하이면_목표_재고까지_발주한다() {
        assertThat(inventory(4).needsRestock()).isFalse();
        assertThat(inventory(3).needsRestock()).isTrue();
        assertThat(inventory(3).restockQuantity()).isEqualTo(17);
    }

    @Test
    void 판매_가능_재고가_3개_초과에서_3개_이하가_될_때만_품절_임박에_진입한다() {
        assertThat(ProductInventory.entersLowStock(5, 3)).isTrue();
        assertThat(ProductInventory.entersLowStock(4, 2)).isTrue();
        assertThat(ProductInventory.entersLowStock(10, 3)).isTrue();
        assertThat(ProductInventory.entersLowStock(3, 2)).isFalse();
        assertThat(ProductInventory.entersLowStock(2, 1)).isFalse();
        assertThat(ProductInventory.entersLowStock(5, 0)).isTrue();
        assertThat(ProductInventory.entersLowStock(3, 0)).isFalse();
        assertThat(ProductInventory.entersLowStock(5, 4)).isFalse();
    }

    private static ProductInventory inventory(final int onHandQuantity) {
        final Product product = new Product(1L, new Book(1L, null, null, "제목", "작가", null, null, null, null, null), "상품", null,
            new BigDecimal("20000"), new BigDecimal("18000"), new BigDecimal("12000"), onHandQuantity);
        return (ProductInventory) ReflectionTestUtils.getField(product, "inventory");
    }
}
