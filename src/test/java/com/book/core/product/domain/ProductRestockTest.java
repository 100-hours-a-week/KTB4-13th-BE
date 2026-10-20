package com.book.core.product.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.common.exception.CoreException;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class ProductRestockTest {
    @Test
    void 발주는_대기_상태로_만들고_한_번만_입고한다() {
        final ProductRestock restock = new ProductRestock(1L, 17);
        final LocalDateTime receivedAt = LocalDateTime.of(2026, 10, 21, 9, 0);

        restock.receive(receivedAt);

        assertThat(restock.status()).isEqualTo(ProductRestockStatus.COMPLETED);
        assertThat(restock.receivedAt()).isEqualTo(receivedAt);
        assertThatThrownBy(() -> restock.receive(receivedAt)).isInstanceOf(CoreException.class);
        assertThatThrownBy(() -> new ProductRestock(1L, 0)).isInstanceOf(CoreException.class);
    }
}
