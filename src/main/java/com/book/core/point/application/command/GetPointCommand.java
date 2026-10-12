package com.book.core.point.application.command;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;

// @formatter:off
public record GetPointCommand(Long userId) {
    public GetPointCommand {
        if (userId == null || userId <= 0) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
    }
}
// @formatter:on
