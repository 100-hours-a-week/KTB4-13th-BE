package com.book.core.payment.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.order.application.usecase.LockPaymentOrderUseCase;
import com.book.core.payment.application.command.PreparePgOperationCommand;
import com.book.core.payment.application.port.PgOperationRepositoryPort;
import com.book.core.payment.domain.PgOperation;
import com.book.core.payment.domain.PgOperationType;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Records unresolved operations independently from the failed payment transaction. */
@UseCase
@RequiredArgsConstructor
@Slf4j
public class MarkPgOperationUncertainUseCase {
    private static final Duration REVIEW_DELAY = Duration.ofMinutes(10);
    private final LockPaymentOrderUseCase lockOrder;
    private final PgOperationRepositoryPort operations;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void execute(final PreparePgOperationCommand command, final boolean urgent) {
        lockOrder.execute(command.orderKey());
        final Optional<PgOperation> matching = operations.findByRequestHash(command.requestHash())
            .filter((final PgOperation operation) -> operation.orderKey().equals(command.orderKey()) && operation.type() == command.type()
                && operation.inputHash().equals(command.inputHash()));
        final Optional<PgOperation> target;
        if (matching.isEmpty() && command.type() == PgOperationType.APPROVE) {
            target = operations.findByRequestHash(command.isolatedApprovalHash());
        } else {
            target = matching;
        }
        target.ifPresent((final PgOperation operation) -> {
            operation.verifyInput(command.orderKey(), command.inputHash(), command.type());
            mark(operation, urgent);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void execute(final PgOperation candidate, final boolean urgent) {
        lockOrder.execute(candidate.orderKey());
        operations.findByRequestHash(candidate.requestHash()).ifPresent((final PgOperation operation) -> mark(operation, urgent));
    }

    private void mark(final PgOperation operation, final boolean urgent) {
        if (operation.completed() || operation.rejected()) {
            return;
        }
        final Instant now = Instant.now();
        operation.markUncertain(now);
        if ((urgent || !now.isBefore(operation.uncertainAt().plus(REVIEW_DELAY))) && operation.requireReview()) {
            // NOTE(케빈): 운영 알림 채널과 확인·복구 권한, 담당자 대응 절차를 함께 정하면 좋겠습니다.
            log.atError().addKeyValue("pgOperationId", operation.id()).log("[PAYMENT] 결제 결과 운영 확인이 필요합니다.");
        }
    }
}
