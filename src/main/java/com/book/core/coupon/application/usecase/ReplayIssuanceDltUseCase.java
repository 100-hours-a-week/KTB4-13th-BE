package com.book.core.coupon.application.usecase;

import com.book.common.annotation.UseCase;
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
    public void execute() {
        logs.lockNextRequested().ifPresent(request -> {
            publisher.publishAndWait(request.messageKey(), request.payload());
            logs.markReplayed(request.id());
        });
    }
}
