package com.book.core.notification.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.book.core.notification.application.event.NotificationStreamMessage;
import com.book.core.notification.application.usecase.SubscribeNotificationsUseCase;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@Tag("integration")
class NotificationRedisFanoutIntegrationTest {
    @Container
    static final GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7.4-alpine")).withExposedPorts(6379);

    @Test
    void 두_인스턴스는_같은_사용자의_로컬_SSE_연결에_알림을_전달한다() throws Exception {
        final var firstConnectionFactory = connectionFactory();
        final var secondConnectionFactory = connectionFactory();
        final var publisher = new StringRedisTemplate(firstConnectionFactory);
        publisher.afterPropertiesSet();
        final var firstSubscriptions = new SubscribeNotificationsUseCase();
        final var secondSubscriptions = new SubscribeNotificationsUseCase();
        final List<NotificationStreamMessage> firstMessages = new CopyOnWriteArrayList<>();
        final List<NotificationStreamMessage> secondMessages = new CopyOnWriteArrayList<>();
        final var firstSubscribed = new CountDownLatch(1);
        final var secondSubscribed = new CountDownLatch(1);
        final var firstReceived = new CountDownLatch(1);
        final var secondReceived = new CountDownLatch(1);
        firstSubscriptions.subscribe(42L, message -> {
            firstMessages.add(message);
            if (message.eventName().equals("notification")) {
                firstReceived.countDown();
            }
        });
        secondSubscriptions.subscribe(42L, message -> {
            secondMessages.add(message);
            if (message.eventName().equals("notification")) {
                secondReceived.countDown();
            }
        });
        final var firstSubscriber = new NotificationRedisSubscriber(firstSubscriptions, new SimpleMeterRegistry()) {
            @Override
            public void onChannelSubscribed(final byte[] channel, final long count) {
                super.onChannelSubscribed(channel, count);
                firstSubscribed.countDown();
            }
        };
        final var secondSubscriber = new NotificationRedisSubscriber(secondSubscriptions, new SimpleMeterRegistry()) {
            @Override
            public void onChannelSubscribed(final byte[] channel, final long count) {
                super.onChannelSubscribed(channel, count);
                secondSubscribed.countDown();
            }
        };
        final var firstListener = listener(firstConnectionFactory, firstSubscriber);
        final var secondListener = listener(secondConnectionFactory, secondSubscriber);
        try {
            firstListener.start();
            secondListener.start();
            assertThat(firstSubscribed.await(10, TimeUnit.SECONDS)).isTrue();
            assertThat(secondSubscribed.await(10, TimeUnit.SECONDS)).isTrue();

            publisher.convertAndSend(NotificationRedisPublisher.CHANNEL, "42:101");

            assertThat(firstReceived.await(10, TimeUnit.SECONDS)).isTrue();
            assertThat(secondReceived.await(10, TimeUnit.SECONDS)).isTrue();
            assertThat(firstMessages).contains(NotificationStreamMessage.notification(101L));
            assertThat(secondMessages).contains(NotificationStreamMessage.notification(101L));

            final var disconnected = redis.execInContainer("redis-cli", "CLIENT", "KILL", "TYPE", "PUBSUB");
            assertThat(disconnected.getExitCode()).isZero();
            assertThat(disconnected.getStdout().trim()).isEqualTo("2");
            final var syncRequired = NotificationStreamMessage.syncRequired();
            await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
                assertThat(firstMessages).containsExactly(syncRequired, NotificationStreamMessage.notification(101L), syncRequired);
                assertThat(secondMessages).containsExactly(syncRequired, NotificationStreamMessage.notification(101L), syncRequired);
            });

            publisher.convertAndSend(NotificationRedisPublisher.CHANNEL, "42:102");
            await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
                assertThat(firstMessages).contains(NotificationStreamMessage.notification(102L));
                assertThat(secondMessages).contains(NotificationStreamMessage.notification(102L));
            });
        } finally {
            firstListener.stop();
            firstListener.destroy();
            secondListener.stop();
            secondListener.destroy();
            firstConnectionFactory.destroy();
            secondConnectionFactory.destroy();
        }
    }

    private LettuceConnectionFactory connectionFactory() {
        final var factory = new LettuceConnectionFactory(redis.getHost(), redis.getMappedPort(6379));
        factory.afterPropertiesSet();
        factory.start();
        return factory;
    }

    private RedisMessageListenerContainer listener(
        final LettuceConnectionFactory connectionFactory,
        final NotificationRedisSubscriber subscriber
    ) throws Exception {
        final var container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(subscriber, new ChannelTopic(NotificationRedisPublisher.CHANNEL));
        container.afterPropertiesSet();
        return container;
    }
}
