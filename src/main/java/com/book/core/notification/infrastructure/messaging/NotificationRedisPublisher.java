package com.book.core.notification.infrastructure.messaging;

import com.book.core.notification.application.event.NotificationCreatedEvent;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** The DB commit is authoritative; Pub/Sub has no replay. */
@Component
@ConditionalOnProperty(name = "notification.redis.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
class NotificationRedisPublisher {
    static final String CHANNEL = "book:notification:created";
    private final StringRedisTemplate redis;
    private final MeterRegistry metrics;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(final NotificationCreatedEvent event) {
        metrics.counter("notification.saved").increment();
        try {
            final Long subscriberCount = redis.convertAndSend(CHANNEL, event.userId() + ":" + event.notificationId());
            final String result = subscriberCount == null || subscriberCount == 0 ? "no_subscribers" : "success";
            metrics.counter("notification.redis.publish", "result", result).increment();
        } catch (final RuntimeException exception) {
            metrics.counter("notification.redis.publish", "result", "failure").increment();
            log.warn("[NOTIFICATION] 커밋된 알림의 Redis 발행에 실패했습니다. 재전송은 지원되지 않습니다.");
        }
    }
}
