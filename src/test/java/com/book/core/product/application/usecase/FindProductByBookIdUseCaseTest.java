package com.book.core.product.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import com.book.core.book.domain.Book;
import com.book.core.product.application.command.ProductListCursor;
import com.book.core.product.application.command.ProductListSort;
import com.book.core.product.application.port.ProductRepositoryPort;
import com.book.core.product.application.result.ProductListItem;
import com.book.core.product.domain.Product;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class FindProductByBookIdUseCaseTest {
    private final FakeProductRepository productRepository = new FakeProductRepository();
    private final FindProductByBookIdUseCase useCase = new FindProductByBookIdUseCase(productRepository);

    @Test
    void bookId로_활성_Product를_찾으면_반환한다() {
        productRepository.products.put(10L, product(10L));

        final Optional<Product> found = useCase.execute(10L);

        assertThat(found).isPresent();
        assertThat(found.get().book().id()).isEqualTo(10L);
    }

    @Test
    void Product가_없으면_빈_Optional을_반환한다() {
        final Optional<Product> found = useCase.execute(999L);

        assertThat(found).isEmpty();
    }

    private Product product(final long bookId) {
        final Book book = new Book(bookId, null, null, "제목", "작가", null, "출판사", "소설", LocalDate.of(2026, 1, 1), null);
        return new Product(bookId, book, "상품명", null, new BigDecimal("20000.00"), new BigDecimal("18000.00"), new BigDecimal("12000.00"),
            10);
    }

    private static class FakeProductRepository implements ProductRepositoryPort {
        final Map<Long, Product> products = new HashMap<>();

        @Override
        public List<Product> findByIds(final List<Long> productIds) {
            return List.of();
        }

        @Override
        public Optional<Product> findActiveById(final Long productId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<Product> findActiveByBookId(final Long bookId) {
            return Optional.ofNullable(products.get(bookId));
        }

        @Override
        public List<Product> findActiveByBookIds(final List<Long> bookIds) {
            return List.of();
        }

        @Override
        public List<ProductListItem> findActiveProducts(final Long categoryId, final LocalDate publishedFrom, final LocalDate publishedTo,
            final ProductListSort sort, final ProductListCursor cursor, final int limit) {
            throw new UnsupportedOperationException();
        }
    }
}
