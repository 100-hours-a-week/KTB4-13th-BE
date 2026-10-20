package com.book.core.settlement.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.merchant.domain.Merchant;
import com.book.core.settlement.domain.Settlement;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 가맹점별 주기와 합계를 확인한다. 전체 반복에 트랜잭션을 두지 않아 가맹점별 실패를 격리한다. */
@UseCase
@RequiredArgsConstructor
@Slf4j
public class TransferSettlementsUseCase {
    private final CompleteSettlementTransferUseCase completeSettlementTransferUseCase;

    /**
     * 정산 주기에 해당하고 합계가 양수인 가맹점의 정산을 SENT로 변경한다. 외부 이체 API 연동 전이므로 지급 상태만 변경한다.
     */
    public void transfer(final LocalDate targetDate, final List<Merchant> merchants, final List<Settlement> settlements) {
        final Map<Long, Merchant> merchantMap = new HashMap<>();
        merchants.forEach((final Merchant merchant) -> merchantMap.put(merchant.id(), merchant));
        final Map<Long, List<Settlement>> settlementMap = settlements.stream().collect(Collectors.groupingBy(Settlement::merchantId));
        for (final Map.Entry<Long, List<Settlement>> settlementGroup : settlementMap.entrySet()) {
            final Long merchantId = settlementGroup.getKey();
            final Merchant merchant = merchantMap.get(merchantId);
            try {
                if (merchant == null) {
                    log.atWarn().addKeyValue("merchantId", merchantId).log("[SETTLEMENT] 정산 가맹점 정보 없음 merchantId: {}", merchantId);
                    continue;
                }
                if (targetDate.getDayOfMonth() % merchant.settlementCycle() != 0) {
                    log.atDebug().addKeyValue("merchantId", merchantId).log(
                        "[SETTLEMENT] 정산 주기가 아니어서 지급을 건너뜁니다. merchantId={}, settlementCycle={}, dayOfMonth={}", merchantId,
                        merchant.settlementCycle(), targetDate.getDayOfMonth());
                    continue;
                }
                final BigDecimal transferAmount =
                    settlementGroup.getValue().stream().map(Settlement::settlementAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
                if (transferAmount.compareTo(BigDecimal.ZERO) <= 0) {
                    log.atWarn().addKeyValue("merchantId", merchantId)
                        .log("[SETTLEMENT] 가맹점 미정산 금액 확인 필요 merchantId: {}, transferAmount: {}", merchantId, transferAmount);
                    continue;
                }
                completeSettlementTransferUseCase.execute(settlementGroup.getValue());
                log.atInfo().addKeyValue("merchantId", merchantId).log("[SETTLEMENT] 가맹점 정산 지급 상태 변경 완료 merchantId: {}, count: {}",
                    merchantId, settlementGroup.getValue().size());
            } catch (final Exception exception) {
                log.atError().addKeyValue("merchantId", merchantId).log("[SETTLEMENT] 가맹점 정산 실패 merchantId: {}", merchantId, exception);
            }
        }
    }
}
