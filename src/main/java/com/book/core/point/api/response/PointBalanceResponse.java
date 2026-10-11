package com.book.core.point.api.response;

import java.math.BigDecimal;

// @formatter:off
public record PointBalanceResponse(Long userId, BigDecimal balance) {}
// @formatter:on
