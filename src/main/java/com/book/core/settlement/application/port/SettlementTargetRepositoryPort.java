package com.book.core.settlement.application.port;

import com.book.core.settlement.domain.SettlementTarget;
import com.book.core.settlement.domain.SettlementTargetSummary;
import java.time.LocalDate;
import java.util.List;

public interface SettlementTargetRepositoryPort {
    void saveAll(final List<SettlementTarget> targets);

    List<SettlementTargetSummary> findSummary(final LocalDate settlementDate);
}
