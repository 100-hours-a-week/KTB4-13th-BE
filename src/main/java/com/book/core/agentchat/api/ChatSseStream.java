package com.book.core.agentchat.api;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
final class ChatSseStream {
    private final SseEmitter emitter;
    private final BlockingQueue<Runnable> events = new ArrayBlockingQueue<>(8);
    private final AtomicBoolean closed = new AtomicBoolean();
    private boolean previewStopped;

    ChatSseStream(final SseEmitter emitter) {
        this.emitter = emitter;
        emitter.onCompletion(this::close);
        emitter.onTimeout(this::close);
        emitter.onError(failure -> close());
    }

    boolean isClosed() {
        return closed.get();
    }

    void delta(final String text) {
        if (isClosed() || previewStopped) {
            return;
        }
        // 미리보기가 밀리면 남은 조각을 생략하고 최종 이벤트 자리를 남깁니다.
        if (events.remainingCapacity() <= 1) {
            previewStopped = true;
            return;
        }
        events.offer(() -> send("delta", Map.of("text", text)));
    }

    void finish(final String name, final Object data) {
        if (!isClosed()) {
            events.offer(() -> {
                send(name, data);
                if (!isClosed()) {
                    emitter.complete();
                }
                close();
            });
        }
    }

    void write() {
        try {
            while (!isClosed()) {
                final Runnable event = events.take();
                if (!isClosed()) {
                    event.run();
                }
            }
        } catch (final InterruptedException failure) {
            Thread.currentThread().interrupt();
        } finally {
            close();
        }
    }

    void close() {
        if (closed.compareAndSet(false, true)) {
            events.clear();
            events.offer(() -> {
            });
        }
    }

    private void send(final String name, final Object data) {
        try {
            emitter.send(SseEmitter.event().name(name).data(data, MediaType.APPLICATION_JSON));
        } catch (final IOException | IllegalStateException failure) {
            close();
            log.debug("대화 스트림 전송을 중단합니다. errorType={}", failure.getClass().getSimpleName());
        }
    }
}
