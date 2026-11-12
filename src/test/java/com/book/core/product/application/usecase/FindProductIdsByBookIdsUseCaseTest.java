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

class FindProductIdsByBookIdsUseCaseTest {
    private final FakeProductRepository productRepository = new FakeProductRepository();
    private final FindProductIdsByBookIdsUseCase useCase = new FindProductIdsByBookIdsUseCase(productRepository);

    @Test
    void 활성_상품이_있는_bookId마다_해당_상품의_productId를_반환한다() {
        productRepository.productsByBookId.put(10L, product(501L, 10L));
        productRepository.productsByBookId.put(20L, product(502L, 20L));

        final Map<Long, Long> productIdsByBookId = useCase.execute(List.of(10L, 20L));

        assertThat(productIdsByBookId).containsExactlyInAnyOrderEntriesOf(Map.of(10L, 501L, 20L, 502L));
        assertThat(productRepository.requestedBookIds).isEqualTo(List.of(10L, 20L));
    }

    @Test
    void 활성_상품이_없는_bookId는_결과에_포함하지_않는다() {
        productRepository.productsByBookId.put(10L, product(501L, 10L));

        final Map<Long, Long> productIdsByBookId = useCase.execute(List.of(10L, 999L));

        assertThat(productIdsByBookId).containsExactlyInAnyOrderEntriesOf(Map.of(10L, 501L));
    }

    @Test
    void bookId_목록이_비어_있으면_저장소를_조회하지_않고_빈_결과를_반환한다() {
        final Map<Long, Long> productIdsByBookId = useCase.execute(List.of());

        assertThat(productIdsByBookId).isEmpty();
        assertThat(productRepository.requestedBookIds).isNull();
    }

    private Product product(final long productId, final long bookId) {
        final Book book = new Book(bookId, null, null, "제목", "작가", null, "출판사", "소설", LocalDate.of(2026, 1, 1), null);
        return new Product(productId, book, "상품명", null, new BigDecimal("20000.00"), new BigDecimal("18000.00"), new BigDecimal("12000.00"),
            10);
    }

    private static class FakeProductRepository implements ProductRepositoryPort {
        final Map<Long, Product> productsByBookId = new HashMap<>();
        List<Long> requestedBookIds;

        @Override
        public List<Product> findByIds(final List<Long> productIds) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<Product> findActiveById(final Long productId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<Product> findActiveByBookId(final Long bookId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Product> findActiveByBookIds(final List<Long> bookIds) {
            requestedBookIds = bookIds;
            return bookIds.stream().map(productsByBookId::get).filter((final var product) -> product != null).toList();
        }

        @Override
        public List<ProductListItem> findActiveProducts(final Long categoryId, final LocalDate publishedFrom, final LocalDate publishedTo,
            final ProductListSort sort, final ProductListCursor cursor, final int limit) {
            throw new UnsupportedOperationException();
        }
    }
}
