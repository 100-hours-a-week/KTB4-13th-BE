package com.book.core.notification.infrastructure.messaging;

import com.book.core.notification.application.event.NotificationCreatedEvent;
import com.book.core.notification.application.usecase.SubscribeNotificationsUseCase;
import io.micrometer.core.instrument.MeterRegistry;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.SubscriptionListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
class NotificationRedisSubscriber implements MessageListener, SubscriptionListener {
    private final SubscribeNotificationsUseCase subscribers;
    private final MeterRegistry metrics;

    @Override
    public void onChannelSubscribed(final byte[] channel, final long count) {
        metrics.counter("notification.redis.subscribed").increment();
        metrics.counter("notification.redis.sync_required").increment();
        subscribers.notifySyncRequired();
    }

    @Override
    public void onMessage(final Message message, final byte[] pattern) {
        try {
            final String[] ids = new String(message.getBody(), StandardCharsets.UTF_8).split(":", -1);
            if (ids.length != 2) {
                throw new IllegalArgumentException("invalid message");
            }
            final long userId = Long.parseLong(ids[0]);
            final long notificationId = Long.parseLong(ids[1]);
            if (userId <= 0 || notificationId <= 0) {
                throw new IllegalArgumentException("invalid identifier");
            }
            metrics.counter("notification.redis.received", "result", "success").increment();
            subscribers.notifySubscribers(new NotificationCreatedEvent(userId, notificationId));
        } catch (final IllegalArgumentException exception) {
            metrics.counter("notification.redis.received", "result", "invalid").increment();
            log.warn("[NOTIFICATION] 형식이 올바르지 않은 Redis 이벤트를 무시했습니다.");
        }
    }
}
