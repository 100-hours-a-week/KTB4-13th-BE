package com.book.core.notification.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class NotificationHeartbeatSchedulerTest {
    @Test
    void heartbeat_한번이_실패해도_다음_주기는_계속_실행한다() throws Exception {
        final var scheduler = new NotificationHeartbeatScheduler();
        final var attempts = new AtomicInteger();
        final var recovered = new CountDownLatch(1);
        try {
            scheduler.start(() -> {
                if (attempts.getAndIncrement() == 0) {
                    throw new IllegalStateException("first heartbeat failed");
                }
                recovered.countDown();
            }, 10, TimeUnit.MILLISECONDS);
            assertThat(recovered.await(2, TimeUnit.SECONDS)).isTrue();
            assertThat(attempts.get()).isGreaterThanOrEqualTo(2);
        } finally {
            scheduler.close();
        }
    }
}
