package com.book.core.notification.infrastructure.messaging;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "notification.redis.enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
class NotificationRedisListenerSupervisor {
    private static final long RETRY_INTERVAL_SECONDS = 5;
    private final RedisMessageListenerContainer listenerContainer;
    private final MeterRegistry metrics;
    private final ScheduledExecutorService executor =
        Executors.newSingleThreadScheduledExecutor(Thread.ofPlatform().daemon().name("notification-redis-listener").factory());

    NotificationRedisListenerSupervisor(
        @Qualifier("notificationRedisListener") final RedisMessageListenerContainer listenerContainer,
        final MeterRegistry metrics
    ) {
        this.listenerContainer = listenerContainer;
        this.metrics = metrics;
    }

    @EventListener(ApplicationReadyEvent.class)
    void retryUntilConnected() {
        executor.scheduleWithFixedDelay(this::startListener, 0, RETRY_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    void startListener() {
        if (listenerContainer.isRunning()) {
            return;
        }
        try {
            listenerContainer.start();
            metrics.counter("notification.redis.listener.start", "result", "success").increment();
        } catch (final RuntimeException exception) {
            try {
                listenerContainer.stop();
            } catch (final RuntimeException stopException) {
                metrics.counter("notification.redis.listener.reset", "result", "failure").increment();
                log.warn("Redis 알림 구독을 초기화하지 못했습니다. 다음 재시도를 확인합니다.");
            }
            metrics.counter("notification.redis.listener.start", "result", "failure").increment();
            log.warn("Redis 알림 구독을 시작하지 못했습니다. 5초 뒤 다시 시도합니다.");
        }
    }

    @PreDestroy
    void close() {
        executor.shutdownNow();
        if (listenerContainer.isRunning()) {
            listenerContainer.stop();
        }
    }
}
