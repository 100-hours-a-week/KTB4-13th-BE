package com.book.core.payment.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.payment.application.port.PaymentClient;
import com.book.core.payment.application.result.CanceledPaymentResult;
import com.book.core.payment.application.result.PgPaymentResult;
import com.book.core.payment.domain.Payment;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class RequestPgCancelUseCase {
    private final PaymentClient client;
    private final GetSuccessfulPaymentPgOrderIdUseCase getPgOrderId;

    public CanceledPaymentResult execute(final String orderKey, final Payment payment, final BigDecimal amount,
        final BigDecimal refundableAmount, final String idempotencyKey) {
        if (amount.signum() < 0 || amount.stripTrailingZeros().scale() > 0 || refundableAmount.compareTo(amount) < 0) {
            throw new CoreException(ErrorCode.PAYMENT_INVALID_AMOUNT);
        }
        if (amount.signum() == 0) {
            return new CanceledPaymentResult("", OffsetDateTime.now());
        }
        final String pgOrderId = getPgOrderId.execute(payment.id(), orderKey);
        final PgPaymentResult result = client.cancel(payment.externalPaymentKey(), amount, idempotencyKey);
        final BigDecimal expectedBalance = refundableAmount.subtract(amount);
        if (result == null || !pgOrderId.equals(result.orderKey()) || !payment.externalPaymentKey().equals(result.paymentKey())
            || result.totalAmount() == null || result.totalAmount().compareTo(payment.paidAmount()) != 0 || result.balanceAmount() == null
            || result.balanceAmount().compareTo(expectedBalance) != 0
            || (!"CANCELED".equals(result.status()) && !"PARTIAL_CANCELED".equals(result.status()))) {
            throw new CoreException(ErrorCode.PG_RESPONSE_MISMATCH);
        }
        final List<PgPaymentResult.Cancellation> matches = result.cancellations().stream()
            .filter(item -> "DONE".equals(item.status()) && item.amount() != null && item.amount().compareTo(amount) == 0
                && item.refundableAmount() != null && item.refundableAmount().compareTo(expectedBalance) == 0
                && item.transactionKey() != null && !item.transactionKey().isBlank() && item.canceledAt() != null)
            .toList();
        if (matches.size() != 1) {
            throw new CoreException(ErrorCode.PG_RESPONSE_MISMATCH);
        }
        return new CanceledPaymentResult(matches.getFirst().transactionKey(), matches.getFirst().canceledAt());
    }
}
