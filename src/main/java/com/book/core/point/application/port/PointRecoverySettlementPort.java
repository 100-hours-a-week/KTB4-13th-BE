package com.book.core.point.application.port;

import com.book.core.point.domain.PointBalance;
import java.math.BigDecimal;

public interface PointRecoverySettlementPort {
    void settleReleasedReservation(PointBalance balance, BigDecimal releasedAmount, Long attemptId);
}
