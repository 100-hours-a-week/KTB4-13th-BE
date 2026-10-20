package com.book.core.settlement.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.settlement.application.port.SettlementRepositoryPort;
import com.book.core.settlement.domain.Settlement;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class CompleteSettlementTransferUseCase {
    private final SettlementRepositoryPort settlementRepository;

    /** 저장된 정산을 다시 조회해 지급 상태를 SENT로 변경한다. */
    @Transactional
    public void execute(final List<Settlement> settlements) {
        final List<Settlement> entities = settlementRepository.findAllById(settlements.stream().map(Settlement::id).toList());
        entities.forEach(Settlement::sent);
        settlementRepository.saveAll(entities);
        log.debug("정산 지급 상태 변경 요청 count: {}", entities.size());
    }
}
