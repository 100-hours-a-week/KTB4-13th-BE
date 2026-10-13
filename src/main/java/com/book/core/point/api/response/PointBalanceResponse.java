package com.book.core.point.api.response;

import java.math.BigDecimal;

public record PointBalanceResponse(Long userId, BigDecimal balance) {
}
