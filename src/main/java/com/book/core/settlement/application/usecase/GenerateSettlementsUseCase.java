package com.book.core.settlement.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.settlement.application.port.SettlementRepositoryPort;
import com.book.core.settlement.domain.Settlement;
import com.book.core.settlement.domain.SettlementCalculator;
import com.book.core.settlement.domain.SettlementState;
import com.book.core.settlement.domain.SettlementTargetSummary;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

/** 가맹점별 정산을 생성한다. 최근 SENT 매출 누락 시 실패하는 참고 계약을 유지한다. */
@UseCase
@RequiredArgsConstructor
public class GenerateSettlementsUseCase {
    private final SettlementRepositoryPort settlementRepository;
    private final SettlementCalculator settlementCalculator = new SettlementCalculator();

    @Transactional
    public int execute(final List<SettlementTargetSummary> summaries, final Map<Long, BigDecimal> recentSalesAmountMap) {
        final var settlements = summaries.stream().map((final var summary) -> {
            final var calculated = settlementCalculator.calculate(summary.targetAmount(), recentSalesAmountMap.get(summary.merchantId()));
            return new Settlement(summary.merchantId(), summary.settlementDate(), calculated.originalAmount(), calculated.feeAmount(),
                calculated.feeRate(), calculated.settlementAmount(), SettlementState.READY);
        }).toList();
        settlementRepository.saveAll(settlements);
        return settlements.size();
    }
}
