package com.book.common.paging;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;

// @formatter:off
public record OffsetLimit(int offset, int limit) {
    public static final int MAX_LIMIT = 100;

    public OffsetLimit {
        if (offset < 0 || limit <= 0 || limit > MAX_LIMIT) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
    }
}
// @formatter:on
