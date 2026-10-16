package com.book.core.settlement.application.service;

import com.book.core.cancel.application.usecase.GetCancelsForSettlementUseCase;
import com.book.core.payment.application.usecase.GetPaymentsForSettlementUseCase;
import com.book.core.settlement.application.usecase.CreateSettlementTargetsUseCase;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** 정산 대상 적재, 계산, 이체 상태 처리를 순서대로 조율한다. */
@Service
@RequiredArgsConstructor
@Slf4j
public class SettlementService {
    private static final int PAGE_SIZE = 1000;
    private final GetPaymentsForSettlementUseCase getPaymentsForSettlementUseCase;
    private final GetCancelsForSettlementUseCase getCancelsForSettlementUseCase;
    private final CreateSettlementTargetsUseCase createSettlementTargetsUseCase;

    public void loadTargets(final LocalDate targetDate, final LocalDateTime from, final LocalDateTime to) {
        processPayments(targetDate, from, to);
        processCancels(targetDate, from, to);
    }



    private void processPayments(final LocalDate targetDate, final LocalDateTime from, final LocalDateTime to) {
        int page = 0;
        boolean hasNext;
        do {
            final var payments = getPaymentsForSettlementUseCase.execute(from, to, page, PAGE_SIZE);
            try {
                createSettlementTargetsUseCase.processPayments(targetDate, payments.content());
            } catch (final Exception exception) {
                log.error("결제 정산 대상 생성 실패 page: {}, size: {}", page, PAGE_SIZE, exception);
            }
            hasNext = payments.hasNext();
            page++;
        } while (hasNext);
    }

    private void processCancels(final LocalDate targetDate, final LocalDateTime from, final LocalDateTime to) {
        int page = 0;
        boolean hasNext;
        do {
            final var cancels = getCancelsForSettlementUseCase.execute(from, to, page, PAGE_SIZE);
            try {
                createSettlementTargetsUseCase.processCancels(targetDate, cancels.content());
            } catch (final Exception exception) {
                log.error("취소 정산 대상 생성 실패 page: {}, size: {}", page, PAGE_SIZE, exception);
            }
            hasNext = cancels.hasNext();
            page++;
        } while (hasNext);
    }
}
