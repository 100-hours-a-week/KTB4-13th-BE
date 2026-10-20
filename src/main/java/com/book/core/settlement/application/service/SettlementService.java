package com.book.core.settlement.application.service;

import com.book.core.merchant.application.usecase.GetMerchantsUseCase;
import com.book.core.merchant.domain.Merchant;
import com.book.core.settlement.application.usecase.GenerateSettlementsUseCase;
import com.book.core.settlement.application.usecase.GetSettlementTargetsUseCase;
import com.book.core.settlement.application.usecase.GetSettlementsUseCase;
import com.book.core.settlement.application.usecase.LoadSettlementTargetsUseCase;
import com.book.core.settlement.application.usecase.TransferSettlementsUseCase;
import com.book.core.settlement.domain.Settlement;
import com.book.core.settlement.domain.SettlementState;
import com.book.core.settlement.domain.SettlementTargetSummary;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** 정산 대상 적재, 계산, 이체 상태 처리를 순서대로 조율한다. */
@Service
@RequiredArgsConstructor
@Slf4j
public class SettlementService {
    private final LoadSettlementTargetsUseCase loadSettlementTargetsUseCase;
    private final GetSettlementTargetsUseCase getSettlementTargetsUseCase;
    private final GetSettlementsUseCase getSettlementsUseCase;
    private final GenerateSettlementsUseCase generateSettlementsUseCase;
    private final GetMerchantsUseCase getMerchantsUseCase;
    private final TransferSettlementsUseCase transferSettlementsUseCase;

    /** 지정한 기간의 결제와 취소를 정산 대상으로 적재한다. */
    public void loadTargets(final LocalDate targetDate, final LocalDateTime from, final LocalDateTime to) {
        loadSettlementTargetsUseCase.execute(targetDate, from, to);
    }

    /** 정산일의 가맹점별 대상 금액과 최근 매출을 조회해 정산을 생성한다. */
    public int generate(final LocalDate targetDate) {
        log.info("[SETTLEMENT] 정산 생성 시작 targetDate: {}", targetDate);
        final List<SettlementTargetSummary> summaries = getSettlementTargetsUseCase.execute(targetDate);
        final Map<Long, BigDecimal> recentSalesAmountMap =
            getSettlementsUseCase.recentSalesAmounts(targetDate, summaries.stream().map(SettlementTargetSummary::merchantId).toList());
        final int count = generateSettlementsUseCase.execute(summaries, recentSalesAmountMap);
        log.info("[SETTLEMENT] 정산 생성 완료 targetDate: {}, count: {}", targetDate, count);
        return count;
    }

    /** 지급 대기 정산과 가맹점을 조회해 지급 상태 처리를 요청한다. */
    public void transfer(final LocalDate targetDate) {
        log.info("[SETTLEMENT] 정산 지급 상태 처리 시작 targetDate: {}", targetDate);
        final List<Settlement> readySettlements = getSettlementsUseCase.byState(SettlementState.READY);
        final List<Merchant> merchants = getMerchantsUseCase.execute(readySettlements.stream().map(Settlement::merchantId).toList());
        transferSettlementsUseCase.transfer(targetDate, merchants, readySettlements);
        log.info("[SETTLEMENT] 정산 지급 상태 처리 종료 targetDate: {}, readyCount: {}", targetDate, readySettlements.size());
    }

}
