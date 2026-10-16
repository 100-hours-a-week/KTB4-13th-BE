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

    public void transfer(final LocalDate targetDate, final List<Merchant> merchants, final List<Settlement> settlements) {
        final Map<Long, Merchant> merchantMap = new HashMap<>();
        merchants.forEach((final var merchant) -> merchantMap.put(merchant.id(), merchant));
        final var settlementMap = settlements.stream().collect(Collectors.groupingBy(Settlement::merchantId));
        for (final var settlementGroup : settlementMap.entrySet()) {
            final var merchantId = settlementGroup.getKey();
            final var merchant = merchantMap.get(merchantId);
            try {
                if (merchant == null) {
                    log.warn("정산 가맹점 정보 없음 merchantId: {}", merchantId);
                    continue;
                }
                if (targetDate.getDayOfMonth() % merchant.settlementCycle() != 0) {
                    log.info("정산 주기 아님 merchantId: {}, settlementCycle: {}, dayOfMonth: {}", merchantId, merchant.settlementCycle(),
                        targetDate.getDayOfMonth());
                    continue;
                }
                final var transferAmount =
                    settlementGroup.getValue().stream().map(Settlement::settlementAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
                if (transferAmount.compareTo(BigDecimal.ZERO) <= 0) {
                    log.warn("가맹점 미정산 금액 확인 필요 merchantId: {}, transferAmount: {}", merchantId, transferAmount);
                    continue;
                }
                // NOTE: 참고 구현과 동일하게 외부 이체 API는 미연동이다. SENT는 실제 송금 증거가 아니다.
                completeSettlementTransferUseCase.execute(settlementGroup.getValue());
            } catch (final Exception exception) {
                log.error("가맹점 정산 실패 merchantId: {}", merchantId, exception);
            }
        }
    }
}
