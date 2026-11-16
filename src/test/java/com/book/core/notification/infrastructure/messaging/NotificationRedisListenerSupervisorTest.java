package com.book.core.notification.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

class NotificationRedisListenerSupervisorTest {
    @Test
    void Redis가_없어도_애플리케이션_시작을_막지_않고_리스너를_재시도할_수_있다() {
        final var listener = mock(RedisMessageListenerContainer.class);
        final var metrics = new SimpleMeterRegistry();
        final var supervisor = new NotificationRedisListenerSupervisor(listener, metrics);
        doThrow(new IllegalStateException("Redis unavailable")).when(listener).start();

        assertThatCode(supervisor::startListener).doesNotThrowAnyException();

        verify(listener).stop();
        assertThat(metrics.get("notification.redis.listener.start").tag("result", "failure").counter().count()).isEqualTo(1);
        supervisor.close();
    }
}
