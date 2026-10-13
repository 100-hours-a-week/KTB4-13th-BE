package com.book.core.product.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.coupon.application.result.CouponResult;
import com.book.core.coupon.application.usecase.GetCouponTargetsUseCase;
import com.book.core.coupon.application.usecase.GetCouponsUseCase;
import com.book.core.coupon.domain.enums.CouponType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

import com.book.core.product.application.command.GetProductDetailCommand;
import com.book.core.product.application.command.GetProductsCommand;
import com.book.core.product.application.command.ProductListCursor;
import com.book.core.product.application.command.ProductListSort;
import com.book.core.product.application.result.GetProductDetailResult;
import com.book.core.product.application.result.GetProductsResult;
import com.book.core.product.application.usecase.GetProductDetailUseCase;
import com.book.core.product.application.usecase.GetProductsUseCase;
import com.book.core.product.application.usecase.RefreshProductPopularitySnapshotUseCase;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProductServiceTest {
    private final GetProductDetailUseCase useCase = mock(GetProductDetailUseCase.class);
    private final GetProductsUseCase getProductsUseCase = mock(GetProductsUseCase.class);
    private final RefreshProductPopularitySnapshotUseCase refreshProductPopularitySnapshotUseCase =
        mock(RefreshProductPopularitySnapshotUseCase.class);
    private final GetCouponTargetsUseCase couponTargetReader = mock(GetCouponTargetsUseCase.class);
    private final GetCouponsUseCase couponReader = mock(GetCouponsUseCase.class);
    private final ProductService productService =
        new ProductService(useCase, getProductsUseCase, refreshProductPopularitySnapshotUseCase, couponTargetReader, couponReader);

    @Test
    void 상품_상세에_상품과_카테고리_쿠폰을_연결하고_기존_필드는_유지한다() {
        final var command = new GetProductDetailCommand(20L);
        final var result = new GetProductDetailResult(20L, "상품명", null, "작가", "출판사", java.time.LocalDate.of(2026, 1, 1), "소설", null,
            java.math.BigDecimal.TEN, java.math.BigDecimal.ONE, 0L, java.math.BigDecimal.ZERO, 1, List.of());
        when(useCase.execute(command)).thenReturn(result);
        when(couponTargetReader.execute(List.of(20L))).thenReturn(Set.of(10L));
        final var expiredAt = LocalDateTime.of(2026, 10, 12, 9, 0);
        when(couponReader.execute(Set.of(10L))).thenReturn(
            List.of(new CouponResult(10L, "상품 쿠폰", CouponType.PERCENT_RATE, new BigDecimal("0.10"), BigDecimal.TEN, expiredAt)));
        final var actual = productService.getProductDetail(command);
        assertThat(actual).usingRecursiveComparison().ignoringFields("coupons").isEqualTo(result);
        assertThat(actual.coupons()).singleElement().satisfies((final var coupon) -> {
            assertThat(coupon.id()).isEqualTo(10L);
            assertThat(coupon.status()).isEqualTo("ACTIVE");
            assertThat(coupon.name()).isEqualTo("상품 쿠폰");
            assertThat(coupon.type()).isEqualTo("PERCENT_RATE");
            assertThat(coupon.discount()).isEqualByComparingTo("0.10");
            assertThat(coupon.minOrderAmount()).isEqualByComparingTo("10");
            assertThat(coupon.expiredAt()).isEqualTo(expiredAt.toString());
            assertThat(coupon.maxDiscountAmount()).isNull();
            assertThat(coupon.maxUseCount()).isNull();
            assertThat(coupon.usedCount()).isNull();
        });
        final var order = inOrder(useCase, couponTargetReader, couponReader);
        order.verify(useCase).execute(command);
        order.verify(couponTargetReader).execute(List.of(20L));
        order.verify(couponReader).execute(Set.of(10L));
    }

    @Test
    void 상품이_없으면_쿠폰을_조회하지_않는다() {
        final var command = new GetProductDetailCommand(20L);
        when(useCase.execute(command)).thenThrow(new CoreException(ErrorCode.PRODUCT_NOT_FOUND));
        assertThatThrownBy(() -> productService.getProductDetail(command)).isInstanceOf(CoreException.class);
        verifyNoInteractions(couponTargetReader, couponReader);
    }

    @Test
    void 상품_목록_조회를_UseCase에_위임한다() {
        final var command = new GetProductsCommand(null, null, null, ProductListSort.POPULARITY, new ProductListCursor(31L, 4L), 20);
        final var result = GetProductsResult.of(List.of(), null);
        when(getProductsUseCase.execute(command)).thenReturn(result);

        assertThat(productService.getProducts(command)).isSameAs(result);

        verify(getProductsUseCase).execute(command);
    }

    @Test
    void 인기_스냅샷_갱신을_UseCase에_위임한다() {
        productService.refreshProductPopularitySnapshot();

        verify(refreshProductPopularitySnapshotUseCase).execute();
    }
}
