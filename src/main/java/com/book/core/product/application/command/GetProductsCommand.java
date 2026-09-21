package com.book.core.product.application.command;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;

public record GetProductsCommand(Long categoryId, String sort, Long cursor, Integer limit) {
    public static final int DEFAULT_LIMIT = 20;

    public GetProductsCommand {
        if (categoryId != null && categoryId <= 0) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        if (cursor != null && cursor <= 0) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        if (limit != null && limit <= 0) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
    }

    public int pageSize() {
        return limit == null ? DEFAULT_LIMIT : limit;
    }
}
