package com.book.core.product.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.book.core.book.domain.Book;
import com.book.core.product.application.port.ProductRepositoryPort;
import com.book.core.product.application.result.ProductOfferResult;
import com.book.core.product.domain.Product;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class GetProductOffersUseCaseTest {
    private final ProductRepositoryPort repository = mock(ProductRepositoryPort.class);
    private final GetProductOffersUseCase useCase = new GetProductOffersUseCase(repository);

    @Test
    void 빈_입력은_조회하지_않는다() {
        assertThat(useCase.execute(List.of())).isEmpty();
        verifyNoInteractions(repository);
    }

    @Test
    void 입력_순서와_재고를_유지하며_중복과_없는_상품은_생략한다() {
        final Product first = product(1L);
        final Product second = product(2L);
        when(repository.findByIds(List.of(2L, 99L, 1L, 2L))).thenReturn(List.of(first, second));
        final List<ProductOfferResult> result = useCase.execute(List.of(2L, 99L, 1L, 2L));
        assertThat(result).extracting(ProductOfferResult::productId).containsExactly(2L, 1L);
        assertThat(result.getFirst().price()).isEqualByComparingTo("12000");
        assertThat(result.getFirst().stockQuantity()).isEqualTo(3);
        verify(repository).findByIds(List.of(2L, 99L, 1L, 2L));
    }

    @Test
    void 판매_중지_상품과_삭제_책은_가격을_보내지_않는다() {
        final Product removed = product(1L);
        removed.delete();
        final Product deletedBook = product(2L);
        deletedBook.book().delete();
        when(repository.findByIds(List.of(1L, 2L))).thenReturn(List.of(removed, deletedBook));
        assertThat(useCase.execute(List.of(1L, 2L))).allSatisfy((final ProductOfferResult offer) -> {
            assertThat(offer.active()).isFalse();
            assertThat(offer.price()).isNull();
            assertThat(offer.isbn()).isNotNull();
        });
    }

    private Product product(final Long id) {
        final Book book = new Book(id, "9788900000011", null, "책", "작가", null, "출판사", "소설", LocalDate.of(2026, 1, 1), null);
        return new Product(id, book, "상품", null, new BigDecimal("15000"), new BigDecimal("12000"), new BigDecimal("9000"), 3);
    }
}
