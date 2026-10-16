package com.book.core.settlement.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.settlement.application.port.SettlementRepositoryPort;
import com.book.core.settlement.domain.Settlement;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class CompleteSettlementTransferUseCase {
    private final SettlementRepositoryPort settlementRepository;

    @Transactional
    public void execute(final List<Settlement> settlements) {
        final var entities = settlementRepository.findAllById(settlements.stream().map(Settlement::id).toList());
        entities.forEach(Settlement::sent);
        settlementRepository.saveAll(entities);
    }
}
