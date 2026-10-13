package com.book.core.product.application.service;

import com.book.core.coupon.application.usecase.GetCouponTargetsUseCase;
import com.book.core.coupon.application.usecase.GetCouponsUseCase;
import com.book.core.product.application.command.GetProductDetailCommand;
import com.book.core.product.application.command.GetProductsCommand;
import com.book.core.product.application.result.GetProductCouponResult;
import com.book.core.product.application.result.GetProductDetailResult;
import com.book.core.product.application.result.GetProductsResult;
import com.book.core.product.application.usecase.GetProductDetailUseCase;
import com.book.core.product.application.usecase.GetProductsUseCase;
import com.book.core.product.application.usecase.RefreshProductPopularitySnapshotUseCase;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final GetProductDetailUseCase getProductDetailUseCase;
    private final GetProductsUseCase getProductsUseCase;
    private final RefreshProductPopularitySnapshotUseCase refreshProductPopularitySnapshotUseCase;
    private final GetCouponTargetsUseCase couponTargetReader;
    private final GetCouponsUseCase couponReader;

    public GetProductDetailResult getProductDetail(final GetProductDetailCommand command) {
        final var product = getProductDetailUseCase.execute(command);
        final var couponIds = couponTargetReader.execute(List.of(product.productId()));
        final var coupons = couponReader.execute(couponIds).stream().map(GetProductCouponResult::from).toList();
        return product.withCoupons(coupons);
    }

    public GetProductsResult getProducts(final GetProductsCommand command) {
        return getProductsUseCase.execute(command);
    }

    public void refreshProductPopularitySnapshot() {
        refreshProductPopularitySnapshotUseCase.execute();
    }
}
