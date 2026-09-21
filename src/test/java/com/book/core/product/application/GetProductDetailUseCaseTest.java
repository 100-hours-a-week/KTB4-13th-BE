package com.book.core.product.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.book.domain.Book;
import com.book.core.product.application.command.GetProductDetailCommand;
import com.book.core.product.application.port.ProductRepositoryPort;
import com.book.core.product.application.usecase.GetProductDetailUseCase;
import com.book.core.product.domain.Product;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class GetProductDetailUseCaseTest {
    private final FakeProductRepository productRepository = new FakeProductRepository();
    private final GetProductDetailUseCase useCase = new GetProductDetailUseCase(productRepository);

    @Test
    void 활성_상품과_도서의_상세_결과를_반환한다() {
        productRepository.product = product();

        final var result = useCase.execute(new GetProductDetailCommand(20L));

        assertThat(result.productId()).isEqualTo(20L);
        assertThat(result.itemName()).isEqualTo("상품명");
        assertThat(result.author()).isEqualTo("작가");
        assertThat(result.publisher()).isEqualTo("출판사");
        assertThat(result.publishedAt()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(result.salePrice()).isEqualByComparingTo("20000.00");
        assertThat(result.discountedPrice()).isEqualByComparingTo("18000.00");
        assertThat(result.stockQuantity()).isEqualTo(10);
        assertThat(result.reviewCount()).isZero();
        assertThat(result.reviewRate()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.coupons()).isEmpty();
    }

    @Test
    void 상품이_없으면_E404를_던진다() {
        assertThatThrownBy(() -> useCase.execute(new GetProductDetailCommand(20L)))
                .isInstanceOfSatisfying(
                        CoreException.class,
                        exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.PRODUCT_NOT_FOUND));
    }

    @Test
    void 양수가_아닌_상품_ID는_요청_오류다() {
        assertThatThrownBy(() -> new GetProductDetailCommand(0L))
                .isInstanceOfSatisfying(
                        CoreException.class,
                        exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
    }

    private static Product product() {
        final var book = new Book(10L, null, null, "도서명", "작가", null, "출판사", "소설", LocalDate.of(2026, 1, 1), null);
        return new Product(
                20L,
                book,
                "상품명",
                "thumbnail.jpg",
                new BigDecimal("20000.00"),
                new BigDecimal("18000.00"),
                new BigDecimal("12000.00"),
                10);
    }

    private static final class FakeProductRepository implements ProductRepositoryPort {
        private Product product;

        @Override
        public Optional<Product> findActiveById(final Long productId) {
            return Optional.ofNullable(product);
        }
    }
}
