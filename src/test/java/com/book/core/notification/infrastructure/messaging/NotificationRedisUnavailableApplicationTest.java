package com.book.core.notification.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.MeterRegistry;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {"notification.redis.enabled=true", "spring.data.redis.host=127.0.0.1", "spring.data.redis.port=1",
    "spring.data.redis.connect-timeout=100ms", "spring.data.redis.timeout=100ms"})
@ActiveProfiles("test")
@Tag("integration")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class NotificationRedisUnavailableApplicationTest {
    @Autowired
    ConfigurableApplicationContext context;
    @Autowired
    MeterRegistry metrics;

    @Test
    void Redis_연결이_불가능해도_애플리케이션은_시작하고_구독_실패를_기록한다() throws InterruptedException {
        final var failureCounter = metrics.counter("notification.redis.listener.start", "result", "failure");
        final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (failureCounter.count() == 0 && System.nanoTime() < deadline) {
            Thread.sleep(10);
        }

        assertThat(context.isActive()).isTrue();
        assertThat(failureCounter.count()).isEqualTo(1);
    }
}
