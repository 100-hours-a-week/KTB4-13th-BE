package com.book.core.settlement.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.settlement.application.port.SettlementRepositoryPort;
import com.book.core.settlement.domain.Settlement;
import com.book.core.settlement.domain.SettlementState;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class GetSettlementsUseCase {
    private final SettlementRepositoryPort settlementRepository;

    @Transactional(readOnly = true)
    public List<Settlement> byState(final SettlementState state) {
        return settlementRepository.findByState(state);
    }

    @Transactional(readOnly = true)
    public Map<Long, BigDecimal> recentSalesAmounts(final LocalDate settlementDate, final Collection<Long> merchantIds) {
        return settlementRepository.readRecentSalesAmounts(settlementDate.minusMonths(1), settlementDate.minusDays(1), merchantIds);
    }
}
