package com.book.core.settlement.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.settlement.application.port.SettlementTargetRepositoryPort;
import com.book.core.settlement.domain.SettlementTargetSummary;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class GetSettlementTargetsUseCase {
    private final SettlementTargetRepositoryPort settlementTargetRepository;

    @Transactional(readOnly = true)
    public List<SettlementTargetSummary> execute(final LocalDate targetDate) {
        return settlementTargetRepository.findSummary(targetDate);
    }
}
