package com.book.core.product.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.book.core.book.domain.Book;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ProductTest {
    @Test
    void 상품은_도서와_가격과_재고를_보존한다() {
        final var book = book();
        final var product = new Product(
                20L,
                book,
                "상품명",
                "thumbnail.jpg",
                new BigDecimal("20000.00"),
                new BigDecimal("18000.00"),
                new BigDecimal("12000.00"),
                10);

        assertThat(product.book()).isSameAs(book);
        assertThat(product.salePrice()).isEqualByComparingTo("20000.00");
        assertThat(product.discountedPrice()).isEqualByComparingTo("18000.00");
        assertThat(product.stockQuantity()).isEqualTo(10);
        assertThat(product.isActive()).isTrue();
    }

    @Test
    void 삭제된_상품은_활성_상품이_아니다() {
        final var product = new Product(
                20L,
                book(),
                "상품명",
                null,
                new BigDecimal("20000.00"),
                new BigDecimal("18000.00"),
                new BigDecimal("12000.00"),
                10);

        product.delete();

        assertThat(product.isActive()).isFalse();
    }

    private static Book book() {
        return new Book(10L, null, null, "도서명", "작가", null, "출판사", "소설", LocalDate.of(2026, 1, 1), null);
    }
}
