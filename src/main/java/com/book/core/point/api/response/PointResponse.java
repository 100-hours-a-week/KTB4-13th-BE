package com.book.core.point.api.response;

import java.util.List;

// @formatter:off
public record PointResponse(PointBalanceResponse balance, List<PointHistoryResponse> histories) {}
// @formatter:on
