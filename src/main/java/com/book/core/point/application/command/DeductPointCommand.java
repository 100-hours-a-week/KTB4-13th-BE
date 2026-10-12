package com.book.core.point.application.command;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.point.domain.enums.PointType;
import java.math.BigDecimal;

// @formatter:off
public record DeductPointCommand(Long userId, PointType type, Long referenceId, BigDecimal amount) {
    public DeductPointCommand {
        if (userId == null || userId <= 0 || type == null || referenceId == null || amount == null) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
    }
}
// @formatter:on
