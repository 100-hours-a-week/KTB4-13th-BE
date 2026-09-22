package com.book.core.product.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.book.core.product.application.command.GetProductDetailCommand;
import com.book.core.product.application.command.GetProductsCommand;
import com.book.core.product.application.result.GetProductDetailResult;
import com.book.core.product.application.result.GetProductsResult;
import com.book.core.product.application.usecase.GetProductDetailUseCase;
import com.book.core.product.application.usecase.GetProductsUseCase;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProductServiceTest {
    private final GetProductDetailUseCase useCase = mock(GetProductDetailUseCase.class);
    private final GetProductsUseCase getProductsUseCase = mock(GetProductsUseCase.class);
    private final ProductService productService = new ProductService(useCase, getProductsUseCase);

    @Test
    void 상품_상세_조회를_UseCase에_위임한다() {
        final var command = new GetProductDetailCommand(20L);
        final var result = new GetProductDetailResult(
                20L,
                "상품명",
                null,
                "작가",
                "출판사",
                java.time.LocalDate.of(2026, 1, 1),
                java.math.BigDecimal.TEN,
                java.math.BigDecimal.ONE,
                0L,
                java.math.BigDecimal.ZERO,
                1,
                List.of());
        when(useCase.execute(command)).thenReturn(result);

        assertThat(productService.getProductDetail(command)).isSameAs(result);

        verify(useCase).execute(command);
    }

    @Test
    void 상품_목록_조회를_UseCase에_위임한다() {
        final var command = new GetProductsCommand(null, null, null, 20);
        final var result = GetProductsResult.of(List.of(), null);
        when(getProductsUseCase.execute(command)).thenReturn(result);

        assertThat(productService.getProducts(command)).isSameAs(result);

        verify(getProductsUseCase).execute(command);
    }
}
