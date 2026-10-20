package com.book.core.settlement.infrastructure.persistence.repository;

import com.book.core.settlement.application.port.SettlementRepositoryPort;
import com.book.core.settlement.domain.Settlement;
import com.book.core.settlement.domain.SettlementState;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class SettlementRepositoryAdapter implements SettlementRepositoryPort {
    private final SettlementJpaRepository settlementRepository;

    @Override
    public void saveAll(final List<Settlement> settlements) {
        settlementRepository.saveAll(settlements);
    }

    @Override
    public List<Settlement> findByState(final SettlementState state) {
        return settlementRepository.findByState(state);
    }

    @Override
    public List<Settlement> findAllById(final Collection<Long> ids) {
        return settlementRepository.findAllById(ids);
    }

    @Override
    public Map<Long, BigDecimal> readRecentSalesAmounts(final LocalDate startDate, final LocalDate endDate,
        final Collection<Long> merchantIds) {
        return settlementRepository.sumOriginalAmount(merchantIds, startDate, endDate).stream()
            .collect(Collectors.toMap(MerchantAmountProjection::getMerchantId, MerchantAmountProjection::getAmount));
    }
}
