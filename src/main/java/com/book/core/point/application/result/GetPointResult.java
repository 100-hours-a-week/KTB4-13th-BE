package com.book.core.point.application.result;

import java.math.BigDecimal;
import java.util.List;

public record GetPointResult(Long userId, BigDecimal balance, List<PointHistoryResult> histories) {
}
