package com.book.core.product.application.service;

import com.book.core.coupon.application.usecase.GetCouponTargetsUseCase;
import com.book.core.coupon.application.usecase.GetCouponsUseCase;
import com.book.core.product.application.command.GetProductDetailCommand;
import com.book.core.product.application.command.GetProductsCommand;
import com.book.core.coupon.application.result.CouponResult;
import com.book.core.product.application.result.GetProductDetailResult;
import com.book.core.product.application.result.GetProductsResult;
import com.book.core.product.application.usecase.CreateProductRestockUseCase;
import com.book.core.product.application.usecase.GetProductDetailUseCase;
import com.book.core.product.application.usecase.GetProductsUseCase;
import com.book.core.product.application.usecase.GetReceivableRestocksUseCase;
import com.book.core.product.application.usecase.GetRestockCandidatesUseCase;
import com.book.core.product.application.usecase.ReceiveProductRestockUseCase;
import com.book.core.product.application.usecase.RecordProductViewUseCase;
import com.book.core.product.application.usecase.RefreshProductPopularitySnapshotUseCase;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {
    private final GetProductDetailUseCase getProductDetailUseCase;
    private final GetProductsUseCase getProductsUseCase;
    private final RefreshProductPopularitySnapshotUseCase refreshProductPopularitySnapshotUseCase;
    private final GetCouponTargetsUseCase couponTargetReader;
    private final GetCouponsUseCase couponReader;
    private final GetRestockCandidatesUseCase getRestockCandidatesUseCase;
    private final CreateProductRestockUseCase createProductRestockUseCase;
    private final GetReceivableRestocksUseCase getReceivableRestocksUseCase;
    private final ReceiveProductRestockUseCase receiveProductRestockUseCase;
    private final RecordProductViewUseCase recordProductViewUseCase;
    private final Clock clock;

    public GetProductDetailResult getProductDetail(final GetProductDetailCommand command) {
        final var product = getProductDetailUseCase.execute(command);
        final var couponIds = couponTargetReader.execute(List.of(product.productId()));
        final var activeCouponIds = couponReader.execute(couponIds).stream().map(CouponResult::id).toList();
        final GetProductDetailResult result = product.withCouponIds(activeCouponIds);
        if (command.userId() != null) {
            try {
                recordProductViewUseCase.execute(command.userId(), product.productId());
            } catch (final RuntimeException exception) {
                log.atError().addKeyValue("productId", product.productId()).addKeyValue("userId", command.userId())
                    .log("[PRODUCT] 상품 조회 기록 저장에 실패했습니다.", exception);
            }
        }
        return result;
    }

    public GetProductsResult getProducts(final GetProductsCommand command) {
        return getProductsUseCase.execute(command);
    }

    public void refreshProductPopularitySnapshot() {
        refreshProductPopularitySnapshotUseCase.execute();
    }

    public List<Long> getRestockCandidates() {
        return getRestockCandidatesUseCase.execute();
    }

    public boolean createRestock(final Long productId) {
        return createProductRestockUseCase.execute(productId);
    }

    public List<Long> getReceivableRestocks() {
        return getReceivableRestocksUseCase.execute(LocalDateTime.now(clock));
    }

    public boolean receiveRestock(final Long restockId) {
        return receiveProductRestockUseCase.execute(restockId, LocalDateTime.now(clock));
    }
}
