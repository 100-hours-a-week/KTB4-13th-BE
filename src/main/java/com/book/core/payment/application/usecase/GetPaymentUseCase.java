package com.book.core.payment.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.payment.application.port.PaymentRepositoryPort;
import com.book.core.payment.domain.Payment;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

/** 주문 ID에 연결된 결제의 공개 조회 계약입니다. */
@UseCase
@RequiredArgsConstructor
public class GetPaymentUseCase {
    private final PaymentRepositoryPort paymentRepository;

    /** 주문 ID에 연결된 결제를 반환합니다. 대상이 없으면 NOT_FOUND_DATA로 거절합니다. */
    @Transactional(readOnly = true)
    public Payment execute(final Long orderId) {
        return paymentRepository.findByOrderId(orderId).orElseThrow(() -> new CoreException(ErrorCode.NOT_FOUND_DATA));
    }
}
