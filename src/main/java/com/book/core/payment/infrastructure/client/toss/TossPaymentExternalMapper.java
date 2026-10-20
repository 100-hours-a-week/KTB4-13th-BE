package com.book.core.payment.infrastructure.client.toss;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.payment.application.result.PgPaymentResult;
import com.book.core.payment.domain.enums.PaymentMethod;
import java.util.List;

final class TossPaymentExternalMapper {
    private TossPaymentExternalMapper() {}

    static PgPaymentResult map(final TossPaymentResponse response) {
        if (response == null || response.paymentKey() == null || response.orderId() == null || response.totalAmount() == null
            || response.balanceAmount() == null || response.status() == null) {
            throw new CoreException(ErrorCode.PG_RESPONSE_MISMATCH);
        }
        PaymentMethod method = null;
        String approveCode = null;
        if (response.card() != null && "카드".equals(response.method())) {
            method = PaymentMethod.CARD;
            approveCode = response.card().approveNo();
        } else if (response.card() != null && "간편결제".equals(response.method())) {
            method = PaymentMethod.EASY_PAY;
            approveCode = response.card().approveNo();
        }
        List<PgPaymentResult.Cancellation> cancellations = List.of();
        if (response.cancels() != null) {
            cancellations = response.cancels().stream().map(item -> new PgPaymentResult.Cancellation(item.transactionKey(),
                item.cancelAmount(), item.refundableAmount(), item.cancelStatus(), item.canceledAt())).toList();
        }
        return new PgPaymentResult(response.paymentKey(), response.orderId(), response.totalAmount(), response.balanceAmount(),
            response.status(), method, approveCode, cancellations);
    }
}
