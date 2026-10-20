package com.book.core.settlement.infrastructure.persistence.repository;

import com.book.core.settlement.application.port.SettlementTargetRepositoryPort;
import com.book.core.settlement.domain.SettlementTarget;
import com.book.core.settlement.domain.SettlementTargetSummary;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class SettlementTargetRepositoryAdapter implements SettlementTargetRepositoryPort {
    private final SettlementTargetJpaRepository settlementTargetRepository;

    @Override
    public void saveAll(final List<SettlementTarget> targets) {
        settlementTargetRepository.saveAll(targets);
    }

    @Override
    public List<SettlementTargetSummary> findSummary(final LocalDate settlementDate) {
        return settlementTargetRepository.findSummary(settlementDate);
    }
}
