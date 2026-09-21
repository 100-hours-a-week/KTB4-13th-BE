package com.book.core.product.application.result;

import java.util.List;

public record GetProductsResult(List<GetProductItemResult> items, String nextCursor) {
    public static GetProductsResult of(final List<GetProductItemResult> items, final String nextCursor) {
        return new GetProductsResult(items, nextCursor);
    }
}
