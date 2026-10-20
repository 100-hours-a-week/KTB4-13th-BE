package com.book.core.point.application.command;

import com.book.core.point.domain.enums.PointType;
import java.math.BigDecimal;

public record DeductPointCommand(Long userId, PointType type, Long referenceId, BigDecimal amount) {
}
