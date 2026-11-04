package com.book.core.payment.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.payment.application.port.PaymentAttemptRepositoryPort;
import com.book.core.payment.application.port.PaymentRepositoryPort;
import com.book.core.payment.application.port.PgOperationRepositoryPort;
import com.book.core.payment.domain.Payment;
import com.book.core.payment.domain.PaymentAttempt;
import com.book.core.payment.domain.PaymentAttemptState;
import com.book.core.payment.domain.PgOperation;
import com.book.core.payment.domain.PgOperationType;
import com.book.core.payment.domain.enums.PaymentState;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Detects checkouts whose external result cannot be ruled out under the caller's order lock. */
@UseCase
@RequiredArgsConstructor
@Slf4j
public class HasUnconfirmedCheckoutUseCase {
    private final PaymentRepositoryPort payments;
    private final PaymentAttemptRepositoryPort attempts;
    private final PgOperationRepositoryPort operations;

    @Transactional(propagation = Propagation.MANDATORY, readOnly = true)
    public boolean execute(final Long orderId) {
        final var payment = payments.findByOrderId(orderId);
        if (payment.isEmpty() || payment.get().state() != PaymentState.READY || payment.get().paidAmount().signum() == 0) {
            return false;
        }
        // NOTE(케빈): 결제창을 열지 않은 시도를 안전하게 구분할 방법과 확인 대상 해제 절차를 검토해 주세요.
        return attempts.findLatestByPaymentId(payment.get().id())
            .map((final PaymentAttempt attempt) -> attempt.state() == PaymentAttemptState.READY).orElse(true);
    }

    /** Records an unresolved expired checkout while the caller holds the order lock. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void requireReview(final String orderKey, final Long orderId) {
        if (operations.existsPending(orderKey)) {
            return;
        }
        final Optional<Payment> payment = payments.findByOrderId(orderId);
        if (payment.isEmpty() || payment.get().state() != PaymentState.READY || payment.get().paidAmount().signum() <= 0) {
            return;
        }
        final Optional<PaymentAttempt> attempt = attempts.findLatestByPaymentId(payment.get().id());
        if (attempt.isPresent() && attempt.get().state() != PaymentAttemptState.READY) {
            return;
        }
        final String eventKey;
        if (attempt.isPresent()) {
            eventKey = "unidentified-payment-attempt:" + attempt.get().id();
        } else {
            eventKey = "unidentified-legacy-payment:" + payment.get().id();
        }
        final String hash = PgOperation.hash(eventKey);
        if (operations.findByRequestHash(hash).isPresent()) {
            return;
        }
        final PgOperation operation = new PgOperation(orderKey, hash, hash, PgOperationType.APPROVE);
        attempt.ifPresent((final PaymentAttempt item) -> operation.recordUnidentifiedApprovalDetails(payment.get().paidAmount(),
            item.pgOrderId(), item.id()));
        operation.markUncertain(Instant.now());
        operation.requireReview();
        operations.save(operation);
        log.error("[PAYMENT] 만료된 주문의 결제 결과를 확인할 수 없어 운영 확인이 필요합니다.");
    }
}
