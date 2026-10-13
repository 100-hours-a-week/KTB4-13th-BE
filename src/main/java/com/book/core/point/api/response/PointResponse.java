package com.book.core.point.api.response;

import java.util.List;

public record PointResponse(PointBalanceResponse balance, List<PointHistoryResponse> histories) {
}
