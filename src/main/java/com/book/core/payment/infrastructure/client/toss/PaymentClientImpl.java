package com.book.core.payment.infrastructure.client.toss;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.payment.application.port.PaymentClient;
import com.book.core.payment.application.result.PgPaymentResult;
import feign.FeignException;
import java.math.BigDecimal;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class PaymentClientImpl implements PaymentClient {
    private final TossPaymentFeignClient client;

    @Override
    public PgPaymentResult approve(final String paymentKey, final String orderKey, final BigDecimal amount, final String idempotencyKey) {
        return invoke(() -> client.approve(idempotencyKey, new TossPaymentFeignClient.ApproveRequest(paymentKey, orderKey, amount)));
    }

    @Override
    public PgPaymentResult find(final String paymentKey) {
        return invoke(() -> client.find(paymentKey));
    }

    @Override
    public PgPaymentResult cancel(final String paymentKey, final BigDecimal amount, final BigDecimal refundableAmount,
        final String idempotencyKey) {
        return invoke(
            () -> client.cancel(paymentKey, idempotencyKey, new TossPaymentFeignClient.CancelRequest("고객 요청", amount, refundableAmount)));
    }

    private PgPaymentResult invoke(final Supplier<TossPaymentResponse> action) {
        try {
            return TossPaymentExternalMapper.map(action.get());
        } catch (final FeignException exception) {
            throw new CoreException(ErrorCode.PG_UNAVAILABLE);
        }
    }
}
