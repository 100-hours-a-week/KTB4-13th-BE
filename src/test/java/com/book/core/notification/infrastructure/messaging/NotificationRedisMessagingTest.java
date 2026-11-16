package com.book.core.notification.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.book.core.notification.application.event.NotificationCreatedEvent;
import com.book.core.notification.application.event.NotificationStreamMessage;
import com.book.core.notification.application.usecase.SubscribeNotificationsUseCase;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.DefaultMessage;
import org.springframework.data.redis.core.StringRedisTemplate;

class NotificationRedisMessagingTest {
    @Test
    void 발행_실패는_전파되지_않고_실패_지표에_기록된다() {
        final var redis = mock(StringRedisTemplate.class);
        final var metrics = new SimpleMeterRegistry();
        doThrow(new IllegalStateException("unavailable")).when(redis).convertAndSend(anyString(), anyString());
        final var publisher = new NotificationRedisPublisher(redis, metrics);
        assertThatCode(() -> publisher.publish(new NotificationCreatedEvent(42L, 101L))).doesNotThrowAnyException();
        verify(redis).convertAndSend(NotificationRedisPublisher.CHANNEL, "42:101");
        assertThat(metrics.get("notification.saved").counter().count()).isEqualTo(1);
        assertThat(metrics.get("notification.redis.publish").tag("result", "failure").counter().count()).isEqualTo(1);
    }

    @Test
    void 수신한_식별자를_해당_사용자의_모든_구독에만_전달한다() {
        final var subscriptions = new SubscribeNotificationsUseCase();
        final var metrics = new SimpleMeterRegistry();
        final List<NotificationStreamMessage> first = new ArrayList<>();
        final List<NotificationStreamMessage> second = new ArrayList<>();
        final List<NotificationStreamMessage> other = new ArrayList<>();
        subscriptions.subscribe(42L, first::add);
        subscriptions.subscribe(42L, second::add);
        subscriptions.subscribe(43L, other::add);
        final var subscriber = new NotificationRedisSubscriber(subscriptions, metrics);
        subscriber.onMessage(new DefaultMessage(new byte[0], "42:101".getBytes(StandardCharsets.UTF_8)), null);
        subscriber.onMessage(new DefaultMessage(new byte[0], "invalid".getBytes(StandardCharsets.UTF_8)), null);
        assertThat(first).containsExactly(NotificationStreamMessage.notification(101L));
        assertThat(second).isEqualTo(first);
        assertThat(other).isEmpty();
        assertThat(metrics.get("notification.redis.received").tag("result", "invalid").counter().count()).isEqualTo(1);
    }

    @Test
    void Redis_구독_복구_시_연결된_클라이언트에_목록_동기화_신호를_보낸다() {
        final var subscriptions = new SubscribeNotificationsUseCase();
        final var metrics = new SimpleMeterRegistry();
        final List<NotificationStreamMessage> received = new ArrayList<>();
        subscriptions.subscribe(42L, received::add);
        final var subscriber = new NotificationRedisSubscriber(subscriptions, metrics);

        subscriber.onChannelSubscribed("book:notification:created".getBytes(StandardCharsets.UTF_8), 1);

        assertThat(received).containsExactly(NotificationStreamMessage.syncRequired());
        assertThat(metrics.get("notification.redis.subscribed").counter().count()).isEqualTo(1);
        assertThat(metrics.get("notification.redis.sync_required").counter().count()).isEqualTo(1);
    }
}
