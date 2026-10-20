package com.book.core.settlement.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.paging.SliceResult;
import com.book.core.cancel.application.usecase.GetCancelsForSettlementUseCase;
import com.book.core.cancel.domain.Cancel;
import com.book.core.payment.application.usecase.GetPaymentsForSettlementUseCase;
import com.book.core.payment.domain.Payment;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 결제와 취소를 페이지별로 읽어 정산 대상을 적재한다. */
@UseCase
@RequiredArgsConstructor
@Slf4j
public class LoadSettlementTargetsUseCase {
    private static final int PAGE_SIZE = 1000;
    private final GetPaymentsForSettlementUseCase getPaymentsForSettlementUseCase;
    private final GetCancelsForSettlementUseCase getCancelsForSettlementUseCase;
    private final CreateSettlementTargetsUseCase createSettlementTargetsUseCase;

    /** 결제 적재 후 취소를 적재한다. 적재에 실패한 페이지는 기록하고 다음 페이지를 처리한다. */
    public void execute(final LocalDate targetDate, final LocalDateTime from, final LocalDateTime to) {
        log.info("[SETTLEMENT] 정산 대상 적재 시작 targetDate: {}", targetDate);
        processPayments(targetDate, from, to);
        processCancels(targetDate, from, to);
        log.info("[SETTLEMENT] 정산 대상 적재 종료 targetDate: {}", targetDate);
    }

    /** 결제를 천 건씩 조회하고 페이지별로 정산 대상을 저장한다. */
    private void processPayments(final LocalDate targetDate, final LocalDateTime from, final LocalDateTime to) {
        int page = 0;
        boolean hasNext;
        do {
            final SliceResult<Payment> payments = getPaymentsForSettlementUseCase.execute(from, to, page, PAGE_SIZE);
            try {
                createSettlementTargetsUseCase.processPayments(targetDate, payments.content());
                log.info("[SETTLEMENT] 결제 정산 대상 페이지 처리 완료 targetDate: {}, page: {}, count: {}", targetDate, page,
                    payments.content().size());
            } catch (final Exception exception) {
                log.error("[SETTLEMENT] 결제 정산 대상 생성 실패 page: {}, size: {}", page, PAGE_SIZE, exception);
            }
            hasNext = payments.hasNext();
            page++;
        } while (hasNext);
    }

    /** 취소를 천 건씩 조회하고 페이지별로 정산 대상을 저장한다. */
    private void processCancels(final LocalDate targetDate, final LocalDateTime from, final LocalDateTime to) {
        int page = 0;
        boolean hasNext;
        do {
            final SliceResult<Cancel> cancels = getCancelsForSettlementUseCase.execute(from, to, page, PAGE_SIZE);
            try {
                createSettlementTargetsUseCase.processCancels(targetDate, cancels.content());
                log.info("[SETTLEMENT] 취소 정산 대상 페이지 처리 완료 targetDate: {}, page: {}, count: {}", targetDate, page, cancels.content().size());
            } catch (final Exception exception) {
                log.error("[SETTLEMENT] 취소 정산 대상 생성 실패 page: {}, size: {}", page, PAGE_SIZE, exception);
            }
            hasNext = cancels.hasNext();
            page++;
        } while (hasNext);
    }
}
