package com.book.core.point.api.converter;

import com.book.core.point.api.response.PointBalanceResponse;
import com.book.core.point.api.response.PointHistoryResponse;
import com.book.core.point.api.response.PointResponse;
import com.book.core.point.application.result.GetPointResult;
import org.springframework.stereotype.Component;

@Component
public class PointResultConverter {
    public PointResponse toGetPointResponse(final GetPointResult result) {
        return new PointResponse(new PointBalanceResponse(result.userId(), result.balance()),
            result.histories().stream().map((final var history) -> new PointHistoryResponse(history.id(), history.userId(), history.type(),
                history.referenceId(), history.amount(), history.createdAt())).toList());
    }
}
