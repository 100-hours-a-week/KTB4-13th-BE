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

    private static ProductInventory inventory(final int onHandQuantity) {
        final Product product = new Product(1L, new Book(1L, null, null, "제목", "작가", null, null, null, null, null), "상품", null,
            new BigDecimal("20000"), new BigDecimal("18000"), new BigDecimal("12000"), onHandQuantity);
        return (ProductInventory) ReflectionTestUtils.getField(product, "inventory");
    }
}
