package com.book.core.product.application.command;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;

// @formatter:off
public record GetProductsCommand(Long categoryId, ProductListSort sort, ProductListCursor cursor, Integer limit) {
    public static final int DEFAULT_LIMIT = 20;

    public GetProductsCommand {
        if (sort == null) {
            sort = ProductListSort.CREATED_AT;
        }
        if (categoryId != null && categoryId <= 0) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        if (cursor != null) {
            if (cursor.productId() == null || cursor.productId() <= 0) {
                throw new CoreException(ErrorCode.INVALID_REQUEST);
            }
            if (sort == ProductListSort.CREATED_AT && cursor.salesQuantity() != null) {
                throw new CoreException(ErrorCode.INVALID_REQUEST);
            }
            if (sort == ProductListSort.POPULARITY && !cursor.hasPopularityKeys()) {
                throw new CoreException(ErrorCode.INVALID_REQUEST);
            }
        }
        if (limit != null && limit <= 0) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
    }

    public int pageSize() {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        return limit;
    }
}
// @formatter:on
