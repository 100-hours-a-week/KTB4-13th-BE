package com.book.core.notification.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class NotificationStreamManagerTest {
    @Test
    void timeout은_연결을_완료하고_로컬_구독을_정리한다() {
        final var scheduler = new NotificationHeartbeatScheduler();
        final var registry = new SimpleMeterRegistry();
        final var unsubscribed = new AtomicInteger();
        try (final var emitters = mockConstruction(SseEmitter.class)) {
            final var manager = new NotificationStreamManager(registry, scheduler);
            manager.open(subscriber -> unsubscribed::incrementAndGet);
            final var emitter = emitters.constructed().getFirst();
            final var timeoutHandler = ArgumentCaptor.forClass(Runnable.class);
            verify(emitter).onTimeout(timeoutHandler.capture());

            timeoutHandler.getValue().run();

            assertThat(registry.get("notification.sse.connections").gauge().value()).isZero();
            assertThat(unsubscribed.get()).isEqualTo(1);
            verify(emitter).complete();
        } finally {
            scheduler.close();
        }
    }

    @Test
    void 연결이_끊긴_경우_정리하고_다른_연결의_heartbeat는_계속된다() throws Exception {
        final var scheduler = new NotificationHeartbeatScheduler();
        final var registry = new SimpleMeterRegistry();
        final var unsubscribed = new AtomicInteger();
        try (final var emitters = mockConstruction(SseEmitter.class)) {
            final var manager = new NotificationStreamManager(registry, scheduler);
            manager.open(subscriber -> unsubscribed::incrementAndGet);
            manager.open(subscriber -> () -> {
            });
            final var broken = emitters.constructed().get(0);
            final var healthy = emitters.constructed().get(1);
            doThrow(new IOException("broken pipe")).when(broken).send(any(SseEmitter.SseEventBuilder.class));

            assertThatCode(manager::heartbeat).doesNotThrowAnyException();
            assertThat(unsubscribed.get()).isEqualTo(1);
            assertThat(registry.get("notification.sse.connections").gauge().value()).isEqualTo(1);
            verify(broken, never()).completeWithError(any());
            verify(healthy, times(2)).send(any(SseEmitter.SseEventBuilder.class));
            manager.heartbeat();
            verify(healthy, times(3)).send(any(SseEmitter.SseEventBuilder.class));
        } finally {
            scheduler.close();
        }
    }

    @Test
    void 연결_정리에서_예외가_발생해도_다른_연결의_heartbeat는_계속된다() throws Exception {
        final var scheduler = new NotificationHeartbeatScheduler();
        final var registry = new SimpleMeterRegistry();
        try (final var emitters = mockConstruction(SseEmitter.class)) {
            final var manager = new NotificationStreamManager(registry, scheduler);
            manager.open(subscriber -> () -> {
                throw new IllegalStateException("cleanup failed");
            });
            manager.open(subscriber -> () -> {
            });
            final var broken = emitters.constructed().get(0);
            final var healthy = emitters.constructed().get(1);
            doThrow(new IOException("broken pipe")).when(broken).send(any(SseEmitter.SseEventBuilder.class));

            assertThatCode(manager::heartbeat).doesNotThrowAnyException();
            assertThat(registry.get("notification.sse.connections").gauge().value()).isEqualTo(1);
            verify(healthy, times(2)).send(any(SseEmitter.SseEventBuilder.class));
        } finally {
            scheduler.close();
        }
    }

    @Test
    void 오류_완료가_다시_실패해도_heartbeat가_중단되지_않는다() throws Exception {
        final var scheduler = new NotificationHeartbeatScheduler();
        final var registry = new SimpleMeterRegistry();
        try (final var emitters = mockConstruction(SseEmitter.class)) {
            final var manager = new NotificationStreamManager(registry, scheduler);
            manager.open(subscriber -> () -> {
            });
            manager.open(subscriber -> () -> {
            });
            final var broken = emitters.constructed().get(0);
            final var healthy = emitters.constructed().get(1);
            doThrow(new IllegalStateException("send failed")).when(broken).send(any(SseEmitter.SseEventBuilder.class));
            doThrow(new IllegalStateException("completion failed")).when(broken).completeWithError(any());

            assertThatCode(manager::heartbeat).doesNotThrowAnyException();
            assertThat(registry.get("notification.sse.connections").gauge().value()).isEqualTo(1);
            verify(healthy, times(2)).send(any(SseEmitter.SseEventBuilder.class));
        } finally {
            scheduler.close();
        }
    }
}
