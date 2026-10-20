package com.book.core.point.application.command;

import com.book.core.point.domain.enums.PointType;
import java.math.BigDecimal;

public record EarnPointCommand(Long userId, PointType type, Long referenceId, BigDecimal amount) {
}
