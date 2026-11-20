package com.book.core.notification.api;

import com.book.core.notification.application.event.NotificationStreamMessage;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** 이 인스턴스의 연결만 관리하며 끊어진 동안의 이벤트는 재전송하지 않습니다. */
@Component
@Slf4j
public class NotificationStreamManager {
    private static final long TIMEOUT_MILLIS = 180_000L;
    private final Map<UUID, Stream> streams = new ConcurrentHashMap<>();
    private final NotificationHeartbeatScheduler heartbeatScheduler;
    private final MeterRegistry metrics;

    NotificationStreamManager(final MeterRegistry meterRegistry, final NotificationHeartbeatScheduler heartbeatScheduler) {
        this.heartbeatScheduler = heartbeatScheduler;
        this.metrics = meterRegistry;
        Gauge.builder("notification.sse.connections", streams, Map::size).description("Active notification SSE connections")
            .register(meterRegistry);
    }

    @PostConstruct
    void startHeartbeat() {
        heartbeatScheduler.start(this::heartbeat);
    }

    public SseEmitter open(final Function<Consumer<NotificationStreamMessage>, Runnable> subscribe) {
        final UUID id = UUID.randomUUID();
        final Stream stream = new Stream(id);
        streams.put(id, stream);
        metrics.counter("notification.sse.opened").increment();
        stream.emitter.onCompletion(stream::close);
        stream.emitter.onTimeout(() -> {
            stream.close();
            stream.emitter.complete();
        });
        stream.emitter.onError((final Throwable exception) -> stream.close());
        try {
            stream.attach(subscribe.apply(stream::send));
            stream.emitter.send(SseEmitter.event().name("connected").data("connected"));
            metrics.counter("notification.sse.connected").increment();
        } catch (final IOException exception) {
            stream.fail(exception);
        } catch (final RuntimeException exception) {
            stream.fail(exception);
        }
        return stream.emitter;
    }

    void heartbeat() {
        for (final Stream stream : streams.values()) {
            try {
                stream.heartbeat();
            } catch (final RuntimeException exception) {
                log.warn("SSE heartbeat 연결 정리에 실패했습니다.", exception);
            }
        }
    }

    private final class Stream {
        private final UUID id;
        private final SseEmitter emitter = new SseEmitter(TIMEOUT_MILLIS);
        private final AtomicBoolean closed = new AtomicBoolean();
        private final AtomicReference<Runnable> unsubscribe = new AtomicReference<>();

        private Stream(final UUID id) {
            this.id = id;
        }

        private void attach(final Runnable action) {
            unsubscribe.set(action);
            if (closed.get()) {
                detach();
            }
        }

        private void close() {
            if (closed.compareAndSet(false, true)) {
                streams.remove(id, this);
                metrics.counter("notification.sse.closed").increment();
                detach();
            }
        }

        private void detach() {
            final Runnable action = unsubscribe.getAndSet(null);
            if (action != null) {
                try {
                    action.run();
                } catch (final RuntimeException exception) {
                    metrics.counter("notification.sse.cleanup", "result", "failure").increment();
                    log.warn("SSE 연결 구독을 정리하지 못했습니다.");
                }
            }
        }

        private void send(final NotificationStreamMessage message) {
            try {
                metrics.counter("notification.sse.delivery", "result", "attempt").increment();
                final var event = SseEmitter.event().name(message.eventName());
                if (message.notificationId() == null) {
                    event.data("required");
                } else {
                    event.data(Map.of("notificationId", message.notificationId()), MediaType.APPLICATION_JSON);
                }
                emitter.send(event);
                metrics.counter("notification.sse.delivery", "result", "sent").increment();
                if (message.notificationId() == null) {
                    metrics.counter("notification.sse.sync_required").increment();
                }
            } catch (final IOException exception) {
                metrics.counter("notification.sse.delivery", "result", "failure").increment();
                fail(exception);
            } catch (final RuntimeException exception) {
                metrics.counter("notification.sse.delivery", "result", "failure").increment();
                fail(exception);
            }
        }

        private void heartbeat() {
            try {
                emitter.send(SseEmitter.event().comment("heartbeat"));
                metrics.counter("notification.sse.heartbeat", "result", "sent").increment();
            } catch (final IOException exception) {
                metrics.counter("notification.sse.heartbeat", "result", "failure").increment();
                close();
            } catch (final RuntimeException exception) {
                metrics.counter("notification.sse.heartbeat", "result", "failure").increment();
                fail(exception);
            }
        }

        private void fail(final Throwable exception) {
            close();
            try {
                emitter.completeWithError(exception);
            } catch (final RuntimeException completionFailure) {
                log.debug("이미 종료된 SSE 연결의 오류 완료에 실패했습니다.", completionFailure);
            }
        }
    }
}
