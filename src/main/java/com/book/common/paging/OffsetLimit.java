package com.book.common.paging;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;

// @formatter:off
public record OffsetLimit(int offset, int limit) {
    public OffsetLimit {
        if (offset < 0 || limit <= 0) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
    }
}
// @formatter:on
