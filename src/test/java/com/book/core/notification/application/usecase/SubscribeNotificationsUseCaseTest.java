package com.book.core.notification.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import com.book.core.notification.application.event.NotificationCreatedEvent;
import com.book.core.notification.application.event.NotificationStreamMessage;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class SubscribeNotificationsUseCaseTest {
    @Test
    void 같은_사용자의_연결마다_전달하고_닫힌_연결과_다른_사용자는_제외한다() {
        final var useCase = new SubscribeNotificationsUseCase();
        final List<NotificationStreamMessage> first = new ArrayList<>();
        final List<NotificationStreamMessage> second = new ArrayList<>();
        final List<NotificationStreamMessage> other = new ArrayList<>();
        final Runnable closeFirst = useCase.subscribe(1L, first::add);
        useCase.subscribe(1L, second::add);
        useCase.subscribe(2L, other::add);
        final var firstEvent = new NotificationCreatedEvent(1L, 10L);
        useCase.notifySubscribers(firstEvent);
        closeFirst.run();
        useCase.notifySubscribers(new NotificationCreatedEvent(1L, 11L));
        assertThat(first).containsExactly(NotificationStreamMessage.notification(10L));
        assertThat(second).containsExactly(NotificationStreamMessage.notification(10L), NotificationStreamMessage.notification(11L));
        assertThat(other).isEmpty();
    }

    @Test
    void 한_연결의_전달_오류가_다른_연결의_전달을_막지_않는다() {
        final var useCase = new SubscribeNotificationsUseCase();
        final List<NotificationStreamMessage> received = new ArrayList<>();
        useCase.subscribe(1L, event -> {
            throw new IllegalStateException("closed");
        });
        useCase.subscribe(1L, received::add);
        final var event = new NotificationCreatedEvent(1L, 10L);
        useCase.notifySubscribers(event);
        assertThat(received).containsExactly(NotificationStreamMessage.notification(10L));
    }

    @Test
    void 동기화_신호는_현재_인스턴스의_모든_연결에만_전달한다() {
        final var useCase = new SubscribeNotificationsUseCase();
        final List<NotificationStreamMessage> first = new ArrayList<>();
        final List<NotificationStreamMessage> other = new ArrayList<>();
        useCase.subscribe(1L, first::add);
        useCase.subscribe(2L, other::add);

        useCase.notifySyncRequired();

        assertThat(first).containsExactly(NotificationStreamMessage.syncRequired());
        assertThat(other).containsExactly(NotificationStreamMessage.syncRequired());
    }
}
