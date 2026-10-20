package com.book.core.settlement.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.settlement.application.port.SettlementRepositoryPort;
import com.book.core.settlement.domain.Settlement;
import com.book.core.settlement.domain.SettlementAmount;
import com.book.core.settlement.domain.SettlementCalculator;
import com.book.core.settlement.domain.SettlementState;
import com.book.core.settlement.domain.SettlementTargetSummary;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

/** 가맹점별 정산을 생성한다. 최근 SENT 매출 누락 시 실패하는 참고 계약을 유지한다. */
@UseCase
@RequiredArgsConstructor
@Slf4j
public class GenerateSettlementsUseCase {
    private final SettlementRepositoryPort settlementRepository;
    private final SettlementCalculator settlementCalculator = new SettlementCalculator();

    /** 가맹점별 대상 금액에서 수수료를 차감한 정산을 READY 상태로 저장한다. */
    @Transactional
    public int execute(final List<SettlementTargetSummary> summaries, final Map<Long, BigDecimal> recentSalesAmountMap) {
        final List<Settlement> settlements = summaries.stream().map((final SettlementTargetSummary summary) -> {
            final SettlementAmount calculated =
                settlementCalculator.calculate(summary.targetAmount(), recentSalesAmountMap.get(summary.merchantId()));
            return new Settlement(summary.merchantId(), summary.settlementDate(), calculated.originalAmount(), calculated.feeAmount(),
                calculated.feeRate(), calculated.settlementAmount(), SettlementState.READY);
        }).toList();
        settlementRepository.saveAll(settlements);
        log.debug("정산 저장 요청 count: {}", settlements.size());
        return settlements.size();
    }
}
