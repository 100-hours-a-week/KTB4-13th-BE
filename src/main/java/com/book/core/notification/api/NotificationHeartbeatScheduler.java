package com.book.core.notification.api;

import jakarta.annotation.PreDestroy;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** 다른 업무의 Spring 스케줄러와 분리된 단일 SSE heartbeat 실행기입니다. */
@Component
@Slf4j
class NotificationHeartbeatScheduler {
    private final ScheduledExecutorService executor =
        Executors.newSingleThreadScheduledExecutor(Thread.ofPlatform().daemon().name("notification-sse-heartbeat").factory());

    void start(final Runnable heartbeat) {
        start(heartbeat, 30, TimeUnit.SECONDS);
    }

    void start(final Runnable heartbeat, final long interval, final TimeUnit unit) {
        executor.scheduleWithFixedDelay(() -> {
            try {
                heartbeat.run();
            } catch (final RuntimeException exception) {
                log.warn("SSE heartbeat 실행에 실패했습니다. 다음 주기에 다시 시도합니다.", exception);
            }
        }, interval, interval, unit);
    }

    @PreDestroy
    void close() {
        executor.shutdownNow();
    }
}
