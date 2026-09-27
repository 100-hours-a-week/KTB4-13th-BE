package com.book.core.product.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.book.domain.Book;
import com.book.core.product.application.command.GetProductsCommand;
import com.book.core.product.application.command.ProductListCursor;
import com.book.core.product.application.command.ProductListSort;
import com.book.core.product.application.port.ProductRepositoryPort;
import com.book.core.product.application.result.ProductListItem;
import com.book.core.product.application.usecase.GetProductsUseCase;
import com.book.core.product.domain.Product;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class GetProductsUseCaseTest {
    private final FakeProductRepository productRepository = new FakeProductRepository();
    private final GetProductsUseCase useCase = new GetProductsUseCase(productRepository);

    @Test
    void 상품_목록을_페이지_크기만큼_변환하고_다음_커서를_반환한다() {
        final var cursor = new ProductListCursor(104L, null, null, null);
        productRepository.products = List.of(item(103L, 0L, 0L), item(102L, 0L, 0L), item(101L, 0L, 0L));

        final var result = useCase.execute(new GetProductsCommand(7L, ProductListSort.CREATED_AT, cursor, 2));

        assertThat(result.items()).extracting(item -> item.itemId()).containsExactly(103L, 102L);
        assertThat(result.items().getFirst().itemName()).isEqualTo("상품 103");
        assertThat(result.items().getFirst().author()).isEqualTo("작가");
        assertThat(result.items().getFirst().orderCount()).isZero();
        assertThat(result.items().getFirst().reviewCount()).isZero();
        assertThat(result.items().getFirst().reviewRate()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.nextCursor()).isEqualTo("102");
        assertThat(productRepository.categoryId).isEqualTo(7L);
        assertThat(productRepository.sort).isEqualTo(ProductListSort.CREATED_AT);
        assertThat(productRepository.cursor).isSameAs(cursor);
        assertThat(productRepository.limit).isEqualTo(3);
    }

    @Test
    void 인기순_다음_커서에_마지막_상품의_전체_정렬_키를_인코딩한다() {
        productRepository.products = List.of(item(103L, 12L, 3L, new BigDecimal("8.5")), item(102L, 10L, 2L, new BigDecimal("9.0")),
            item(101L, 8L, 1L, new BigDecimal("7.0")));

        final var result = useCase.execute(new GetProductsCommand(null, ProductListSort.POPULARITY, null, 2));

        assertThat(result.items()).extracting(item -> item.itemId()).containsExactly(103L, 102L);
        assertThat(result.nextCursor()).isEqualTo(new ProductListCursor(102L, 10L, 2L, new BigDecimal("9.0")).toPopularityToken());
        assertThat(productRepository.sort).isEqualTo(ProductListSort.POPULARITY);
    }

    @Test
    void 상품이_없으면_빈_목록과_널_커서를_반환한다() {
        final var result = useCase.execute(new GetProductsCommand(null, null, null, 20));

        assertThat(result.items()).isEmpty();
        assertThat(result.nextCursor()).isNull();
    }

    @Test
    void 양수가_아닌_필터와_페이지_크기는_요청_오류다() {
        assertThatThrownBy(() -> new GetProductsCommand(0L, null, null, 20)).isInstanceOfSatisfying(CoreException.class,
            exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
        assertThatThrownBy(() -> new GetProductsCommand(null, null, null, 0)).isInstanceOfSatisfying(CoreException.class,
            exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
        assertThatThrownBy(
            () -> new GetProductsCommand(null, ProductListSort.POPULARITY, new ProductListCursor(1L, -1L, 0L, BigDecimal.ZERO), 20))
            .isInstanceOfSatisfying(CoreException.class,
                exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
    }

    private static ProductListItem item(final long id, final long salesQuantity, final long reviewCount) {
        return item(id, salesQuantity, reviewCount, BigDecimal.ZERO);
    }

    private static ProductListItem item(final long id, final long salesQuantity, final long reviewCount, final BigDecimal reviewRate) {
        return new ProductListItem(product(id), salesQuantity, reviewCount, reviewRate);
    }

    private static Product product(final long id) {
        final var book = new Book(10L, null, null, "도서명", "작가", null, "출판사", "소설", LocalDate.of(2026, 1, 1), null);
        return new Product(id, book, "상품 " + id, "thumbnail.jpg", new BigDecimal("20000.00"), new BigDecimal("18000.00"),
            new BigDecimal("12000.00"), 10);
    }

    private static final class FakeProductRepository implements ProductRepositoryPort {
        private List<ProductListItem> products = List.of();
        private Long categoryId;
        private ProductListSort sort;
        private ProductListCursor cursor;
        private int limit;

        @Override
        public Optional<Product> findActiveById(final Long productId) {
            return Optional.empty();
        }

        @Override
        public Optional<Product> findActiveByBookId(final Long bookId) {
            return Optional.empty();
        }

        @Override
        public List<ProductListItem> findActiveProducts(final Long categoryId, final ProductListSort sort, final ProductListCursor cursor,
            final int limit) {
            this.categoryId = categoryId;
            this.sort = sort;
            this.cursor = cursor;
            this.limit = limit;
            return products;
        }
    }
}
