package com.book.core.notification.application.usecase;

import com.book.core.notification.application.event.NotificationCreatedEvent;
import com.book.core.notification.application.event.NotificationStreamMessage;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import org.springframework.stereotype.Service;

/** 현재 인스턴스의 연결에만 커밋된 알림을 전달합니다. */
@Service
public class SubscribeNotificationsUseCase {
    private final ConcurrentHashMap<Long, Set<Consumer<NotificationStreamMessage>>> subscribers = new ConcurrentHashMap<>();

    public Runnable subscribe(final Long userId, final Consumer<NotificationStreamMessage> subscriber) {
        subscribers.compute(userId, (final Long key, final Set<Consumer<NotificationStreamMessage>> existing) -> {
            final Set<Consumer<NotificationStreamMessage>> listeners;
            if (existing == null) {
                listeners = ConcurrentHashMap.newKeySet();
            } else {
                listeners = existing;
            }
            listeners.add(subscriber);
            return listeners;
        });
        return () -> removeSubscriber(userId, subscriber);
    }

    public void notifySubscribers(final NotificationCreatedEvent event) {
        final Set<Consumer<NotificationStreamMessage>> listeners = subscribers.get(event.userId());
        notifySubscribers(event.userId(), listeners, NotificationStreamMessage.notification(event.notificationId()));
    }

    public void notifySyncRequired() {
        for (final Map.Entry<Long, Set<Consumer<NotificationStreamMessage>>> entry : subscribers.entrySet()) {
            notifySubscribers(entry.getKey(), entry.getValue(), NotificationStreamMessage.syncRequired());
        }
    }

    private void notifySubscribers(
        final Long userId,
        final Set<Consumer<NotificationStreamMessage>> listeners,
        final NotificationStreamMessage message
    ) {
        if (listeners == null) {
            return;
        }
        for (final Consumer<NotificationStreamMessage> listener : listeners) {
            try {
                listener.accept(message);
            } catch (final RuntimeException exception) {
                removeSubscriber(userId, listener);
            }
        }
    }

    private void removeSubscriber(final Long userId, final Consumer<NotificationStreamMessage> subscriber) {
        subscribers.computeIfPresent(userId, (final Long key, final Set<Consumer<NotificationStreamMessage>> listeners) -> {
            listeners.remove(subscriber);
            if (listeners.isEmpty()) {
                return null;
            }
            return listeners;
        });
    }
}
