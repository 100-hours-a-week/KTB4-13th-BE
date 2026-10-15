package com.book.core.cancel.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.cancel.application.port.CancelBalanceRepositoryPort;
import com.book.core.cancel.domain.CancelBalance;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

/** 결제 시점의 취소 가능 금액을 기록합니다. */
@UseCase
@RequiredArgsConstructor
public class CreateCancelBalanceUseCase {
    private final CancelBalanceRepositoryPort cancelBalanceRepository;

    /** 결제 당시 실결제액과 사용 포인트 및 쿠폰 금액으로 주문의 취소 가능 잔액을 초기화합니다. */
    @Transactional
    public void execute(final Long orderId, final BigDecimal paidAmount, final BigDecimal pointAmount, final BigDecimal couponAmount) {
        cancelBalanceRepository
            .save(new CancelBalance(orderId, paidAmount, pointAmount, couponAmount, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
    }
}
