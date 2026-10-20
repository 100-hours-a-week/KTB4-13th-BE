package com.book.core.settlement.application.port;

import com.book.core.settlement.domain.Settlement;
import com.book.core.settlement.domain.SettlementState;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface SettlementRepositoryPort {
    void saveAll(final List<Settlement> settlements);

    List<Settlement> findByState(final SettlementState state);

    List<Settlement> findAllById(final Collection<Long> ids);

    Map<Long, BigDecimal> readRecentSalesAmounts(final LocalDate startDate, final LocalDate endDate, final Collection<Long> merchantIds);
}
