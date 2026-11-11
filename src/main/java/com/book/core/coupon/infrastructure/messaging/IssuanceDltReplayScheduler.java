package com.book.core.coupon.infrastructure.messaging;

import com.book.core.coupon.application.usecase.ReplayIssuanceDltUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "coupon.kafka.worker-enabled", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
class IssuanceDltReplayScheduler {
    private final ReplayIssuanceDltUseCase replay;

    @Scheduled(fixedDelay = 1000, scheduler = "couponDltReplayExecutor")
    void replayRequested() {
        try {
            replay.execute();
        } catch (final RuntimeException failure) {
            log.atWarn().addKeyValue("errorType", failure.getClass().getSimpleName()).log("[COUPON] 실패 건 재발행을 다시 시도합니다.");
        }
    }
}
