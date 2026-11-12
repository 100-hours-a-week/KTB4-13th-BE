package com.book.core.coupon.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import java.util.List;
import com.book.core.coupon.application.port.IssuanceDltLogRepositoryPort;
import com.book.core.coupon.application.port.IssuanceRequestPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@ConditionalOnProperty(name = "coupon.kafka.worker-enabled", matchIfMissing = true)
@RequiredArgsConstructor
public class ReplayIssuanceDltUseCase {
    private final IssuanceDltLogRepositoryPort logs;
    private final IssuanceRequestPublisher publisher;

    @Transactional
    public int execute(final List<Long> ids) {
        final var selected = ids.stream().distinct().toList();
        if (selected.isEmpty() || selected.size() > 100) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        final var requests = logs.lockPending(selected);
        if (requests.size() != selected.size()) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        for (final var request : requests) {
            publisher.publishAndWait(request.messageKey(), request.payload());
            logs.markReplayed(request.id());
        }
        return requests.size();
    }
}
