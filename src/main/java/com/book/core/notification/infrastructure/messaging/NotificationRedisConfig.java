package com.book.core.notification.infrastructure.messaging;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import lombok.extern.slf4j.Slf4j;

@Configuration
@ConditionalOnProperty(name = "notification.redis.enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
class NotificationRedisConfig {
    @Bean
    RedisMessageListenerContainer notificationRedisListener(
        final RedisConnectionFactory connectionFactory,
        final NotificationRedisSubscriber subscriber,
        final MeterRegistry metrics
    ) {
        final var container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setAutoStartup(false);
        Gauge.builder("notification.redis.listening", container, NotificationRedisConfig::listening).register(metrics);
        container.setErrorHandler(error -> {
            metrics.counter("notification.redis.listener.errors").increment();
            log.warn("알림 Redis 구독에서 오류가 발생했습니다. 연결 복구를 확인합니다.");
        });
        container.addMessageListener(subscriber, new ChannelTopic(NotificationRedisPublisher.CHANNEL));
        return container;
    }

    private static int listening(final RedisMessageListenerContainer container) {
        if (container.isListening()) {
            return 1;
        }
        return 0;
    }
}
