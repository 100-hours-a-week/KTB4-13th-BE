package com.book.core.product.api.response;

import com.book.core.product.application.result.GetProductsResult;
import java.util.List;

public record ProductListResponse(List<ProductItemResponse> items, String nextCursor) {
    public static ProductListResponse from(final GetProductsResult result) {
        final var items = result.items().stream().map(ProductItemResponse::from).toList();
        return new ProductListResponse(items, result.nextCursor());
    }
}
