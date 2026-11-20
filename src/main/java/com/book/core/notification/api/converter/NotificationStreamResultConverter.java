package com.book.core.notification.api.converter;

import com.book.core.notification.api.NotificationStreamManager;
import com.book.core.notification.application.event.NotificationStreamMessage;
import java.util.function.Consumer;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
@RequiredArgsConstructor
public class NotificationStreamResultConverter {
    private final NotificationStreamManager streamManager;

    public ResponseEntity<SseEmitter> toResponse(final Function<Consumer<NotificationStreamMessage>, Runnable> subscribe) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).header("X-Accel-Buffering", "no")
            .contentType(MediaType.TEXT_EVENT_STREAM).body(streamManager.open(subscribe));
    }
}
