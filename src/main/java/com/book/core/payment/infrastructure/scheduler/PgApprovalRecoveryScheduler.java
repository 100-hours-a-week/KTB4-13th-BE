package com.book.core.payment.infrastructure.scheduler;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.payment.application.command.ApprovePaymentCommand;
import com.book.core.payment.application.port.PgOperationRepositoryPort;
import com.book.core.payment.application.service.PaymentService;
import com.book.core.payment.application.usecase.MarkPgOperationUncertainUseCase;
import com.book.core.payment.application.usecase.CompensatePgApprovalUseCase;
import com.book.core.payment.domain.PgOperation;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Rechecks approval operations whose result could not be recorded. */
@Component
@RequiredArgsConstructor
@Slf4j
public class PgApprovalRecoveryScheduler {
    private static final Duration IN_FLIGHT_DELAY = Duration.ofSeconds(30);
    private static final Duration REVIEW_DELAY = Duration.ofMinutes(10);
    private static final Duration RETRY_WINDOW = Duration.ofDays(14);
    private final PgOperationRepositoryPort operations;
    private final PaymentService payments;
    private final MarkPgOperationUncertainUseCase markUncertain;
    private final CompensatePgApprovalUseCase compensate;

    @Scheduled(fixedDelay = 60000, initialDelay = 60000)
    public void recover() {
        for (final PgOperation operation : operations.findPendingApprovals()) {
            try {
                recover(operation);
            } catch (final RuntimeException exception) {
                // Do not log payment keys or raw external responses.
                log.atWarn().addKeyValue("pgOperationId", operation.id()).log("[PAYMENT] 결제 결과 재확인을 완료하지 못했습니다.");
            }
        }
    }

    private void recover(final PgOperation operation) {
        final Instant now = Instant.now();
        final Instant startedAt;
        if (operation.compensationRequested()) {
            startedAt = operation.compensationRequestedAt();
        } else {
            startedAt = operation.requestedAt();
        }
        if (operation.approvalAmount() == null || operation.pgOrderId() == null || (operation.approvalAmount().signum() > 0
            && (operation.externalPaymentKey() == null || operation.externalPaymentKey().isBlank())) || startedAt == null) {
            markUncertain.execute(operation, true);
            return;
        }
        if (startedAt.plus(RETRY_WINDOW).isBefore(now)) {
            markUncertain.execute(operation, true);
            return;
        }
        if (operation.uncertainAt() == null && startedAt.plus(IN_FLIGHT_DELAY).isAfter(now)) {
            return;
        }
        if (operation.uncertainAt() != null && !now.isBefore(operation.uncertainAt().plus(REVIEW_DELAY))) {
            markUncertain.execute(operation, false);
            return;
        }
        markUncertain.execute(operation, false);
        try {
            if (operation.compensationRequested()) {
                compensate.execute(operation.orderKey(), operation.requestHash());
            } else {
                payments.success(new ApprovePaymentCommand(operation.orderKey(), operation.pgOrderId(), operation.externalPaymentKey(),
                    operation.approvalAmount(), operation.attemptId()));
            }
        } catch (final CoreException exception) {
            if (exception.errorCode() == ErrorCode.PG_RESPONSE_MISMATCH || exception.errorCode() == ErrorCode.PG_REVIEW_REQUIRED) {
                markUncertain.execute(operation, true);
            }
        }
    }
}
