package com.book.core.payment.infrastructure.client.toss;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.payment.application.port.PaymentClient;
import com.book.core.payment.application.result.PgPaymentResult;
import java.math.BigDecimal;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

@Component
@RequiredArgsConstructor
class PaymentClientImpl implements PaymentClient {
    private final TossPaymentHttpClient client;

    @Override
    public PgPaymentResult approve(final String paymentKey, final String orderKey, final BigDecimal amount, final String idempotencyKey) {
        return invoke(() -> client.approve(idempotencyKey, new TossPaymentHttpClient.ApproveRequest(paymentKey, orderKey, amount)));
    }

    @Override
    public PgPaymentResult find(final String paymentKey) {
        return invoke(() -> client.find(paymentKey));
    }

    @Override
    public PgPaymentResult cancel(final String paymentKey, final BigDecimal amount, final String idempotencyKey) {
        return invoke(() -> client.cancel(paymentKey, idempotencyKey, new TossPaymentHttpClient.CancelRequest("고객 요청", amount)));
    }

    private PgPaymentResult invoke(final Supplier<TossPaymentResponse> action) {
        try {
            return TossPaymentExternalMapper.map(action.get());
        } catch (final RestClientException exception) {
            throw new CoreException(ErrorCode.PG_UNAVAILABLE);
        }
    }
}
