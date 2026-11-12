package com.book.core.coupon.application.service;

import com.book.core.coupon.application.command.ReplayIssuanceDltCommand;
import com.book.core.coupon.application.usecase.ReplayIssuanceDltUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "coupon.kafka.worker-enabled", matchIfMissing = true)
public class IssuanceDltService {
    private final ReplayIssuanceDltUseCase replay;

    public int replay(final ReplayIssuanceDltCommand command) {
        return replay.execute(command.ids());
    }
}
